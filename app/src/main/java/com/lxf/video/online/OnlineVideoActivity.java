package com.lxf.video.online;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.danikula.videocache.HttpProxyCacheServer;
import com.dinuscxj.progressbar.CircleProgressBar;
import com.lxf.video.R;
import com.lxf.video.VideoApplication;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.CenterLayout;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

/**
 * 在线播放视频
 * User: luoxf
 * Date: 2016-12-19
 * Time: 21:39
 */
public class OnlineVideoActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {
    private VideoView video;
    private CircleProgressBar pbBefore;
    private TextView tvFullLayout;
    private MediaController mediaController;
    private ImageView ivResumeStart;                            //重播
    private CenterLayout centerLayout;

    private String path = "http://125.39.142.86/data2/video09/2016/03/01/3871799-102-1615.mp4";
    private Uri uri;
//    private String path = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vitamio.isInitialized(getApplicationContext());
        setContentView(R.layout.activity_online_video);

        video = (VideoView) findViewById(R.id.video);
        pbBefore = (CircleProgressBar) findViewById(R.id.pb_before);
        mediaController = (MediaController) findViewById(R.id.media_controller);
        tvFullLayout = (TextView) findViewById(R.id.tv_full_layout);
        tvFullLayout.setOnClickListener(this);
        ivResumeStart = (ImageView) findViewById(R.id.iv_resume_start);
        ivResumeStart.setOnClickListener(this);
        centerLayout = (CenterLayout) findViewById(R.id.centerlayout);
        initVideoView();
    }

    private void initVideoView() {
        HttpProxyCacheServer proxy = VideoApplication.getProxy(this);
        String proxyUrl = proxy.getProxyUrl(path);
        uri = Uri.parse(proxyUrl);
        video.setVideoURI(uri);
        video.setMediaController(mediaController);
        //播放完成显示播放按钮
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                ivResumeStart.setVisibility(View.VISIBLE);
            }
        });
        video.requestFocus();
        video.setOnInfoListener(this);
        video.setOnBufferingUpdateListener(this);
        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // optional need Vitamio 4.0
                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        pbBefore.setProgress(percent);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                if(video.isPlaying()) {
                    video.pause();
                }
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                bufferingEnd();
                break;
            case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                break;
            case MediaPlayer.MEDIA_INFO_FILE_OPEN_OK:
                bufferingStart();
                break;
        }
        return true;
    }

    /**
     * 缓冲结束所做的操作
     * 关闭加载框，视频可以点击，开始播放视频，类似今日头条
     */
    private void bufferingEnd() {
        video.setClickable(true);
        pbBefore.setVisibility(View.GONE);
        video.start();
    }

    /**
     * 缓冲开始播放视频前所做的操作
     */
    private void bufferingStart() {
        video.setClickable(false);
        pbBefore.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_full_layout:
                fullVideo();
                break;
            //重播
            case R.id.iv_resume_start:
                ivResumeStart.setVisibility(View.GONE);
                video.seekTo(0L);
                video.start();
                break;
        }
    }

    /**
     * 全屏操作，横屏
     */
    private void fullVideo() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        centerLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        video.setVideoLayout(VideoView.VIDEO_LAYOUT_STRETCH, 0);
    }

    /**
     * 恢复竖屏
     */
    private void notFullVideo() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        centerLayout .setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.video_height)));
        video.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
    }

    @Override
    public void onBackPressed() {
        //如果是横屏先切换成竖屏
        if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            notFullVideo();
        } else {
            video.stopPlayback();
            super.onBackPressed();
        }

    }
}
