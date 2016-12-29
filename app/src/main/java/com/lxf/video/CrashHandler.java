package com.lxf.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 异常捕捉
 * 作者：luoxf on 2016/5/3 16:15
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Context mContext;
    private Thread.UncaughtExceptionHandler handler;
    // 到SD卡目录
    private final static String DISK_CACHE_PATH = "/EHEALTH/";
    private static CrashHandler crashHandler;

    public static CrashHandler init(Context context) {
        if (crashHandler == null) {
            crashHandler = new CrashHandler(context);
        }
        return crashHandler;
    }
    public CrashHandler(Context context) {
        this.mContext = context;
        handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        SimpleDateFormat simpledateformat = new SimpleDateFormat(
                "dd-MM-yyyy hh:mm:ss");
        // 日期、app版本信息
        StringBuilder buff = new StringBuilder();
        buff.append("Date: ").append(simpledateformat.format(new Date()))
                .append("\n");
        buff.append("========MODEL:" + Build.MODEL + " \n");
        // 崩溃的堆栈信息
        buff.append("Stacktrace:\n\n");
        StringWriter stringwriter = new StringWriter();
        PrintWriter printwriter = new PrintWriter(stringwriter);
        throwable.printStackTrace(printwriter);
        buff.append(stringwriter.toString());
        buff.append("===========\n");
        printwriter.close();

        write2ErrorLog(buff.toString());

        if (handler != null) {
            // 交给还给系统处理
            handler.uncaughtException(thread, throwable);
        }
    }

    /**
     * 创建总文件夹
     */
    public String getFilePath() {
        String cachePath;
        cachePath = Environment.getExternalStorageDirectory().getPath() + DISK_CACHE_PATH;
        File file = new File(cachePath);
        if (!file.exists()) {
            // 创建文件夹
            file.mkdirs();
        }
        return cachePath;
    }

    private void write2ErrorLog(String content) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return;
        }
        if (BuildConfig.DEBUG) {
            File file = new File(getFilePath() + "/crash.txt");
            BufferedWriter bw = null;
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                bw = new BufferedWriter(new FileWriter(file, true));
                bw.append(content);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bw != null) {
                        bw.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            //线上错误提交到友盟
        }
    }
}
