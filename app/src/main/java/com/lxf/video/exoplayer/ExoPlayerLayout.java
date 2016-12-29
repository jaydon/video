package com.lxf.video.exoplayer;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.lxf.video.R;

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
    public final static int UI_VIDEO_STATE_Buffering = 1;   //视频状态：点击开始，缓冲状态
    public final static int UI_VIDEO_PLAYING = 2;           //视频状态：播放状态
    public final static int UI_VIDEO_PAUSING = 3;           //视频状态：暂停状态
    public final static int UI_VIDEO_END_PLAY = 4;          //视频状态：播放完成状态
    public final static int UI_VIDEO_TOUCHED = 5;           //视频状态：播放状态，timeline等，属于触模后的一种动态
    private static final int PROGRESS_BAR_MAX = 100;       //progress 最大值
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5000;//Touched状态多长时间消失
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

    private boolean mDragging;                              //用来表示SeekBar是否在拖动中。
    private boolean mTouched;                               //用来表示控制状态
    private boolean mPausing;                               //是否停止播放
    private StringBuilder formatBuilder = new StringBuilder();
    private Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener;
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
    public void eventCompletPlay() {
        removeCallbacks(touchRunnable);
        removeCallbacks(updateProgressRunnable);
        removeTextureView();
        removeSupportAudio();
    }

    /**
     * 准备播放,先移除掉上一个TextureView，添加TextureView,添加音频保存这个ExoPlayerLayout
     */
    public void eventPreparePlay() {
        //首先清除其它播放，让它恢复播放前的view
        ExoPlayerManager.getInstance().releasePlayer();
        ExoPlayerLayoutManager.getInstance().completeAll();
        initTextureView();
        addTextureView();
        addSupportAudio();
        ExoPlayerLayoutManager.getInstance().setFirstFloor(this);
    }

    /**
     * 添加支持音频
     */
    private void addSupportAudio() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        releaseAllVideos();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        ExoPlayerManager exoPlayerManager = ExoPlayerManager.getInstance();
                        SimpleExoPlayer simpleExoPlayer = exoPlayerManager.getSimpleExoPlayer();
                        //暂停播放
                        if(null != simpleExoPlayer && simpleExoPlayer.getPlaybackState() == ExoPlayer.STATE_READY) {
                            simpleExoPlayer.setPlayWhenReady(false);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        break;
                }
            }
        };
        audioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    /**
     * 播放完成，关掉audio
     */
    private void removeSupportAudio() {
        if(null != mAudioFocusChangeListener) {
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            audioManager.abandonAudioFocus(mAudioFocusChangeListener);
        }
    }

    /**
     * 初始化播放view
     */
    private void initTextureView() {
        ExoPlayerManager exoPlayerManager = ExoPlayerManager.getInstance();
        exoPlayerManager.setTextureView(new TextureView(mContext));

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
            if(null != textureView.getParent()) {
                ((ViewGroup)textureView.getParent()).removeView(textureView);
            }
            exoPlayerManager.setTextureView(null);
        }
    }

    /**
     * 释放exoplayer和ExoplayerLayout的相关播放设置 eventCompletPlay();
     */
    public static void releaseAllVideos() {
        ExoPlayerManager.getInstance().releasePlayer();
        ExoPlayerLayoutManager.getInstance().completeAll();
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
            case UI_VIDEO_STATE_Buffering:
                uiStateBuffering();
                break;
            case UI_VIDEO_PLAYING:
                uiStateReady();
                break;
            case UI_VIDEO_PAUSING:
                uiStatePause();
                break;
            case UI_VIDEO_END_PLAY:
                break;
            case UI_VIDEO_TOUCHED:
                uiStateTouched();
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
    }

    /**
     * 视频播放缓冲状态
     */
    private void uiStateBuffering() {
        ivStart.setVisibility(View.INVISIBLE);
        progressBarLoading.setVisibility(View.VISIBLE);
        exoLayoutBottom.setVisibility(View.INVISIBLE);
        exoBottomProgressbar.setVisibility(View.INVISIBLE);
        ivPause.setVisibility(View.INVISIBLE);
    }

    /**
     * 视频播放可以播放状态
     */
    private void uiStateReady() {
       if(!mTouched && !mPausing) {
           ivStart.setVisibility(View.INVISIBLE);
           progressBarLoading.setVisibility(View.INVISIBLE);
           exoLayoutBottom.setVisibility(View.INVISIBLE);
           exoBottomProgressbar.setVisibility(View.VISIBLE);
           ivPause.setVisibility(View.INVISIBLE);
       }
    }

    /**
     * 视频播放状态：触摸状态
     */
    private void uiStateTouched() {
        ivStart.setVisibility(View.INVISIBLE);
        progressBarLoading.setVisibility(View.INVISIBLE);
        exoLayoutBottom.setVisibility(View.VISIBLE);
        exoBottomProgressbar.setVisibility(View.INVISIBLE);
        ivPause.setVisibility(View.INVISIBLE);
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
                setUIState(UI_VIDEO_STATE_Buffering);
                surfaceContainer.setOnTouchListener(surfaceListener);
                surfaceContainer.setOnClickListener(this);
                if(ExoPlayerLayoutManager.getInstance().getCurrentJcvd() != this) {
                    eventPreparePlay();
                    ExoPlayerManager.getInstance().preparePlayer(mContext, ExoPlayerManager.getInstance().getUrl());
                } else {
                    ExoPlayerManager.getInstance().getSimpleExoPlayer().setPlayWhenReady(true);
                }

                break;
            case R.id.iv_pause:
                mPausing = false;
                ExoPlayerManager.getInstance().setExoPlayWhenRead(true);
                setUIState(UI_VIDEO_STATE_Buffering);
                break;
        }
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
            removeCallbacks(touchRunnable);
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    mTouched = true;
                    setUIState(UI_VIDEO_TOUCHED);
                    ExoPlayerLayout.this.postDelayed(touchRunnable, DEFAULT_SHOW_TIMEOUT_MS);
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

}