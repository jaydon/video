package com.lxf.video.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import com.lxf.video.bean.VideoBean;
import com.lxf.video.exoplayer.ExoPlayerLayout;
import com.lxf.video.exoplayer.ExoPlayerManager;

/**
 * User: luoxf
 * Date: 2016-12-26
 * Time: 10:21
 */
public class ExoPlayerVideoAdapter extends BaseRecyclerViewAdapter<VideoBean> {

    public ExoPlayerVideoAdapter(Context context, int layoutResId) {
        super(context, layoutResId);
    }

    @Override
    protected void convert(BaseViewHolder helper, final VideoBean item, final int position, int viewType) {
        final ExoPlayerLayout exoPlayerLayout = helper.getView(R.id.exo_player_layout);
        final ImageView ivVideoBg = helper.getView(R.id.iv_video_bg);
        ImageLoaderGlideUtil.displayImage(ivVideoBg, item.getVideoUrl(), R.mipmap.ic_launcher);
        TextView tvVideoTitle = helper.getView(R.id.tv_video_title);
        tvVideoTitle.setText(item.getVideoTitle());
        final ImageView ivResumeStart = helper.getView(R.id.iv_resume_start);
        ivResumeStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyDataSetChanged();
                initializePlayer(exoPlayerLayout, item);
            }
        });
    }

    private void initializePlayer(ExoPlayerLayout exoPlayerLayout, VideoBean videoBean) {
        exoPlayerLayout.eventPreparePlay(videoBean.getVideoUrl());
    }

}
