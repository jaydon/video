package com.lxf.video.online;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.lxf.video.R;
import com.lxf.video.exoplayer.ExoPlayerLayout;
import com.lxf.video.exoplayer.ExoPlayerManager;

/**
 * 用于测试ExoPlayerLayout
 * User: luoxf
 * Date: 2016-12-28
 * Time: 15:12
 */
public class ExoPlayerLayoutActivity extends AppCompatActivity{
    private String path = "http://video.jiecao.fm/8/17/bGQS3BQQWUYrlzP1K4Tg4Q__.mp4";
    private ExoPlayerLayout exoPlayerLayout;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer_layout);
        exoPlayerLayout = (ExoPlayerLayout) findViewById(R.id.exo_player_layout);
        ExoPlayerManager.getInstance().setUrl(path);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        exoPlayerLayout.setUIState(ExoPlayerLayout.UI_VIDEO_PAUSING);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ExoPlayerLayout.releaseAllVideos();
    }
}
