package com.lxf.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;




/**
 * @Description:Glide图片加载
 * @Author Yanggm
 * @Date 16/3/23 17:43
 */
public class ImageLoaderGlideUtil {
    private static GlideBuilder glideBuilder;

    private static void checkBuilder(Context context) {
        if (glideBuilder == null) {
            glideBuilder = new GlideBuilder(context);
        }
    }

    /**
     * 设置在内置存储路径缓存目录文件
     *
     * @param dir     路径
     * @param size    大小
     * @param context context
     */
    private static void setInternalCacheDiskCache(Context context, String dir, int size) {
        checkBuilder(context);
        glideBuilder.setDiskCache(new InternalCacheDiskCacheFactory(context, dir, size));
    }



    /**
     * 加载图片
     *
     * @param imageView       imageView
     * @param url             url
     * @param defaultDrawable 默认Drawable
     */
    public static void displayImage(ImageView imageView, String url, Drawable defaultDrawable) {
        if (imageView == null)
            return;

        Glide.with(VideoApplication.getContext()).load(url).placeholder(defaultDrawable).error(defaultDrawable).into(imageView);
    }


    /**
     * 加载图片
     *
     * @param imageView         imageView
     * @param url               url
     * @param defaultDrawableId 默认资源图片
     */
    public static void displayImage(ImageView imageView, String url, @DrawableRes int defaultDrawableId) {

        displayImage(imageView, url, VideoApplication.getContext().getResources().getDrawable(defaultDrawableId));
    }

    /**
     * 加载Uri图片
     *
     * @param imageView         imageView
     * @param uri               uri
     * @param defaultDrawableId 默认资源图片
     */
    public static void displayImage(ImageView imageView, Uri uri, @DrawableRes int defaultDrawableId) {

        displayImage(imageView, uri, VideoApplication.getContext().getResources().getDrawable(defaultDrawableId));
    }

    /**
     * 加载Uri图片
     *
     * @param imageView       imageView
     * @param uri             uri
     * @param defaultDrawable 默认资源图片
     */
    public static void displayImage(ImageView imageView, Uri uri, Drawable defaultDrawable) {
        if (imageView == null)
            return;

        Glide.with(VideoApplication.getContext()).load(uri).placeholder(defaultDrawable).error(defaultDrawable).into(imageView);
    }

    /**
     * 加载Gif图片
     *
     * @param imageView         imageview
     * @param url               url
     * @param defaultDrawableId 默认资源图片
     */
    public static void displayGifImage(final ImageView imageView, String url, @DrawableRes int defaultDrawableId) {
        if (imageView == null)
            return;

        //diskCacheStrategy 设置缓存策略，解决glide加载gif速度超慢的问题 http://blog.csdn.net/u010316858/article/details/49665107
        Glide.with(VideoApplication.getContext()).load(url).asGif().diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .placeholder(defaultDrawableId).error(defaultDrawableId).fitCenter().into(imageView);
    }

    /**
     * 加载Gif图片 没有默认图片
     *
     * @param imageView imageview
     * @param url       url
     */
    public static void displayGifImage(final ImageView imageView, String url) {
        displayGifImage(imageView, url, 0);
    }

    /**
     * 加载Gif图片
     *
     * @param imageView imageView
     * @param gifId     gif资源ID
     */
    public static void displayGifImage(final ImageView imageView, int gifId) {
        if (imageView == null)
            return;

        //diskCacheStrategy 设置缓存策略，解决glide加载gif速度超慢的问题 http://blog.csdn.net/u010316858/article/details/49665107
        Glide.with(VideoApplication.getContext()).load(gifId).diskCacheStrategy(DiskCacheStrategy.SOURCE).fitCenter().into(imageView);
    }

    /**
     * 加载本地图片
     *
     * @param imageView
     * @param drawableRes
     */
    public static void diplayImage(@NonNull final ImageView imageView, @DrawableRes int drawableRes) {

        Glide.with(VideoApplication.getContext()).load(drawableRes).into(imageView);
    }

    ///**
    // * 显示高斯模糊图片 需要将ImageView的ScaleType设为FIT_XY(最好在xml里设置，以免在RecylerView滑动时显示不正常)
    // *
    // * @param imageView         imageView
    // * @param url               url
    // * @param defaultDrawableId 默认资源图片
    // */
    //public static void displayBlurImage(final ImageView imageView, String url, @DrawableRes int defaultDrawableId) {
    //    Glide.with(VideoApplication.getContext()).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(defaultDrawableId).error(defaultDrawableId)
    //            .bitmapTransform(new BlurTransformation(VideoApplication.getContext(), 25, 5)).into(imageView);
    //}

    /**
     * 通过传入的context，isPause为true暂停当前图片的加载，false继续加载图片
     *
     * @param context
     * @param isPause
     */
    public static void pauseOrResumeRequest(Context context, boolean isPause) {
        if (isPause) {
            Glide.with(context).pauseRequestsRecursive();
        } else {
            Glide.with(context).resumeRequestsRecursive();
        }
    }


    /**
     * 加载图片
     */
    public static void displayImage(ImageView imageView, String url, @DrawableRes int defaultDrawableId, @NonNull final ImageLoaderListener<Bitmap> listener) {
        if (imageView == null)
            return;

        Drawable defaultDrawable = VideoApplication.getContext().getResources().getDrawable(defaultDrawableId);

        Glide.with(VideoApplication.getContext()).load(url).asBitmap().placeholder(defaultDrawable).error(defaultDrawable).listener(new RequestListener<String, Bitmap>() {
            @Override
            public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                return listener.loadException(e);
            }

            @Override
            public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                return listener.loadCompeted(resource);
            }

        }).into(imageView);
    }

    public static void getBitMap(String url, Target<Bitmap> target) {
        Glide.with(VideoApplication.getContext()).load(url).asBitmap().into(target);
    }

    /**
     * 停止加载
     */
    public static void stopLoad(View view) {
        if (null != view) {
            Glide.clear(view);
        }
    }

    /**
     * @param <T> 加载图片的类型
     */
    public interface ImageLoaderListener<T> {
        /**
         * 加载成功时执行的操作
         *
         * @return 返回false表示继续加载图片<br/>返回true消耗加载事件，不显示图片
         */
        boolean loadCompeted(T resource);

        /**
         * 加载异常时执行的操作
         *
         * @return 返回false表示继续加载图片<br/>返回true消耗加载事件，不显示图片
         */
        boolean loadException(Exception e);
    }

    /**
     * 清空内存缓存
     * 需要在主线程使用
     */
    public static void clearMemoryCache() {
        Glide.get(VideoApplication.getContext()).clearMemory();
    }

    /**
     * 减少内存缓存
     * 需要在主线程使用
     */
    public static void trimMemoryCache(int level) {
        Glide.get(VideoApplication.getContext()).trimMemory(level);
    }


}
