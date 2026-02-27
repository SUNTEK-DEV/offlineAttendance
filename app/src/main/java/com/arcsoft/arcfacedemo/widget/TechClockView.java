package com.arcsoft.arcfacedemo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 科技感时钟视图
 * 显示实时时间，带发光效果
 */
public class TechClockView extends View {

    private Paint paint;
    private Paint glowPaint;
    private Paint datePaint;

    private String timeText = "";
    private String dateText = "";

    public TechClockView(Context context) {
        super(context);
        init();
    }

    public TechClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TechClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 时间画笔
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFF00FFFF); // 青色
        paint.setTextSize(80);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setShadowLayer(20, 0, 0, 0xFF00FFFF);

        // 发光画笔
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(0x4000FFFF); // 半透明青色
        glowPaint.setTextSize(80);
        glowPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        glowPaint.setTextAlign(Paint.Align.CENTER);

        // 日期画笔
        datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setColor(0xFF00CCCC); // 深青色
        datePaint.setTextSize(24);
        datePaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setShadowLayer(10, 0, 0, 0xFF00FFFF);

        // 启用阴影层
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        updateTime();
        startClock();
    }

    private void updateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault());

        timeText = timeFormat.format(new Date());
        dateText = dateFormat.format(new Date());
    }

    private void startClock() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                updateTime();
                invalidate();
                postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // 绘制发光效果
        canvas.drawText(timeText, centerX, centerY + 30, glowPaint);

        // 绘制时间
        canvas.drawText(timeText, centerX, centerY + 30, paint);

        // 绘制日期
        canvas.drawText(dateText, centerX, centerY - 50, datePaint);
    }
}
