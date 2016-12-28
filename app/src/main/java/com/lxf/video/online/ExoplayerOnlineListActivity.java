package com.lxf.video.online;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lxf.video.DividerUtil;
import com.lxf.video.R;
import com.lxf.video.VideoConstant;
import com.lxf.video.adapter.ExoPlayerVideoAdapter;
import com.lxf.video.bean.VideoBean;

import java.util.ArrayList;
import java.util.List;

/**
 * User: luoxf
 * Date: 2016-12-26
 * Time: 09:40
 */
public class ExoplayerOnlineListActivity extends AppCompatActivity {
    private RecyclerView rvVideoList;
    private ExoPlayerVideoAdapter mAdapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer_list);
        initRecyclerView();
    }

    private void initRecyclerView() {
        rvVideoList = (RecyclerView) findViewById(R.id.rv_exoplayer_video);
        rvVideoList.setLayoutManager(new LinearLayoutManager(this));
        rvVideoList.setHasFixedSize(true);
        rvVideoList.addItemDecoration(DividerUtil.getDividerItemDecoration(this));
        mAdapter = new ExoPlayerVideoAdapter(this, R.layout.item_exoplayer_list);
        rvVideoList.setAdapter(mAdapter);
        List<VideoBean> videoBeenList = new ArrayList<>();
        for(int i = 0; i < VideoConstant.videoUrls.length; i++) {
            String[] urls = VideoConstant.videoUrls[i];
            for(int j = 0; j < urls.length; j++) {
                String url = urls[j];
                VideoBean videoBean = new VideoBean();
                videoBean.setVideoUrl(url);
                videoBean.setVideoThumb(VideoConstant.videoThumbs[i][j]);
                videoBean.setVideoTitle(VideoConstant.videoTitles[i][j]);
                videoBeenList.add(videoBean);
            }
        }
        mAdapter.changeData(videoBeenList);
    }
}
