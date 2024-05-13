/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2019-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date                  Author	                 Action
 * 20190108  	         Steven.W                Create
 * ===========================================================================================
 */
package com.evp.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.evp.commonlib.utils.LogUtils;
import com.evp.commonlib.utils.ObjectPoolHelper;
import com.evp.payment.evpscb.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Electronic signature view.
 */
public class ElectronicSignatureView extends View {

    /**
     * 笔画X坐标起点
     */
    private float mX;
    /**
     * 笔画Y坐标起点
     */
    private float mY;
    /**
     * 手写画笔
     */
    private final Paint mGesturePaint = new Paint();

    private Paint paint;

    private Rect bounds;
    /**
     * 路径
     */
    private final Path mPath = new Path();
    /**
     * 背景画布
     */
    private Canvas cacheCanvas;
    /**
     * 背景Bitmap缓存
     */
    private Bitmap cachebBitmap;
    /**
     * 是否已经签名
     */
    private boolean isTouched = false;

    /**
     * 画笔宽度 px；
     */
    private int mPaintWidth;

    /**
     * 前景色
     */
    private int mPenColor = Color.BLACK;

    private int mBackColor = Color.TRANSPARENT;

    private int textColor;
    private String text;
    private Rect rect;
    private int padding;
    private int background;
    private boolean isFirst = true;

    private List<float[]> mPathPos = new ArrayList<>();
    private int sampleRate = 3;

    private PathMeasure pathMeasure;
    private int measuredWidth;
    private int measuredHeight;

    /**
     * Instantiates a new Electronic signature view.
     *
     * @param context the context
     */
    public ElectronicSignatureView(Context context) {
        super(context);
        init(context);
    }

    /**
     * Instantiates a new Electronic signature view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public ElectronicSignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Instantiates a new Electronic signature view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public ElectronicSignatureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Init.
     *
     * @param context the context
     */
    public void init(Context context) {
        mPaintWidth = (int) context.getResources().getDimension(R.dimen.paint_width);
        mGesturePaint.setAntiAlias(true);
        mGesturePaint.setStyle(Style.STROKE);
        mGesturePaint.setStrokeWidth(mPaintWidth);
        mGesturePaint.setColor(mPenColor);
        mGesturePaint.setStrokeJoin(Paint.Join.ROUND);
        mGesturePaint.setStrokeCap(Paint.Cap.ROUND);

        this.text = "";
        this.textColor = Color.BLACK;
        this.rect = new Rect(0, 0, 500, 200);
        this.padding = 0;
        this.background = Color.WHITE;

        paint = new Paint();
        paint.setColor(textColor);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        bounds = new Rect();
        pathMeasure = new PathMeasure(mPath, false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cachebBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
        cacheCanvas = new Canvas(cachebBitmap);
        cacheCanvas.drawColor(mBackColor);
        isTouched = false;
        measuredWidth = getMeasuredWidth();
        measuredHeight = getMeasuredHeight();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                isTouched = true;
                touchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                touchUp(event);
                break;
            default:
                break;
        }
        // 更新绘制
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(cachebBitmap, 0, 0, mGesturePaint);
        // 通过画布绘制多点形成的图形
        canvas.drawPath(mPath, mGesturePaint);

        if (isFirst) {
            paint.getTextBounds(text, 0, text.length(), bounds);

            cacheCanvas.drawText(text, measuredWidth / 2 - bounds.width() / 2,
                    measuredHeight / 2 + bounds.height() / 2, paint);
            isFirst = false;
        }
    }

    /**
     * Sets text.
     *
     * @param textColor the text color
     * @param text      the text
     */
    public void setText(int textColor, String text) {
        this.textColor = textColor;
        this.text = text;
    }

    /**
     * Sets bitmap.
     *
     * @param rect       the rect
     * @param padding    the padding
     * @param background the background
     */
    public void setBitmap(Rect rect, int padding, int background) {
        this.background = background;
        this.rect = rect;
        this.padding = padding;
    }

    // 手指点下屏幕时调用
    private void touchDown(MotionEvent event) {
        // 重置绘制路线，即隐藏之前绘制的轨迹
        mPath.reset();
        float x = event.getX();
        float y = event.getY();

        mX = x;
        mY = y;
        // mPath绘制的绘制起点
        mPath.moveTo(x, y);
    }

