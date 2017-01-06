package com.lxf.video.exoplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.lxf.video.VideoApplication;


/**
 * 单例管理SimpleExoPlayer
 * User: luoxf
 * Date: 2016-12-27
 * Time: 10:14
 */
public class ExoPlayerManager  implements ExoPlayer.EventListener, SimpleExoPlayer.VideoListener, DefaultBandwidthMeter.EventListener{
    private String TAG = ExoPlayerManager.class.getSimpleName();
    private static ExoPlayerManager mInstance;
    private final static int MSG_PREPARE = 1;                          //准备MEDIA
    private final static int MSG_COMPELETE = 2;                        //完成MEDIA
    private HandlerThread mMediaHandlerThread;                         //处理视频线程
    private MediaHandler mMediaHandler;                                //视频线程消息处理
    private Handler mainHandler;                                       //通知主线程处理UI
    private SimpleExoPlayer mSimpleExoPlayer;
    private TextureView mTextureView;                                  //播放的layout 做统一管理，播放前需要remove掉旧的
    private String mUrl;
    private DefaultBandwidthMeter BANDWIDTH_METER;
    private SurfaceTexture mSurfaceTexture;                            //保存SurfaceTexture;
    private TrackSelection.Factory mVideoTrackSelectionFactory;
    private DefaultTrackSelector mDefaultTrackSelector;
    private Format mVideoSelectFormat;                                  //当前播放视频的画质
    private ExoPlayerManager() {
        mMediaHandlerThread = new HandlerThread(TAG);
        mMediaHandlerThread.start();
        mMediaHandler = new MediaHandler(mMediaHandlerThread.getLooper());
        mainHandler = new Handler(Looper.getMainLooper());
        BANDWIDTH_METER = new DefaultBandwidthMeter(mainHandler, this);
        mVideoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        mDefaultTrackSelector = new DefaultTrackSelector(mVideoTrackSelectionFactory);
    }

