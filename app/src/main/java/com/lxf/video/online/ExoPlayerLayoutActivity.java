package com.lxf.video.online;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.lxf.video.R;
import com.lxf.video.exoplayer.ExoPlayerLayout;

/**
 * 用于测试ExoPlayerLayout
 * User: luoxf
 * Date: 2016-12-28
 * Time: 15:12
 */
public class ExoPlayerLayoutActivity extends AppCompatActivity{
    private String path = "http://125.39.142.86/data2/video09/2016/03/01/3871799-102-1615.mp4";
    private ExoPlayerLayout exoPlayerLayout;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer_layout);
        exoPlayerLayout = (ExoPlayerLayout) findViewById(R.id.exo_player_layout);
        exoPlayerLayout.eventPreparePlay(path);
    }
}