    // 手指在屏幕上滑动时调用
    private void touchMove(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        final float previousX = mX;
        final float previousY = mY;

        final float dx = Math.abs(x - previousX);
        final float dy = Math.abs(y - previousY);

        // 两点之间的距离大于等于3时，生成贝塞尔绘制曲线
        if (dx >= 3 || dy >= 3) {
            // 设置贝塞尔曲线的操作点为起点和终点的一半
            float cX = (x + previousX) / 2;
            float cY = (y + previousY) / 2;

            // 二次贝塞尔，实现平滑曲线；previousX, previousY为操作点，cX, cY为终点
            mPath.quadTo(previousX, previousY, cX, cY);

            // 第二次执行时，第一次结束调用的坐标值将作为第二次调用的初始坐标值
            mX = x;
            mY = y;
        }
    }

    // 手指离开屏幕时调用
    private void touchUp(MotionEvent event) {
        cacheCanvas.drawPath(mPath, mGesturePaint);

        pathMeasure.setPath(mPath,false);
        for (int i = 0; i < pathMeasure.getLength(); i += sampleRate) {
            float[] pos = ObjectPoolHelper.obtainFloatArray(2);
            pathMeasure.getPosTan(i, pos, null);
            mPathPos.add(pos);
            ObjectPoolHelper.releaseFloatArray(pos);
        }
        mPathPos.add(new float[]{-1, -1}); //end flag
        mPath.reset();
    }

    /**
     * 清除画板
     */
    public void clear() {
        isFirst = true;
        if (cacheCanvas != null) {
            isTouched = false;
            mGesturePaint.setColor(mPenColor);
            cacheCanvas.drawColor(mBackColor, PorterDuff.Mode.CLEAR);
            mGesturePaint.setColor(mPenColor);
            invalidate();
        }
    }

    /**
     * 保存画板
     *
     * @param path 保存到路径
     * @throws IOException the io exception
     */
    public void save(String path) throws IOException {
        save(path, false, 0);
    }

    /**
     * 保存bitmap
     *
     * @param clearBlank to clear blank
     * @param blank      board size
     * @return bitmap object
     */
    public Bitmap save(boolean clearBlank, int blank) {
        Bitmap bitmap = cachebBitmap;
        if (clearBlank) {
            bitmap = clearBlank(bitmap, blank, mBackColor);
        }
        bitmap = placeBitmapIntoRect(bitmap, rect, padding, background);

        return bitmap;
    }

