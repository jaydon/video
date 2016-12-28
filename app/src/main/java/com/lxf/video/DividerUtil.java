package com.lxf.video;

import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;

import com.lxf.video.view.DividerItemDecoration;


/**
 *  用于修改divider的分隔线
 * 作者：luoxf on 2016/10/25 14:09
 */

public class DividerUtil {
    /**
     * 用于有左右间距时的分隔线
     * @param context Context
     * @return Paint
     */
    public static Paint getDividerPaint(@NonNull Context context) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(DisplayUtil.dp2px(context, 1));
        paint.setColor(ContextCompat.getColor(context, R.color.divider));
        paint.setAntiAlias(true);
        return paint;
    }

    /**
     * 取得公共的横向分割线，没有间距
     * @param context Context
     * @return DividerItemDecoration
     */
    public static DividerItemDecoration getDividerItemDecoration(@NonNull Context context) {
        return new DividerItemDecoration(
                context, LinearLayoutManager.VERTICAL, R.color.divider, 2);
    }
}
