package com.lxf.video.adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.lxf.video.R;
import com.lxf.video.view.ScrollingRecyclerView;

import java.util.ArrayList;
import java.util.List;


import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * RecyclerViewAdpter基类
 * 添加了headView, footView ,emptyView ,要特别注意emptyview ： 看headView 、footview是否需要显示，所以有多种情况
 *
 * @author luoxf on 2016/8/3 10:14
 */
public abstract class BaseRecyclerViewAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ScrollingRecyclerView.ScrollingCallBack{
    /**
     * 是否在滑动中
     */
    protected boolean isScrolling;
    protected Context mContext;
    protected LayoutInflater mLayoutInflater;
    /**
     * 数据
     */
    protected List<T> mData = new ArrayList<>();
    /**
     * item view
     */
    protected int mLayoutResId;
    /**
     * item 里的view 点击回调
     */
    private OnRecyclerViewItemChildClickListener mChildClickListener;
    /**
     * item 里的view 长点击回调
     */
    private OnRecyclerViewItemChildLongClickListener mChildLongClickListener;
    /**
     * item点击回调
     */
    private OnRecyclerViewItemClickListener onRecyclerViewItemClickListener;
    /**
     * item长点击回调
     */
    private OnRecyclerViewItemLongClickListener onRecyclerViewItemLongClickListener;

    private boolean mNextLoadEnable = false;
    /**
     * 用于控制上次加载是否加载完
     */
    private boolean mLoadingMoreEnable = false;
    /**
     * 是否显示空view
     */
    private boolean mEmptyEnable;
    /**
     * 是否在数据为空时，显示头部
     */
    private boolean mHeadAndEmptyEnable;
    /**
     * 是否在数据为空时，显示底部
     */
    private boolean mFootAndEmptyEnable;
    private RequestLoadMoreListener mRequestLoadMoreListener;
    private LinearLayout mHeaderLayout;
    private LinearLayout mFooterLayout;
    private LinearLayout mCopyHeaderLayout = null;
    private LinearLayout mCopyFooterLayout = null;
    private int pageSize = -1;
    private View mEmptyView;
    public static final int HEADER_VIEW = 0x00000111;
    public static final int LOADING_VIEW = 0x00000222;
    public static final int FOOTER_VIEW = 0x00000333;
    public static final int EMPTY_VIEW = 0x00000555;
    private View mLoadingView;

    public BaseRecyclerViewAdapter(Context context, int layoutResId) {
        this.mContext = context;
        this.mLayoutResId = layoutResId;
    }

    /**
     * 移掉某个数据
     *
     * @param position 位置
     */
    public void remove(int position) {
        mData.remove(position);
        notifyItemRemoved(position + getHeaderLayoutCount());
        notifyDataSetChanged();
    }

    /**
     * 在某处插入数据
     *
     * @param position 位置
     * @param item     数据
     */
    public void insertAt(int position, T item) {
        mData.add(position, item);
        notifyItemInserted(position);
    }

    /**
     * 删除某个item,并刷新
     * @param item
     */
    public void remove(T item) {
        if(null == item) {
            return;
        }
        mData.remove(item);
        notifyDataSetChanged();
    }

    /**
     * 更换数据
     *
     * @param data 数据
     */
    public void changeData(List<T> data) {
        if (null == data) {
            data = new ArrayList<>();
        }
        this.mData = data;
        notifyDataSetChanged();
    }

    /**
     * 清空数据
     */
    public void clearData() {
        if (null != mData) {
            mData.clear();
            notifyDataSetChanged();
        }
    }

    /**
     * 添加新数据
     *
     * @param data 数据
     */
    public void addData(List<T> data) {
        this.mData.addAll(data);
        notifyDataSetChanged();
    }

    /**
     * 取得数据
     *
     * @return 数据
     */
    public List<T> getData() {
        return mData;
    }

    /**
     * 取得某个位置的数据
     *
     * @param position 位置
     * @return 数据
     */
    public T getItem(int position) {
        return mData.get(position);
    }

    /**
     * 取得item view
     *
     * @param layoutResId item layout
     * @param parent      父布局
     * @return
     */
    protected View getItemView(int layoutResId, ViewGroup parent) {
        return mLayoutInflater.inflate(layoutResId, parent, false);
    }

    public void setOnLoadMoreListener(RequestLoadMoreListener requestLoadMoreListener) {
        this.mRequestLoadMoreListener = requestLoadMoreListener;
    }

