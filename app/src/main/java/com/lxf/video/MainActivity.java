package com.lxf.video;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lxf.video.online.OnlineVideoActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView tvPlayVideo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvPlayVideo = (TextView) findViewById(R.id.tv_play_video);
        tvPlayVideo.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_play_video:
                startActivity(new Intent(this, OnlineVideoActivity.class));
                break;
        }
    }
}
