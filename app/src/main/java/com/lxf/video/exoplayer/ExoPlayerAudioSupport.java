package com.lxf.video.exoplayer;

import android.content.Context;
import android.media.AudioManager;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;

/**
 * 这里处理音频
 * User: luoxf
 * Date: 2017-01-06
 * Time: 18:07
 */
public class ExoPlayerAudioSupport {
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener;
    private Context mContext;
    private static ExoPlayerAudioSupport mInstance;
    private AudioManager mAudioManager;
    private ExoPlayerAudioSupport(Context context) {
        this.mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }
    /**
     * 单例获取ExoPlayerManager
     * @return ExoPlayerManager
     */
    public static ExoPlayerAudioSupport getInstance(Context context) {
        if (mInstance == null) {
            synchronized (ExoPlayerAudioSupport.class) {
                if (mInstance == null) {
                    mInstance = new ExoPlayerAudioSupport(context);
                }
            }
        }
        return mInstance;
    }
    /**
     * 添加支持音频
     */
    public void addSupportAudio() {

        mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        ExoPlayerLayoutManager.getInstance().getCurrentJcvd().releaseAllVideos();
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
        mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    /**
     * 播放完成，关掉audio
     */
    public void removeSupportAudio() {
        if(null != mAudioFocusChangeListener) {
            mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        }
    }
}
