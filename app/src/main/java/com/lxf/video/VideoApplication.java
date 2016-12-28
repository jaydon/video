package com.lxf.video;

import android.app.Application;
import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;

/**
 * User: luoxf
 * Date: 2016-12-20
 * Time: 17:55
 */
public class VideoApplication extends Application {
    private static Context mContext;
    private HttpProxyCacheServer proxy;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        proxy = newProxy();

    }

    public static Context getContext() {
        return mContext;
    }

    public static HttpProxyCacheServer getProxy(Context context) {
        VideoApplication app = (VideoApplication) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer(this);
    }
}
