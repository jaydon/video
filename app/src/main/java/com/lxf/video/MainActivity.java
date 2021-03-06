package com.lxf.video;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lxf.video.online.ExoPlayerLayoutActivity;
import com.lxf.video.online.ExoplayerOnlineListActivity;
import com.lxf.video.online.ExoplayerOnlineVideoActivity;
import com.lxf.video.online.VitamioOnlineVideoActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView tvVitamioPlayVideo;            //vitamio
    private TextView tvExoplayerPlayVideo;          //exoplayer
    private TextView tvExoplayerList;               //在线视频列表
    private TextView tvExoplayerLayout;             //测试ExoplayerLayout
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvVitamioPlayVideo = (TextView) findViewById(R.id.tv_vitamio_play_video);
        tvVitamioPlayVideo.setOnClickListener(this);
        tvExoplayerPlayVideo = (TextView) findViewById(R.id.tv_exoplayer_play_video);
        tvExoplayerPlayVideo.setOnClickListener(this);
        tvExoplayerList = (TextView) findViewById(R.id.tv_exoplayer_list);
        tvExoplayerList.setOnClickListener(this);
        tvExoplayerLayout = (TextView) findViewById(R.id.tv_exoplayer_layout);
        tvExoplayerLayout.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_vitamio_play_video:
                startActivity(new Intent(this, VitamioOnlineVideoActivity.class));
                break;
            case R.id.tv_exoplayer_play_video:
                startActivity(new Intent(this, ExoplayerOnlineVideoActivity.class));
                break;
            case R.id.tv_exoplayer_list:
                startActivity(new Intent(this, ExoplayerOnlineListActivity.class));
                break;
            case R.id.tv_exoplayer_layout:
                startActivity(new Intent(this, ExoPlayerLayoutActivity.class));
                break;
        }
    }
}
