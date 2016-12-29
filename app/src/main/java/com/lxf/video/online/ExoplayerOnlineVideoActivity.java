package com.lxf.video.online;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.lxf.video.ImageLoaderGlideUtil;
import com.lxf.video.R;
import com.lxf.video.VideoApplication;


/**
 * User: luoxf
 * Date: 2016-12-22
 * Time: 09:08
 */
public class ExoplayerOnlineVideoActivity extends AppCompatActivity implements View.OnClickListener, ExoPlayer.EventListener {
    private RelativeLayout rlVideo;
    private SimpleExoPlayerView simpleExoPlayerView;
    private TextView tvFullLayout;
    private ImageButton exoPlay;                                       //播放
    private ImageButton exoPause;                                      //暂停
    private ImageView ivResumeStart;                                   //重播
    private ImageView ivVideoBg;

    private SimpleExoPlayer player;
    private boolean isTimelineStatic;
    private DefaultBandwidthMeter bandwidthMeter;
    private String path = "http://video.jiecao.fm/8/17/bGQS3BQQWUYrlzP1K4Tg4Q__.mp4";
    private Uri uri;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_exoplayer);
        rlVideo = (RelativeLayout) findViewById(R.id.rv_video);
        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.simpleExoPlayerView);
        tvFullLayout = (TextView) findViewById(R.id.tv_full_layout);
        tvFullLayout.setOnClickListener(this);
        tvFullLayout.setVisibility(View.VISIBLE);
        exoPlay = (ImageButton) findViewById(R.id.exo_play);
        exoPause = (ImageButton) findViewById(R.id.exo_pause);
        ivResumeStart = (ImageView) findViewById(R.id.iv_resume_start);
        ivResumeStart.setOnClickListener(this);
        ivVideoBg = (ImageView) findViewById(R.id.iv_video_bg);
        initIntentData();
    }

    private void initIntentData() {
        isTimelineStatic = false;
        initializePlayer();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if (Util.SDK_INT > 23) {
//            initializePlayer();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(null != player) {
            player.setPlayWhenReady(true);
        }
//        if ((Util.SDK_INT <= 23 || player == null)) {
//            initializePlayer();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(null != player) {
            player.setPlayWhenReady(false);
        }
//        if (Util.SDK_INT <= 23) {
//            releasePlayer();
//        }
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if (Util.SDK_INT > 23) {
//            releasePlayer();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        releasePlayer();
        initIntentData();
    }

    /**初始化视频播放器
     *
     */
    private void initializePlayer() {
        bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        // 3. Create the player
         player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

        simpleExoPlayerView.setPlayer(player);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, VideoApplication.class.getSimpleName()), bandwidthMeter);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        HttpProxyCacheServer proxy = VideoApplication.getProxy(this);
        String proxyUrl = proxy.getProxyUrl(path);
        uri = Uri.parse(proxyUrl);
        MediaSource videoSource = new ExtractorMediaSource(uri,
                dataSourceFactory, extractorsFactory, null, null);
        //暂停播放使用
        // Prepare the player with the source.
        player.addListener(this);
        player.setPlayWhenReady(true);
        player.prepare(videoSource);
    }

    /**
     * 释放视频播放器
     */
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_full_layout:
                fullVideo();
                break;
            case R.id.exo_play:
                break;
            case R.id.exo_pause:
                break;
            case R.id.iv_resume_start:
                ivResumeStart.setVisibility(View.GONE);
                player.setPlayWhenReady(true);
                player.seekTo(0L);
                break;
        }
    }

    /**
     * 全屏操作，横屏
     */
    private void fullVideo() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        simpleExoPlayerView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /**
     * 恢复竖屏
     */
    private void notFullVideo() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        simpleExoPlayerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.video_height)));
    }

    @Override
    public void onBackPressed() {
        //如果是横屏先切换成竖屏
        if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            notFullVideo();
        } else {
            releasePlayer();
            super.onBackPressed();
        }

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        if(isLoading) {
            ivVideoBg.setVisibility(View.VISIBLE);
            ImageLoaderGlideUtil.displayImage(ivVideoBg,
                    "http://img4.jiecaojingxuan.com/2016/8/17/bd7ffc84-8407-4037-a078-7d922ce0fb0f.jpg",
                    R.mipmap.ic_launcher);
        } else {
            ivVideoBg.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        //播放完成
        if(playbackState == ExoPlayer.STATE_ENDED) {
            exoPause.setVisibility(View.GONE);
            exoPlay.setVisibility(View.VISIBLE);
            ivResumeStart.setVisibility(View.VISIBLE);
        } else if (playbackState == ExoPlayer.STATE_READY) {
            //可以播放
            player.setPlayWhenReady(true);
        } else if(playbackState == ExoPlayer.STATE_BUFFERING) {
            //缓冲完成

        } else if(playbackState == ExoPlayer.STATE_IDLE) {

        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }
}