    /**
     * 保存画板
     *
     * @param path       保存到路径
     * @param clearBlank 是否清楚空白区域
     * @param blank      边缘空白区域
     * @throws IOException the io exception
     */
    public void save(String path, boolean clearBlank, int blank) throws IOException {

        Bitmap bitmap = cachebBitmap;
        // BitmapUtil.createScaledBitmapByHeight(srcBitmap, 300);// 压缩图片
        if (clearBlank) {
            bitmap = clearBlank(bitmap, blank, mBackColor);
        }
        bitmap = placeBitmapIntoRect(bitmap, rect, padding, background);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] buffer = bos.toByteArray();
        if (buffer != null) {
            File file = new File(path);
            if (!file.delete()) {
                LogUtils.w("E-Sign", file.toString() + "is not existed");
            }

            try (OutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(buffer);
            } catch (Exception e) {
                LogUtils.w("E-Sign", e);
            }
        }
    }

    /**
     * 获取画板的bitmap
     *
     * @return the bitmap
     */
    public Bitmap getBitMap() {
        setDrawingCacheEnabled(true);
        buildDrawingCache();
        Bitmap bitmap = getDrawingCache();
        setDrawingCacheEnabled(false);
        return bitmap;
    }

    private static int getTopBorder(Bitmap bp, int width, int height, int blank, int backColor) {
        int[] pixels = new int[width];
        for (int y = 0; y < height; y++) {
            bp.getPixels(pixels, 0, width, 0, y, width, 1);
            for (int pix : pixels) {
                if (pix != backColor) {
                    return y - blank > 0 ? y - blank : 0;
                }
            }
        }
        return 0;
    }

    private static int getBottomBorder(Bitmap bp, int width, int height, int blank, int backColor) {
        int[] pixels = new int[width];
        for (int y = height - 1; y >= 0; y--) {
            bp.getPixels(pixels, 0, width, 0, y, width, 1);
            for (int pix : pixels) {
                if (pix != backColor) {
                    return y + blank > height - 1 ? height - 1 : y + blank;
                }
            }
        }
        return height - 1;
    }

    private static int getLeftBorder(Bitmap bp, int width, int height, int blank, int backColor) {
        int[] pixels = new int[height];
        for (int x = 0; x < width; x++) {
            bp.getPixels(pixels, 0, 1, x, 0, 1, height);
            for (int pix : pixels) {
                if (pix != backColor) {
                    return x - blank > 0 ? x - blank : 0;
                }
            }
        }
        return 0;
    }

    private static int getRightBorder(Bitmap bp, int width, int height, int blank, int backColor) {
        int[] pixels = new int[height];
        for (int x = width - 1; x > 0; x--) {
            bp.getPixels(pixels, 0, 1, x, 0, 1, height);
            for (int pix : pixels) {
                if (pix != backColor) {
                    return x + blank > width - 1 ? width - 1 : x + blank;
                }
            }
        }
        return width - 1;
    }

    /**
     * 逐行扫描 清除边界空白。
     *
     * @param bp    the bitmap
     * @param blank 边距留多少个像素
     * @return formatted bitmap
     */
    private static Bitmap clearBlank(Bitmap bp, int blank, int backColor) {
        int height = bp.getHeight();
        int width = bp.getWidth();

        int newBlank = blank < 0 ? 0 : blank;

        int top = getTopBorder(bp, width, height, newBlank, backColor);
        int bottom = getBottomBorder(bp, width, height, newBlank, backColor);
        int left = getLeftBorder(bp, width, height, newBlank, backColor);
        int right = getRightBorder(bp, width, height, newBlank, backColor);

        return Bitmap.createBitmap(bp, left, top, right - left, bottom - top);
    }

    /**
     * 设置画笔宽度 默认宽度为10px
     *
     * @param paintWidth paint width
     */
    public void setPaintWidth(int paintWidth) {
        this.mPaintWidth = paintWidth > 0 ? paintWidth : 5;
        mGesturePaint.setStrokeWidth(paintWidth);
    }

    /**
     * Sets back color.
     *
     * @param backColor the back color
     */
    public void setBackColor(int backColor) {
        mBackColor = backColor;
    }

    /**
     * 设置画笔颜色
     *
     * @param penColor pen color
     */
    public void setPenColor(int penColor) {
        this.mPenColor = penColor;
        mGesturePaint.setColor(penColor);
    }

    /**
     * 是否有签名
     *
     * @return has signature or not
     */
    public boolean getTouched() {
        return isTouched;
    }

    /**
     * Place bitmap into rect bitmap.
     *
     * @param bitmap     the bitmap
     * @param rect       the rect
     * @param padding    the padding
     * @param background the background
     * @return the bitmap
     */
    public static Bitmap placeBitmapIntoRect(final Bitmap bitmap, final Rect rect, int padding, int background) {
        int newPadding = (padding * 2 >= rect.height() || padding * 2 >= rect.width() || padding < 0) ? 0 : padding;

        int height = rect.height() - newPadding * 2;
        int width = rect.width() - newPadding * 2;
        Matrix matrix = new Matrix();
        float h = (float) height / bitmap.getHeight();
        float w = (float) width / bitmap.getWidth();
        float size = h > w ? w : h;
        if (size > 0.5f) {
            size = 0.5f;
        }
        matrix.postScale(size, size);// 获取缩放比例
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // 根据缩放比例获取新的位图

        Bitmap newBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();
        // 设置边框颜色
        paint.setColor(background);
        canvas.drawRect(new Rect(0, 0, rect.width(), rect.height()), paint);
        if ((h > w ? w : h) > 0.5f) {
            canvas.drawBitmap(bitmap1, rect.width() / 2 - bitmap1.getWidth() / 2,
                    rect.height() / 2 - bitmap1.getHeight() / 2, null);
        } else {
            canvas.drawBitmap(bitmap1, rect.width() / 2 - bitmap1.getWidth() / 2, newPadding, null);
        }

        bitmap1.recycle();

        return newBitmap;
    }

    /**
     * Gets path pos.
     *
     * @return the path pos
     */
    public List<float[]> getPathPos() {
        return mPathPos;
    }

    /**
     * Gets sample rate.
     *
     * @return the sample rate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets sample rate.
     *
     * @param sampleRate the sample rate
     */
    public void setSampleRate(int sampleRate) {
        if (sampleRate >= 1)
            this.sampleRate = sampleRate;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (cacheCanvas != null) {
            cacheCanvas = null;
        }
        if (cachebBitmap != null) {
            cachebBitmap.recycle();
            cachebBitmap = null;
        }
    }
}