    public void openLoadMore(int pageSize, boolean enable) {
        this.pageSize = pageSize;
        mNextLoadEnable = enable;
    }

    public void openLoadMore(boolean enable) {
        mNextLoadEnable = enable;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    /**
     * 设置item点击回调
     *
     * @param onRecyclerViewItemClickListener
     */
    public void setOnRecyclerViewItemClickListener(OnRecyclerViewItemClickListener onRecyclerViewItemClickListener) {
        this.onRecyclerViewItemClickListener = onRecyclerViewItemClickListener;
    }

    /**
     * Item点击接口
     */
    public interface OnRecyclerViewItemClickListener {
        public void onItemClick(View view, int position);
    }

    /**
     * 设置item长点击回调
     *
     * @param onRecyclerViewItemLongClickListener
     */
    public void setOnRecyclerViewItemLongClickListener(OnRecyclerViewItemLongClickListener onRecyclerViewItemLongClickListener) {
        this.onRecyclerViewItemLongClickListener = onRecyclerViewItemLongClickListener;
    }

    /**
     * item长点击接口
     */
    public interface OnRecyclerViewItemLongClickListener {
        public boolean onItemLongClick(View view, int position);
    }

    /**
     * 设置item里的view点击回调
     *
     * @param childClickListener
     */
    public void setOnRecyclerViewItemChildClickListener(OnRecyclerViewItemChildClickListener childClickListener) {
        this.mChildClickListener = childClickListener;
    }

    public interface OnRecyclerViewItemChildClickListener {
        void onItemChildClick(BaseRecyclerViewAdapter adapter, View view, int position);
    }

    public class OnItemChildClickListener implements View.OnClickListener {
        public RecyclerView.ViewHolder mViewHolder;

        @Override
        public void onClick(View v) {
            if (mChildClickListener != null)
                mChildClickListener.onItemChildClick(BaseRecyclerViewAdapter.this, v, mViewHolder.getLayoutPosition() - getHeaderLayoutCount());
        }
    }

    /**
     * 设置item里的view点长击回调
     *
     * @param childLongClickListener
     */
    public void setOnRecyclerViewItemChildLongClickListener(OnRecyclerViewItemChildLongClickListener childLongClickListener) {
        this.mChildLongClickListener = childLongClickListener;
    }

    public interface OnRecyclerViewItemChildLongClickListener {
        boolean onItemChildLongClick(BaseRecyclerViewAdapter adapter, View view, int position);
    }

    public class OnItemChildLongClickListener implements View.OnLongClickListener {
        public RecyclerView.ViewHolder mViewHolder;

        @Override
        public boolean onLongClick(View v) {
            if (mChildLongClickListener != null) {
                return mChildLongClickListener.onItemChildLongClick(BaseRecyclerViewAdapter.this, v, mViewHolder.getLayoutPosition() - getHeaderLayoutCount());
            }
            return false;
        }
    }

    public void setLoadingView(View loadingView) {
        this.mLoadingView = loadingView;
    }

    public int getHeaderLayoutCount() {
        return mHeaderLayout == null ? 0 : 1;
    }

    public int getFooterLayoutCount() {
        return mFooterLayout == null ? 0 : 1;
    }

    public int getmEmptyViewCount() {
        return mEmptyView == null ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        int i = isLoadMore() ? 1 : 0;
        int count = mData.size() + i + getHeaderLayoutCount() + getFooterLayoutCount();
        if (mData.size() == 0 && mEmptyView != null) {
            if (count == 0 && (!mHeadAndEmptyEnable || !mFootAndEmptyEnable)) {
                count += getmEmptyViewCount();
            } else if (mHeadAndEmptyEnable || mFootAndEmptyEnable) {
                count += getmEmptyViewCount();
            }

            if ((mHeadAndEmptyEnable && getHeaderLayoutCount() == 1 && count == 1) || count == 0) {
                mEmptyEnable = true;
                count += getmEmptyViewCount();
            }

        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (mHeaderLayout != null && position == 0) {
            return HEADER_VIEW;
        }
        if (mData.size() == 0 && mEmptyEnable && mEmptyView != null && position <= 2) {
            if ((mHeadAndEmptyEnable || mFootAndEmptyEnable) && position == 1) {
                if (mHeaderLayout == null && mFooterLayout != null) {
                    return FOOTER_VIEW;
                } else if (mHeaderLayout != null) {
                    return EMPTY_VIEW;
                }
            } else if (position == 0) {
                if (mHeaderLayout == null) {
                    return EMPTY_VIEW;
                } else if (mFooterLayout != null) {
                    return EMPTY_VIEW;
                }
            } else if (position == 2 && (mFootAndEmptyEnable || mHeadAndEmptyEnable) && mHeaderLayout != null) {
                return FOOTER_VIEW;

            } else if ((!mFootAndEmptyEnable || !mHeadAndEmptyEnable) && position == 1 && mFooterLayout != null) {
                return FOOTER_VIEW;
            }
        } else if (mData.size() == 0 && mEmptyView != null && getItemCount() == (mHeadAndEmptyEnable ? 2 : 1) && mEmptyEnable) {
            return EMPTY_VIEW;
        } else if (position == mData.size() + getHeaderLayoutCount()) {
            if (mNextLoadEnable)
                return LOADING_VIEW;
            else
                return FOOTER_VIEW;
        } else if (position > mData.size() + getHeaderLayoutCount()) {
            return FOOTER_VIEW;
        }
        return getDefItemViewType(position - getHeaderLayoutCount());
    }

    protected int getDefItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder baseViewHolder;
        this.mLayoutInflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case LOADING_VIEW:
                baseViewHolder = getLoadingView(parent);
                break;
            case HEADER_VIEW:
                baseViewHolder = new BaseViewHolder(mHeaderLayout);
                break;
            case EMPTY_VIEW:
                baseViewHolder = new BaseViewHolder(mEmptyView);
                break;
            case FOOTER_VIEW:
                baseViewHolder = new BaseViewHolder(mFooterLayout);
                break;
            default:
                baseViewHolder = onCreateDefViewHolder(parent, viewType);
                initItemClickListener(baseViewHolder);
        }
        return baseViewHolder;

    }


