package com.lxf.video.exoplayer;

/**
 * 单例模式
 * 管理ExoPlayerLayout，可能会有两个Layout，一个是用于本身，一个用于全屏或者弹窗
 * User: luoxf
 * Date: 2016-12-28
 * Time: 11:50
 */
public class ExoPlayerLayoutManager {
    private static ExoPlayerLayoutManager mInstance;
    private ExoPlayerLayout mFirstExoPlayer;
    private ExoPlayerLayout mSecondExoPlayer;
    private ExoPlayerLayoutManager(){}
    public static ExoPlayerLayoutManager getInstance() {
        if (mInstance == null) {
            synchronized (ExoPlayerLayoutManager.class) {
                if (mInstance == null) {
                    mInstance = new ExoPlayerLayoutManager();
                }
            }
        }
        return mInstance;
    }

    public void setFirstFloor(ExoPlayerLayout exoPlayerLayout) {
        this.mFirstExoPlayer = exoPlayerLayout;
    }

    public void setSecondFloor(ExoPlayerLayout exoPlayerLayout) {
        this.mSecondExoPlayer = exoPlayerLayout;
    }

    public ExoPlayerLayout getFirstFloor() {
        return mFirstExoPlayer;
    }

    public ExoPlayerLayout getSecondFloor() {
        return mSecondExoPlayer;
    }

    public ExoPlayerLayout getCurrentJcvd() {
        if (getSecondFloor() != null) {
            return getSecondFloor();
        }
        return getFirstFloor();
    }

    public void completeAll() {
        if (mSecondExoPlayer != null) {
            mSecondExoPlayer.eventCompletPlay();
            mSecondExoPlayer = null;
        }
        if (mSecondExoPlayer != null) {
            mSecondExoPlayer.eventCompletPlay();
            mSecondExoPlayer = null;
        }
    }
}
