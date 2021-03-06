package com.lxf.video.exoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.lxf.video.R;
import com.lxf.video.VideoApplication;


import java.lang.reflect.Constructor;
import java.util.Formatter;
import java.util.Locale;

/**
 * 播放视频的layout
 * User: luoxf
 * Date: 2016-12-27
 * Time: 21:48
 */
public class ExoPlayerLayout extends FrameLayout implements View.OnClickListener{
    public final static int UI_VIDEO_STATE_INIT = 0;        //视频状态：初始状态
    public final static int UI_VIDEO_STATE_BUFFERING = 1;   //视频状态：点击开始，缓冲状态
    public final static int UI_VIDEO_PLAYING = 2;           //视频状态：播放状态
    public final static int UI_VIDEO_PAUSING = 3;           //视频状态：暂停状态， 暂停中，点击又开始播放
    public final static int UI_VIDEO_FINISH = 4;            //视频状态：播放完成状态
    public final static int UI_VIDEO_TOUCHED = 5;           //视频状态：播放状态，timeline等，属于触模后的一种动态
    public final static int UI_VIDEO_END = 6;               //播放结束，点击再度播放，则从头开始播放
    public final static int UI_VIDEO_IMAGE_QUALITY = 7;     //视频播放画质，更新tvVideoTrack的文字,有两个地方调用，
                                                            // 一个是ExoPlayer 的onTracksChanged，另一个是选择画质待开发中
    private static final int PROGRESS_BAR_MAX = 100;        //progress 最大值
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5000; //Touched状态多长时间消失

    private Context mContext;
    private ViewGroup surfaceContainer;                     //视频加载块
    private ProgressBar progressBarLoading;                 //缓冲播放前加载
    private ImageView  ivStart;                             //播放按钮
    private ViewGroup exoLayoutBottom;                      //播放进度时间区域
    private TextView exoPosition;                           //当前播放位置
    private TextView exoDuration;                           //视频播放时长
    private SeekBar exoProgress;                            //播放进度
    private ProgressBar exoBottomProgressbar;               //底部的progressbar;
    private ImageView ivPause;                              //暂停按钮
    private ImageView ivToPause;                            //点击暂停按钮
    private ImageView ivFullscreen;                         //全屏按钮
    private ImageView ivVideoBg;                            //视频背景
    private TextView tvVideoTrack;                          //视频画质选择