    /**
     * 单例获取ExoPlayerManager
     * @return ExoPlayerManager
     */
    public static ExoPlayerManager getInstance() {
        if (mInstance == null) {
            synchronized (ExoPlayerManager.class) {
                if (mInstance == null) {
                    mInstance = new ExoPlayerManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 准备播放视频,发送准备操作
     * @param context Context
     * @param url 视频地址
     */
    public void preparePlayer(Context context, String url) {
        if(null == context) {
            return;
        }
        if(TextUtils.isEmpty(url)) {
            return;
        }
        ExoPlayerPrepareData exoPlayerPrepareData = new ExoPlayerPrepareData(context, url);
        Message msg = new Message();
        msg.what = MSG_PREPARE;
        msg.obj = exoPlayerPrepareData;
        mMediaHandler.sendMessage(msg);
    }

    /**
     * 释放ExoPlayer，发送释放操作
     */
    public void releasePlayer() {
        Message msg = new Message();
        msg.what = MSG_COMPELETE;
        mMediaHandler.sendMessage(msg);
    }

    /**
     * 真正的释放视频操作
     */
    private void clearExoPlayer() {
        if(null != mSimpleExoPlayer) {
            mSimpleExoPlayer.clearVideoSurface();
            mSimpleExoPlayer.release();
            mSimpleExoPlayer = null;
            mUrl = null;
        }
    }

    /**
     * 真正的准备视频操作
     */
    private void prepareExoPlayer(ExoPlayerPrepareData exoPlayerPrepareData) {
        if(null == exoPlayerPrepareData) {
            return;
        }
        if(null == mSurfaceTexture) {
            return;
        }

        LoadControl loadControl = new DefaultLoadControl();
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(exoPlayerPrepareData.getContext(), mDefaultTrackSelector, loadControl);
        //把player通过surfaceTextureListener联系在一起
        mSimpleExoPlayer.setVideoSurface(new Surface(mSurfaceTexture));
        HttpProxyCacheServer proxy = VideoApplication.getProxy(VideoApplication.getContext());
        //缓存控制
        String proxyUrl = proxy.getProxyUrl(exoPlayerPrepareData.getUrl());
        MediaSource videoSource = buildMediaSource(exoPlayerPrepareData.getContext(), Uri.parse(proxyUrl));
        mSimpleExoPlayer.setPlayWhenReady(true);
        mSimpleExoPlayer.setVideoListener(this);
        mSimpleExoPlayer.addListener(this);
        mSimpleExoPlayer.prepare(videoSource);
    }

    /**
     * 给不同的URL设置不同的MediaSource
     * @param context Context
     * @param uri 视频地址
     * @return MediaSource
     */
    private MediaSource buildMediaSource(Context context, Uri uri) {
        int type = getUrlType(uri.toString());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, new DefaultDataSourceFactory(context, null,
                        new DefaultHttpDataSourceFactory(TAG, null)),
                        new DefaultSsChunkSource.Factory(new DefaultDataSourceFactory(context, BANDWIDTH_METER,
                                new DefaultHttpDataSourceFactory(TAG, BANDWIDTH_METER))), mMediaHandler, null);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, new DefaultDataSourceFactory(context, null,
                        new DefaultHttpDataSourceFactory(TAG, null)),
                        new DefaultDashChunkSource.Factory(new DefaultDataSourceFactory(context, BANDWIDTH_METER,
                                new DefaultHttpDataSourceFactory(TAG, BANDWIDTH_METER))), mMediaHandler, null);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, new DefaultDataSourceFactory(context, BANDWIDTH_METER,
                        new DefaultHttpDataSourceFactory(TAG, BANDWIDTH_METER)), mMediaHandler, null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, new DefaultDataSourceFactory(context, BANDWIDTH_METER,
                        new DefaultHttpDataSourceFactory(TAG, BANDWIDTH_METER)), new DefaultExtractorsFactory(),
                        mMediaHandler, null);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    /**
     * 获取视频地址的类型
     * @param url
     * @return
     */
    @C.ContentType
    private int getUrlType(String url) {
        if (url == null) {
            return C.TYPE_OTHER;
        } else if (url.contains(".mpd")) {
            return C.TYPE_DASH;
        } else if (url.contains(".ism") || url.contains(".isml")) {
            return C.TYPE_SS;
        } else if (url.contains(".m3u8")) {
            return C.TYPE_HLS;
        } else {
            return C.TYPE_OTHER;
        }
    }

    /**
     * {@link com.google.android.exoplayer2.upstream.BandwidthMeter.EventListener}
     * @param elapsedMs 传输时间
     * @param bytes 传输的带宽
     * @param bitrate 平均宽
     */
    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        Toast.makeText(VideoApplication.getContext(), "平均比特率 ：" + bitrate, Toast.LENGTH_SHORT).show();
    }

    class MediaHandler extends Handler {
        public MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //释放ExoPLayer
                case MSG_COMPELETE:
                    clearExoPlayer();
                    break;
                case MSG_PREPARE:
                    prepareExoPlayer((ExoPlayerPrepareData) msg.obj);
                    break;
            }
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                ExoPlayerLayoutManager.getInstance().getCurrentJcvd().updateProgress();
            }
        });
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = mDefaultTrackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            boolean isSupport = true;
            if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                    == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                Log.e("onTracksChanged", "设备不支持视频播放");
                isSupport = false;
            }
            if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                    == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                Log.e("onTracksChanged", "设备不支持音频播放");
                isSupport = false;
            }
            if(!isSupport) {
                return;
            }
        }
        for(int i = 0; i < trackGroups.length; i++) {
            //取出当前播放的视频画质
           if(null != mSimpleExoPlayer && mSimpleExoPlayer.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
               TrackGroup trackGroup = trackGroups.get(i);
               if(trackGroup.length != 1) {
                   return;
               }
               for(int j = 0; j < trackGroup.length; j++) {
                   mVideoSelectFormat = trackGroup.getFormat(j);
                   Log.e("onTracksChanged", mVideoSelectFormat.width + "*" + mVideoSelectFormat.height);
                   mainHandler.post(new Runnable() {
                       @Override
                       public void run() {
                           ExoPlayerLayoutManager.getInstance().getCurrentJcvd().setUIState(ExoPlayerLayout.UI_VIDEO_IMAGE_QUALITY);
                       }
                   });
               }
           }
        }

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        //播放完成
        if( playbackState == ExoPlayer.STATE_ENDED) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    ExoPlayerLayout exoPlayerLayout = ExoPlayerLayoutManager.getInstance().getCurrentJcvd();
                    if(null != exoPlayerLayout && null != mSimpleExoPlayer && 0 != mSimpleExoPlayer.getCurrentPosition()) {
                        exoPlayerLayout.setUIState(ExoPlayerLayout.UI_VIDEO_END);
                    }
                }
            });
        } else if (playbackState == ExoPlayer.STATE_READY) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    ExoPlayerLayout exoPlayerLayout = ExoPlayerLayoutManager.getInstance().getCurrentJcvd();
                    if(null != exoPlayerLayout) {
                        exoPlayerLayout.setUIState(ExoPlayerLayout.UI_VIDEO_PLAYING);
                    }
                }
            });
            //可以播放
        } else if(playbackState == ExoPlayer.STATE_BUFFERING) {
            //缓冲完成
        } else if(playbackState == ExoPlayer.STATE_IDLE) {
            //播放出错：找不到资源播放
        }
    }

    @Override
    public void onPlayerError(final ExoPlaybackException error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VideoApplication.getContext(), error.getCause().toString(), Toast.LENGTH_SHORT).show();
                Log.e("onPlayerError", error.getCause().toString());
                ExoPlayerLayout exoPlayerLayout = ExoPlayerLayoutManager.getInstance().getCurrentJcvd();
                if(null != exoPlayerLayout) {
                    exoPlayerLayout.clickToStart();
                }
            }
        });
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

    }

    @Override
    public void onRenderedFirstFrame() {

    }

    /**
     * Exoplayer播放准备的数据，有需要时可以修改
     */
    private class ExoPlayerPrepareData {
        private Context context;
        private String url;

        public ExoPlayerPrepareData(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        public Context getContext() {
            return context;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public SimpleExoPlayer getSimpleExoPlayer() {
        return mSimpleExoPlayer;
    }

    public TextureView getTextureView() {
        return mTextureView;
    }

    public void setTextureView(TextureView textureView) {
        this.mTextureView = textureView;
        if(null != mTextureView) {
            this.mTextureView.setSurfaceTextureListener(new  TextureView.SurfaceTextureListener() {

                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    //在列表里的，或者是第一次播放，先清除掉旧的SurfaceTexture，再添加到ExoPlayer
                    if (mSurfaceTexture == null || ExoPlayerLayoutManager.getInstance().getFirstFloor().getFullScreenExoPlayerLayout() == null) {
                        if(null != mSimpleExoPlayer) {
                            mSimpleExoPlayer.clearVideoSurface();
                            mSimpleExoPlayer.release();
                        }
                        if(null != mSurfaceTexture) {
                            mSurfaceTexture.release();
                            mSurfaceTexture = null;
                        }
                        mSurfaceTexture = surface;
                        preparePlayer(mTextureView.getContext(), getUrl());
                    } else {
                        //用于全屏和恢复竖屏
                        mTextureView.setSurfaceTexture(mSurfaceTexture);
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    /**
     * 设置播放器状态
     */
    public void setExoPlayWhenRead(boolean playeWhenRead) {
        if(null != mSimpleExoPlayer) {
            mSimpleExoPlayer.setPlayWhenReady(playeWhenRead);
        }
    }

    @Nullable
    public Format getVideoSelectFormat() {
        return mVideoSelectFormat;
    }
}
