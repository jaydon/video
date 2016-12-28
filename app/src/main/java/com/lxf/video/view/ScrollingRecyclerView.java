package com.lxf.video.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * 增加是否在滑动中的判断
 * adapter需要实现{@link ScrollingCallBack}，再把它设置进setScrollingCallBack（）;
 * 如： 中的isScrolling
 * User: luoxf
 * Date: 2016-12-09
 * Time: 11:33
 */
public class ScrollingRecyclerView extends RecyclerView {
    private ScrollingCallBack mScrollingCallBack;
    public ScrollingRecyclerView(Context context) {
        super(context);
    }

    public ScrollingRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollingRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setScrollingCallBack(ScrollingCallBack scrollingCallBack) {
        this.mScrollingCallBack = scrollingCallBack;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        //根据newState状态做处理
        switch (state) {
            case 0:
                if(null != mScrollingCallBack) {
                    mScrollingCallBack.stopScrolling();
                }
                break;

            case 1:
                if(null != mScrollingCallBack) {
                    mScrollingCallBack.scrolling();
                }
                break;

            case 2:
                if(null != mScrollingCallBack) {
                    mScrollingCallBack.scrolling();
                }
                break;
        }
    }

    public interface ScrollingCallBack {
        void stopScrolling();
        void scrolling();
    }

}