    private boolean mDragging;                              //用来表示SeekBar是否在拖动中。
    private boolean mTouched;                               //用来表示控制状态
    private boolean mPausing;                               //是否停止播放
    private StringBuilder formatBuilder = new StringBuilder();
    private Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());

    private ExoPlayerLayout mFullScreenExoPlayerLayout;     //用于保存全屏的ExoPlayerLayout
    private String mUrl;                                    //保存要播放的URL;
    private String mImageUrl;                               //视频背景URL;

    public ExoPlayerLayout(Context context) {
        super(context);
        init(context);
    }

    public ExoPlayerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ExoPlayerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        View.inflate(context, getLayoutId(), this);
        surfaceContainer = (ViewGroup) findViewById(R.id.surface_container);
        progressBarLoading = (ProgressBar) findViewById(R.id.loading);
        ivStart = (ImageView) findViewById(R.id.start);
        ivStart.setOnClickListener(this);
        exoLayoutBottom = (ViewGroup) findViewById(R.id.exo_layout_bottom);
        exoPosition = (TextView) findViewById(R.id.exo_position);
        exoDuration = (TextView) findViewById(R.id.exo_duration);
        exoProgress = (SeekBar) findViewById(R.id.exo_progress);
        exoProgress.setOnSeekBarChangeListener(onSeekBarChangeListener);
        exoBottomProgressbar = (ProgressBar) findViewById(R.id.exo_bottom_progressbar);
        ivPause = (ImageView) findViewById(R.id.iv_pause);
        ivPause.setOnClickListener(this);
        ivToPause = (ImageView) findViewById(R.id.iv_to_pause);
        ivToPause.setOnClickListener(this);
        ivFullscreen = (ImageView) findViewById(R.id.fullscreen);
        ivFullscreen.setOnClickListener(this);
        ivVideoBg = (ImageView) findViewById(R.id.iv_video_bg);
        tvVideoTrack = (TextView) findViewById(R.id.tv_video_track);
        tvVideoTrack.setOnClickListener(this);
        setUIState(UI_VIDEO_STATE_INIT);
    }

    /**
     * 返回一个layout
     * @return layout
     */
    protected int getLayoutId() {
        return R.layout.exo_player_base;
    }

    /**
     * 完成播放,清除ExoPlayerLayout有关的播放设置
     */
    public void eventCompletePlay() {
        setUIState(ExoPlayerLayout.UI_VIDEO_PAUSING);
        removeTextureView();
        //释放掉SurfaceTexture
        ExoPlayerManager.getInstance().setTextureView(null);
        removeCallbacks(updateProgressRunnable);
        removeCallbacks(touchRunnable);
        ExoPlayerAudioSupport.getInstance(mContext.getApplicationContext()).removeSupportAudio();
    }

    /**
     * 准备播放,先移除掉上一个TextureView，添加TextureView,添加音频保存这个ExoPlayerLayout
     */
    public void eventPreparePlay() {
        releaseAllVideos();
        //首先清除其它播放，让它恢复播放前的view
        initTextureView();
        addTextureView();
        ExoPlayerAudioSupport.getInstance(mContext.getApplicationContext()).addSupportAudio();
        ExoPlayerLayoutManager.getInstance().setFirstFloor(this);
    }

    /**
     * 初始化播放view
     */
    private void initTextureView() {
        ExoPlayerManager.getInstance().setTextureView(new TextureView(mContext));
    }

    /**
     * 把TextureView加入到FrameLayout surface_container
     */
    private void addTextureView() {
        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        surfaceContainer.addView(ExoPlayerManager.getInstance().getTextureView(), layoutParams);
    }

    /**
     * 移除掉上一个TextureView
     */
    private void removeTextureView() {
        ExoPlayerManager exoPlayerManager = ExoPlayerManager.getInstance();
        TextureView textureView = exoPlayerManager.getTextureView();
        if(null != textureView) {
            surfaceContainer.removeView(textureView);
        }
    }

    /**
     * 释放exoplayer和ExoplayerLayout的相关播放设置 eventCompletPlay();
     */
    public void releaseAllVideos() {
        ExoPlayerLayoutManager.getInstance().completeAll();
        ExoPlayerManager.getInstance().releasePlayer();
    }

    /**
     *
     * @param newConfig
     */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 更新UI
     * @param uiState UI状态
     */
    public void setUIState(int uiState) {
        switch (uiState) {
            case UI_VIDEO_STATE_INIT:
                uiStateInit();
                break;
            case UI_VIDEO_STATE_BUFFERING:
                uiStateBuffering();
                break;
            case UI_VIDEO_PLAYING:
                uiStateReady();
                break;
            case UI_VIDEO_END:
            case UI_VIDEO_PAUSING:
                uiStatePause();
                break;
            case UI_VIDEO_FINISH:
                uiStateFinish();
                break;
            case UI_VIDEO_TOUCHED:
                uiStateTouched();
                break;
            case UI_VIDEO_IMAGE_QUALITY:
                uiStateImageQuality();
                break;
            default:
                break;
        }
    }

    /**
     * 视频播放初始状态
     */
    private void uiStateInit() {
        ivStart.setVisibility(View.VISIBLE);
        progressBarLoading.setVisibility(View.INVISIBLE);
        exoLayoutBottom.setVisibility(View.INVISIBLE);
        exoBottomProgressbar.setVisibility(View.INVISIBLE);
        ivPause.setVisibility(View.INVISIBLE);
        ivToPause.setVisibility(View.INVISIBLE);
        if(null == ExoPlayerLayoutManager.getInstance().getFirstFloor()) {
            ivVideoBg.setVisibility(View.VISIBLE);
        }
        Glide.with(VideoApplication.getContext()).load(mImageUrl).into(ivVideoBg);
    }

    /**
     * 视频播放缓冲状态
     */
    private void uiStateBuffering() {
        ExoPlayerManager.getInstance().setExoPlayWhenRead(true);
        ivStart.setVisibility(View.INVISIBLE);
        progressBarLoading.setVisibility(View.VISIBLE);
        exoLayoutBottom.setVisibility(View.INVISIBLE);
        exoBottomProgressbar.setVisibility(View.INVISIBLE);
        ivPause.setVisibility(View.INVISIBLE);
        ivToPause.setVisibility(View.INVISIBLE);
        ivVideoBg.setVisibility(View.VISIBLE);
    }

    /**
     * 视频播放可以播放状态
     */
    private void uiStateReady() {
       if(!mTouched && !mPausing) {
           ivVideoBg.setVisibility(View.INVISIBLE);
           ivStart.setVisibility(View.INVISIBLE);
           progressBarLoading.setVisibility(View.INVISIBLE);
           exoLayoutBottom.setVisibility(View.INVISIBLE);
           exoBottomProgressbar.setVisibility(View.VISIBLE);
           ivPause.setVisibility(View.INVISIBLE);
           ivToPause.setVisibility(View.INVISIBLE);
           //全屏状态下的缩小按钮
           if(this == ExoPlayerLayoutManager.getInstance().getSecondFloor()) {
               ivFullscreen.setBackgroundResource(R.mipmap.exo_layout_not_full_screen);
           }
       }
    }

    /**
     * 视频播放状态：触摸状态
     */
    private void uiStateTouched() {
        ivVideoBg.setVisibility(View.INVISIBLE);
        ivStart.setVisibility(View.INVISIBLE);
        progressBarLoading.setVisibility(View.INVISIBLE);
        exoLayoutBottom.setVisibility(View.VISIBLE);
        exoBottomProgressbar.setVisibility(View.INVISIBLE);
        ivPause.setVisibility(View.INVISIBLE);
        ivToPause.setVisibility(View.VISIBLE);
    }

    /**
     * 视频播放状态：完成状态
     */
    private void uiStateFinish() {
        ExoPlayerManager.getInstance().setExoPlayWhenRead(false);
        ivStart.setVisibility(View.INVISIBLE);
        progressBarLoading.setVisibility(View.INVISIBLE);
        exoLayoutBottom.setVisibility(View.VISIBLE);
        exoBottomProgressbar.setVisibility(View.INVISIBLE);
        ivPause.setVisibility(View.INVISIBLE);
        ivToPause.setVisibility(View.INVISIBLE);
        ivVideoBg.setVisibility(View.VISIBLE);
    }

    /**
     * 视频播放状态：暂停状态
     */
    private void uiStatePause() {
        mPausing = true;
        ExoPlayerManager.getInstance().setExoPlayWhenRead(false);
        ivStart.setVisibility(View.INVISIBLE);
        progressBarLoading.setVisibility(View.INVISIBLE);
        exoLayoutBottom.setVisibility(View.INVISIBLE);
        exoBottomProgressbar.setVisibility(View.INVISIBLE);
        ivPause.setVisibility(View.VISIBLE);
        ivToPause.setVisibility(View.INVISIBLE);
        ivVideoBg.setVisibility(View.VISIBLE);
    }

    /**
     * 视频状态：画质状态
     */
    private void uiStateImageQuality() {
        Format format = ExoPlayerManager.getInstance().getVideoSelectFormat();
        if(null != format) {
            tvVideoTrack.setVisibility(View.VISIBLE);
            tvVideoTrack.setText(format.width + "*" + format.height);
        }
    }

    /**
     * 更新当前的进度，并发送个延时消息，更新进度。
     */
    public void updateProgress() {
        ExoPlayerManager exoPlayerManager = ExoPlayerManager.getInstance();
        SimpleExoPlayer simpleExoPlayer = exoPlayerManager.getSimpleExoPlayer();
        long duration = null == simpleExoPlayer? 0 :
                exoPlayerManager.getSimpleExoPlayer().getDuration();
        long position = null == simpleExoPlayer? 0 :
                exoPlayerManager.getSimpleExoPlayer().getCurrentPosition();
        if(null != exoDuration) {
            exoDuration.setText(stringForTime(duration));
        }
        if(null != exoPosition && !mDragging) {
            exoPosition.setText(stringForTime(position));
        }
        if(null != exoProgress && !mDragging) {
            exoProgress.setProgress(progressBarValue(position, duration));
            long bufferedPosition = null == simpleExoPlayer? 0 :
                    exoPlayerManager.getSimpleExoPlayer().getBufferedPosition();
            exoProgress.setSecondaryProgress(progressBarValue(bufferedPosition, duration));
            if(null != exoBottomProgressbar) {
                exoBottomProgressbar.setProgress(exoProgress.getProgress());
                exoBottomProgressbar.setSecondaryProgress(exoProgress.getSecondaryProgress());
            }
        }

        int playbackState = null == simpleExoPlayer ? ExoPlayer.STATE_IDLE :
                simpleExoPlayer.getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            long delayMs;
            if (simpleExoPlayer.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            postDelayed(updateProgressRunnable, delayMs);
        }

    }

    /**
     * 更新时间的任务
     */
    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    /**
     * SeekBar拖动事件
     */
    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            ExoPlayerManager exoPlayerManager = ExoPlayerManager.getInstance();
            SimpleExoPlayer simpleExoPlayer = exoPlayerManager.getSimpleExoPlayer();
            if(null == simpleExoPlayer) {
                return;
            }
            if (fromUser && null != exoPosition) {
                exoPosition.setText(stringForTime(
                        progressToTime(progress, simpleExoPlayer.getDuration())));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //开始拖动的时候，移时定时更新
            mDragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            ExoPlayerManager exoPlayerManager = ExoPlayerManager.getInstance();
            SimpleExoPlayer simpleExoPlayer = exoPlayerManager.getSimpleExoPlayer();
            if(null == simpleExoPlayer) {
                return;
            }
            simpleExoPlayer.seekTo(simpleExoPlayer.getCurrentWindowIndex(),
                    progressToTime(seekBar.getProgress(), simpleExoPlayer.getDuration()));
        }
    };

    /**
     * 点击事件
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //开始播放
            case R.id.start:
                clickToStart();
                break;
            //暂停：点击播放
            case R.id.iv_pause:
                clickToPause();
                break;
            //暂停：点击播放
            case R.id.iv_to_pause:
                mPausing = true;
                mTouched = false;
                if(ExoPlayerLayoutManager.getInstance().getCurrentJcvd() != this) {
                    clickToStart();
                } else {
                    setUIState(UI_VIDEO_PAUSING);
                }
                break;
            //全屏或者恢复原来的高度
            case R.id.fullscreen:
                //全屏
                if(null == ExoPlayerLayoutManager.getInstance().getSecondFloor()) {
//                    ivFullscreen.setBackgroundResource(R.mipmap.exo_layout_not_full_screen);
                    fullVideo();
                } else {
//                    ivFullscreen.setBackgroundResource(R.mipmap.exo_layout_full_screen);
                    //恢复原来的高度
                    ExoPlayerLayoutManager.getInstance().handleBack();
                }
                break;
            //视频画质选择
            case R.id.tv_video_track:
                showVideoImageQulity();
                break;
        }
    }

    /**
     * 开始播放视频
     */
    public void clickToStart() {
        if(TextUtils.isEmpty(mUrl)) {
            Toast.makeText(mContext, "播放的地址不得为空", Toast.LENGTH_SHORT).show();
        }
        ExoPlayerManager.getInstance().setUrl(mUrl);
        setUIState(UI_VIDEO_STATE_BUFFERING);
        setSurfaceContainerClick();
        if(ExoPlayerLayoutManager.getInstance().getCurrentJcvd() != this || null == ExoPlayerManager.getInstance().getSimpleExoPlayer()) {
            eventPreparePlay();
            ExoPlayerManager.getInstance().preparePlayer(mContext, ExoPlayerManager.getInstance().getUrl());
        }
    }

    /**
     * 设置视频可点击
     */
    public void setSurfaceContainerClick() {
        surfaceContainer.setOnTouchListener(surfaceListener);
        surfaceContainer.setOnClickListener(this);
    }

    /**
     * 暂停状态点击再播放
     */
    private void clickToPause() {
        mPausing = false;
        mTouched = false;
        if(ExoPlayerLayoutManager.getInstance().getCurrentJcvd() != this) {
            clickToStart();
        } else {
            ExoPlayerManager exoPlayerManager = ExoPlayerManager.getInstance();
            SimpleExoPlayer simpleExoPlayer = exoPlayerManager.getSimpleExoPlayer();
            if(null == simpleExoPlayer) {
                ExoPlayerLayoutManager.getInstance().setFirstFloor(null);
                ExoPlayerLayoutManager.getInstance().setSecondFloor(null);
                clickToStart();
                return;
            }
            if(simpleExoPlayer.getDuration() - simpleExoPlayer.getCurrentPosition() >= 1000) {
                exoPlayerManager.setExoPlayWhenRead(true);
                setUIState(UI_VIDEO_PLAYING);
            } else {
                removeCallbacks(updateProgressRunnable);
                exoPlayerManager.setExoPlayWhenRead(true);
                simpleExoPlayer.seekTo(0L);
                setUIState(UI_VIDEO_PLAYING);
                updateProgress();
            }
        }
    }

    /**
     * 弹出可选择的视频画质
     */
    private void showVideoImageQulity() {
        ExoPlayerManager exoPlayerManager = ExoPlayerManager.getInstance();
        if(null == exoPlayerManager || null == exoPlayerManager.getSimpleExoPlayer()) {
            return;
        }
        DefaultTrackSelector defaultTrackSelector = exoPlayerManager.getDefaultTrackSelector();
        TrackSelection.Factory factory = exoPlayerManager.getVideoTrackSelectionFactory();
        if(null == defaultTrackSelector || null == factory) {
            return;
        }
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo =  defaultTrackSelector.getCurrentMappedTrackInfo();
        for(int i = 0; i < mappedTrackInfo.length; i++) {
            if(exoPlayerManager.getSimpleExoPlayer().getRendererType(i) ==
                    C.TRACK_TYPE_VIDEO) {
                TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(i);
                StringBuilder sb = new StringBuilder();
                for(int j = 0; j < trackGroupArray.length; j++) {
                    TrackGroup trackGroup = trackGroupArray.get(j);
                    for(int k = 0; k < trackGroup.length; k++) {
                        Format format = trackGroup.getFormat(k);
                        sb.append(format.width + "*" + format.height + " ");
                    }
                }
                Log.e("showVideoImageQulity", sb.toString());
                break;
            }
        }
    }

    /**
     * 全屏操作，横屏
     */
    @SuppressWarnings("unchecked")
    private void fullVideo() {
        ((Activity)mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ViewGroup decorView = (ViewGroup) ((Activity)mContext).findViewById(Window.ID_ANDROID_CONTENT);
        Constructor<ExoPlayerLayout> constructor = null;
        removeTextureView();
        try {
            constructor = (Constructor<ExoPlayerLayout>) ExoPlayerLayout.this.getClass().getConstructor(Context.class);
            mFullScreenExoPlayerLayout = constructor.newInstance(mContext);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            decorView.addView(mFullScreenExoPlayerLayout, lp);
            ExoPlayerLayoutManager.getInstance().setSecondFloor(mFullScreenExoPlayerLayout);
            mFullScreenExoPlayerLayout.setUIState(UI_VIDEO_PLAYING);
            mFullScreenExoPlayerLayout.setUIState(UI_VIDEO_IMAGE_QUALITY);
            mFullScreenExoPlayerLayout.updateProgress();
            mFullScreenExoPlayerLayout.setUrl(mUrl);
            mFullScreenExoPlayerLayout.setImageUrl(mImageUrl);
            mFullScreenExoPlayerLayout.addTextureView();
            mFullScreenExoPlayerLayout.setSurfaceContainerClick();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 恢复竖屏
     */
    public void notFullVideo() {
        ((Activity)mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ExoPlayerLayoutManager.getInstance().getSecondFloor().removeTextureView();
        ExoPlayerLayoutManager.getInstance().getFirstFloor().addTextureView();
        ViewGroup decorView = (ViewGroup) ((Activity)mContext).findViewById(Window.ID_ANDROID_CONTENT);
        decorView.removeView(mFullScreenExoPlayerLayout);
        ExoPlayerLayoutManager.getInstance().setSecondFloor(null);
    }

    /**
     * 把时间转化为
     * @param timeMs 时间
     * @return
     */
    private String stringForTime(long timeMs) {
        if (timeMs == C.TIME_UNSET) {
            timeMs = 0;
        }
        long totalSeconds = (timeMs + 500) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        formatBuilder.setLength(0);
        return hours > 0 ? formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
                : formatter.format("%02d:%02d", minutes, seconds).toString();
    }

    /**
     * 处理进度
     * @param position 当前位置
     * @param duration 全部时长
     * @return
     */
    private int progressBarValue(long position, long duration) {
        return duration == C.TIME_UNSET || duration == 0 ? 0
                : (int) ((position * PROGRESS_BAR_MAX) / duration);
    }

    private long progressToTime(int progress, long duration) {
        return progress * duration / PROGRESS_BAR_MAX;
    }

    //--------------------------------------------处理surface_container点击事件----------------------
    private OnTouchListener surfaceListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if(ExoPlayerLayoutManager.getInstance().getCurrentJcvd() != ExoPlayerLayout.this) {
              return false;
            }
            removeCallbacks(touchRunnable);
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    mTouched = true;
                    SimpleExoPlayer simpleExoPlayer = ExoPlayerManager.getInstance().getSimpleExoPlayer();
                    if(null != simpleExoPlayer && simpleExoPlayer.getPlayWhenReady()) {
                        setUIState(UI_VIDEO_TOUCHED);
                        ExoPlayerLayout.this.postDelayed(touchRunnable, DEFAULT_SHOW_TIMEOUT_MS);
                    }
                    break;
            }
            return false;
        }
    };
    /**
     * 多长时间取消touched状态
     */
    private Runnable touchRunnable = new Runnable() {
        @Override
        public void run() {
            mTouched = false;
            setUIState(UI_VIDEO_PLAYING);
        }
    };

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = imageUrl;
        if(null != ivVideoBg && !TextUtils.isEmpty(mImageUrl)) {
            if(null == ExoPlayerLayoutManager.getInstance().getFirstFloor()) {
                Glide.with(VideoApplication.getContext()).load(mImageUrl).into(ivVideoBg);
            }
        }
    }

    public ExoPlayerLayout getFullScreenExoPlayerLayout() {
        return mFullScreenExoPlayerLayout;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(null != ivVideoBg) {
            ivVideoBg.setVisibility(View.VISIBLE);
            Glide.with(VideoApplication.getContext()).load(mImageUrl).into(ivVideoBg);
        }
    }
}
