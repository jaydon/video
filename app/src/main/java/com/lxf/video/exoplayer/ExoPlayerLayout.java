package com.lxf.video.exoplayer;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.lxf.video.R;

/**
 * 播放视频的layout
 * User: luoxf
 * Date: 2016-12-27
 * Time: 21:48
 */
public class ExoPlayerLayout extends FrameLayout {
    private Context mContext;
    private ViewGroup surfaceContainer;                     //视频加载块
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener;
    private String mUrl;                                    //播放地址
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
        removeTextureView();
        removeSupportAudio();
    }

    /**
     * 准备播放,先移除掉上一个TextureView，添加TextureView,添加音频保存这个ExoPlayerLayout
     */
    public void eventPreparePlay(String url) {
        //首先清除其它播放，让它恢复播放前的view
        this.mUrl = url;
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
        exoPlayerManager.setUrl(mUrl);
        //把player通过surfaceTextureListener联系在一起
        exoPlayerManager.getTextureView().setSurfaceTextureListener(exoPlayerManager);
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
    private void releaseAllVideos() {
        ExoPlayerManager.getInstance().releasePlayer();
        ExoPlayerLayoutManager.getInstance().completeAll();
    }

}
