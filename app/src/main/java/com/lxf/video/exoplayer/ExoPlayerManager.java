package com.lxf.video.exoplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.TextureView;

import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
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
public class ExoPlayerManager  implements ExoPlayer.EventListener, SimpleExoPlayer.VideoListener{
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
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private SurfaceTexture mSurfaceTexture;                            //保留SurfaceTexture
    private ExoPlayerManager() {
        mMediaHandlerThread = new HandlerThread(TAG);
        mMediaHandlerThread.start();
        mMediaHandler = new MediaHandler(mMediaHandlerThread.getLooper());
        mainHandler = new Handler(Looper.getMainLooper());
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
            mSimpleExoPlayer.release();
            mSurfaceTexture = null;
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
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(exoPlayerPrepareData.getContext(), trackSelector, loadControl);
        //把player通过surfaceTextureListener联系在一起
        mSimpleExoPlayer.setVideoTextureView(mTextureView);
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
                    clearExoPlayer();
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

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        //播放完成
        if(playbackState == ExoPlayer.STATE_ENDED) {
        } else if (playbackState == ExoPlayer.STATE_READY) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    ExoPlayerLayoutManager.getInstance().getCurrentJcvd().setUIState(ExoPlayerLayout.UI_VIDEO_PLAYING);
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
    public void onPlayerError(ExoPlaybackException error) {
        ExoPlayerLayoutManager.getInstance().getCurrentJcvd().setUIState(ExoPlayerLayout.UI_VIDEO_STATE_INIT);
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
}