    private BaseViewHolder getLoadingView(ViewGroup parent) {
        if (mLoadingView == null) {
            return createBaseViewHolder(parent, R.layout.include_loading);
        }
        return new BaseViewHolder(mLoadingView);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int type = holder.getItemViewType();
        if (type == EMPTY_VIEW || type == HEADER_VIEW || type == FOOTER_VIEW || type == LOADING_VIEW) {
            setFullSpan(holder);
        }
    }

    /**
     * 瀑布流
     *
     * @param holder
     */
    protected void setFullSpan(RecyclerView.ViewHolder holder) {
        if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            params.setFullSpan(true);
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            final GridLayoutManager gridManager = ((GridLayoutManager) manager);
            gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int type = getItemViewType(position);
                    return (type == EMPTY_VIEW || type == HEADER_VIEW || type == FOOTER_VIEW || type == LOADING_VIEW) ? gridManager.getSpanCount() : 1;
                }
            });
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int positions) {
        int viewType = holder.getItemViewType();

        switch (viewType) {
            case LOADING_VIEW:
                BaseViewHolder holderLoading = (BaseViewHolder) holder;
                final ImageView iv = holderLoading.getView(R.id.ivLoading);
                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
                        iv.getContext(), R.anim.loading_animation);
                iv.startAnimation(hyperspaceJumpAnimation);
                addLoadMore(holder);
                break;
            case HEADER_VIEW:
                break;
            case EMPTY_VIEW:
                break;
            case FOOTER_VIEW:
                break;
            default:
                convert((BaseViewHolder) holder, mData.get(holder.getLayoutPosition() - getHeaderLayoutCount()), positions, viewType);
                break;
        }

    }


    /**
     * 拓展多个type
     *
     * @param parent   父view
     * @param viewType 类型
     * @return
     */
    protected BaseViewHolder onCreateDefViewHolder(ViewGroup parent, int viewType) {
        return createBaseViewHolder(parent, mLayoutResId);
    }

    protected BaseViewHolder createBaseViewHolder(ViewGroup parent, int layoutResId) {
        return new BaseViewHolder(getItemView(layoutResId, parent));
    }

    public LinearLayout getHeaderLayout() {
        return mHeaderLayout;
    }

    public LinearLayout getFooterLayout() {
        return mFooterLayout;
    }

    public void addHeaderView(View header) {
        addHeaderView(header, -1);
    }

    public void addHeaderView(View header, int index) {
        if (mHeaderLayout == null) {
            if (mCopyHeaderLayout == null) {
                mHeaderLayout = new LinearLayout(header.getContext());
                mHeaderLayout.setOrientation(LinearLayout.VERTICAL);
                mHeaderLayout.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
                mCopyHeaderLayout = mHeaderLayout;
            } else {
                mHeaderLayout = mCopyHeaderLayout;
            }
        }
        index = index >= mHeaderLayout.getChildCount() ? -1 : index;
        mHeaderLayout.addView(header, index);
        this.notifyDataSetChanged();
    }

    public void addFooterView(View footer) {
        addFooterView(footer, -1);
    }

    public void addFooterView(View footer, int index) {
        mNextLoadEnable = false;
        if (mFooterLayout == null) {
            if (mCopyFooterLayout == null) {
                mFooterLayout = new LinearLayout(footer.getContext());
                mFooterLayout.setOrientation(LinearLayout.VERTICAL);
                mFooterLayout.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
                mCopyFooterLayout = mFooterLayout;
            } else {
                mFooterLayout = mCopyFooterLayout;
            }
        }
        index = index >= mFooterLayout.getChildCount() ? -1 : index;
        mFooterLayout.addView(footer, index);
        this.notifyDataSetChanged();
    }

    public void removeHeaderView(View header) {
        if (mHeaderLayout == null) return;

        mHeaderLayout.removeView(header);
        if (mHeaderLayout.getChildCount() == 0) {
            mHeaderLayout = null;
        }
        this.notifyDataSetChanged();
    }

    public void removeFooterView(View footer) {
        if (mFooterLayout == null) return;

        mFooterLayout.removeView(footer);
        if (mFooterLayout.getChildCount() == 0) {
            mFooterLayout = null;
        }
        this.notifyDataSetChanged();
    }

    public void removeAllHeaderView() {
        if (mHeaderLayout == null) return;

        mHeaderLayout.removeAllViews();
        mHeaderLayout = null;
    }

    public void removeAllFooterView() {
        if (mFooterLayout == null) return;

        mFooterLayout.removeAllViews();
        mFooterLayout = null;
    }

    public void setEmptyView(View emptyView) {
        setEmptyView(false, false, emptyView);
    }

    public void setEmptyView(boolean isHeadAndEmpty, View emptyView) {
        setEmptyView(isHeadAndEmpty, false, emptyView);
    }

    /**
     * @param isHeadAndEmpty false 当数据为空时，没有显示headViwe；true 当数据为空时，会显示 headView
     * @param isFootAndEmpty false 当数据为空时，没有显示footViwe；true 当数据为空时，会显示 footViwe
     * @param emptyView      空数据时显示的view
     */
    public void setEmptyView(boolean isHeadAndEmpty, boolean isFootAndEmpty, View emptyView) {
        mHeadAndEmptyEnable = isHeadAndEmpty;
        mFootAndEmptyEnable = isFootAndEmpty;
        mEmptyView = emptyView;
        mEmptyEnable = true;
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    /**
     * 停止加载更多，
     *
     * @param isNextLoad true， 仍然显示 loading view；false 不显示loadingview
     */
    public void notifyDataChangedAfterLoadMore(boolean isNextLoad) {
        mNextLoadEnable = isNextLoad;
        mLoadingMoreEnable = false;
        notifyDataSetChanged();

    }

    public void notifyDataChangedAfterLoadMore(List<T> data, boolean isNextLoad) {
        mData.addAll(data);
        notifyDataChangedAfterLoadMore(isNextLoad);

    }


    private void addLoadMore(RecyclerView.ViewHolder holder) {
        if (isLoadMore() && !mLoadingMoreEnable) {
            mLoadingMoreEnable = true;
            mRequestLoadMoreListener.onLoadMoreRequested();
        }
    }

    private void initItemClickListener(final BaseViewHolder baseViewHolder) {
        if (onRecyclerViewItemClickListener != null) {
            baseViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRecyclerViewItemClickListener.onItemClick(v, baseViewHolder.getLayoutPosition() - getHeaderLayoutCount());
                }
            });
        }
        if (onRecyclerViewItemLongClickListener != null) {
            baseViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return onRecyclerViewItemLongClickListener.onItemLongClick(v, baseViewHolder.getLayoutPosition() - getHeaderLayoutCount());
                }
            });
        }
    }


    private boolean isLoadMore() {
        return mNextLoadEnable && pageSize != -1 && mRequestLoadMoreListener != null && mData.size() >= pageSize;
    }

    public interface RequestLoadMoreListener {

        void onLoadMoreRequested();
    }

    protected abstract void convert(BaseViewHolder helper, T item, int position, int viewType);

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void stopScrolling() {
        isScrolling = false;
        notifyDataSetChanged();
    }

    public void scrolling() {
        isScrolling = true;
    }

}
