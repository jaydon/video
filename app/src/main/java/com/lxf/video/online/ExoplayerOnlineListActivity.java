package com.lxf.video.online;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.lxf.video.DividerUtil;
import com.lxf.video.R;
import com.lxf.video.VideoConstant;
import com.lxf.video.adapter.ExoPlayerVideoAdapter;
import com.lxf.video.bean.VideoBean;
import com.lxf.video.event.ExoPlayerLayoutChangeEvent;
import com.lxf.video.exoplayer.ExoPlayerLayout;
import com.lxf.video.exoplayer.ExoPlayerLayoutManager;
import com.lxf.video.exoplayer.ExoPlayerManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

        rvVideoList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int _firstItemPosition = -1, _lastItemPosition;
            View fistView, lastView;
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(newState == RecyclerView.SCROLL_STATE_IDLE) {

                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                //判断是当前layoutManager是否为LinearLayoutManager
                // 只有LinearLayoutManager才有查找第一个和最后一个可见view位置的方法
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                    //获取最后一个可见view的位置
                    int lastItemPosition = linearManager.findLastVisibleItemPosition();
                    //获取第一个可见view的位置
                    int firstItemPosition = linearManager.findFirstVisibleItemPosition();
                    //获取可见view的总数
                    int visibleItemCount = linearManager.getChildCount();

                    if (_firstItemPosition < firstItemPosition) {
                        _firstItemPosition = firstItemPosition;
                        _lastItemPosition = lastItemPosition;
                        GCView(fistView);
                        fistView = recyclerView.getChildAt(0);
                        lastView = recyclerView.getChildAt(visibleItemCount - 1);
                    } else if (_lastItemPosition > lastItemPosition) {
                        _firstItemPosition = firstItemPosition;
                        _lastItemPosition = lastItemPosition;
                        GCView(lastView);
                        fistView = recyclerView.getChildAt(0);
                        lastView = recyclerView.getChildAt(visibleItemCount - 1);
                    }
                }
            }
            /**
             * 回收播放
             * @param gcView
             */
            public void GCView(View gcView) {
                ExoPlayerLayout exoPlayerLayout = ExoPlayerLayoutManager.getInstance().getCurrentJcvd();
                if(null != gcView && exoPlayerLayout == gcView.findViewById(R.id.exo_player_layout)) {
                    exoPlayerLayout.releaseAllVideos();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ExoPlayerLayoutManager.getInstance().handlePausingUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ExoPlayerLayoutManager.getInstance().releaseAllVideos();
    }

    @Override
    public void onBackPressed() {
        if(!ExoPlayerLayoutManager.getInstance().handleBack()) {
            super.onBackPressed();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ExoPlayerLayoutChangeEvent event) {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
