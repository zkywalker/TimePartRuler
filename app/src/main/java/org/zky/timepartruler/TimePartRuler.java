package org.zky.timepartruler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间尺控件
 * Created by kun on 2016/5/18.
 */

public class TimePartRuler extends View {
    private static final String TAG = "TimePartRuler";

    private int viewWidth;
    private int viewHeight;

    //上面矩形高度 0.5 height
    private int rectHeight;
    //尺子和矩形的间隔 dp
    private int spaceHeight = dp2px(3);
    //尺子的高度 0.4 height - space
//    private int rulerHeight;
    //刻度数字高度 0.1 height
//    private int numHeight;

    private Paint linePaint = new Paint();
    private Paint bgPaint = new Paint();
    private Paint lockPaint = new Paint();
    private Paint centerTextPaint = new Paint();

    //最小刻度（5min）长度：一页显示 三小时 最小刻度为5分钟 也就是一页有36个刻度
    private int timeScale = 1;
    //时间的长度 一天有24小时 也就是长度是 一小时长度*24
    private int totalTime = 1;
    //滚动    http://blog.csdn.net/guolin_blog/article/details/48719871
    private Scroller scroller;

    float lastX = 0;
    //选中的数据
    private List<TimePart> data;
    //矩形
    private Rect rect;
    //矩形2
    private Rect rect2;

    //背景颜色，可以修改
    private String bgColor = "#20000000";

    private Bitmap lock;

    //滚动监听
    private OnScrollListener scrollListener;

    public TimePartRuler(Context context) {
        super(context);
    }

    public TimePartRuler(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setAntiAlias(true);
        linePaint.setColor(Color.BLACK);
        linePaint.setTextAlign(Paint.Align.CENTER);
        linePaint.setTextSize(sp2px(6));

        lockPaint.setAntiAlias(true);
        lockPaint.setColor(Color.WHITE);

        centerTextPaint.setAntiAlias(true);
        centerTextPaint.setColor(Color.WHITE);
        centerTextPaint.setTextAlign(Paint.Align.CENTER);
        centerTextPaint.setTextSize(sp2px(7));

        bgPaint.setAntiAlias(true);
        bgPaint.setColor(Color.parseColor(bgColor));

        rect = new Rect();
        rect2 = new Rect();

        scroller = new Scroller(context);
        lock = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_suoding);
        data = new ArrayList<>();
    }

    public TimePartRuler(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 画刻度尺的背景
     * startX是 矩形框+空隙高度 startY endY设个偏移 时间条总长度为1小时长度(5分钟长度*12)乘以24 endY留个空间画刻度值
     *
     * @param canvas 画布
     */
    public void drawBg(Canvas canvas) {
        rect.set(-1, rectHeight + spaceHeight, timeScale * 12 * 24 + 1, (int) (viewHeight * 0.9));
        canvas.drawRect(rect, bgPaint);
    }

    /**
     * 画刻度尺刻度
     *
     * @param canvas 画布
     */
    public void drawLines(Canvas canvas) {
        //底部的线
        canvas.drawLine(0, (float) (viewHeight * 0.9), totalTime, (float) (viewHeight * 0.9), linePaint);
        for (int i = 0; i <= totalTime; i++) {
            int fiveMin = timeScale;
            //最小刻度五分钟来算  半小时刻度 正小时刻度
            if (i % fiveMin == 0) {
                if (i % (fiveMin * 6) == 0) {
                    if (i % (fiveMin * 12) == 0) {
                        //整小时刻度
                        canvas.drawLine(i, (float) (rectHeight + spaceHeight), i, (float) (viewHeight * 0.9), linePaint);
                        //画刻度值
                        canvas.drawText(formatString(i / (timeScale * 12), 0, 0), i, (float) (viewHeight * 0.97), linePaint);
                    } else {
                        //否则画半小时刻度
                        canvas.drawLine(i, (float) ((float) (rectHeight + spaceHeight) + viewHeight * 0.2), i, (float) (viewHeight * 0.9), linePaint);
                    }
                } else {
                    //5分钟刻度的六倍就是半小时刻度 ，包括了整小时刻度
                    canvas.drawLine(i, (float) ((float) (rectHeight + spaceHeight) + viewHeight * 0.2 + dp2px(6)), i, (float) (viewHeight * 0.9), linePaint);
                }
            }

        }
    }

    /**
     * 画时间片段，TODO 因为红框的时间片段要覆盖在其他时间片段上，所以防止覆盖 要后画
     *
     * @param canvas
     */
    public void drawTimeRect(Canvas canvas) {
        for (TimePart temp : data) {

            //根据五分钟的长度算出一分钟的长度，然后算出起始的x坐标
            // 如果是先除以3600小数点的数据会被舍去 位置就不准确了
            int x1 = temp.getStartSec() * timeScale / 300;
            int x2 = temp.getEndSec() * timeScale / 300;
            rect.set(x1, 0, x2 - dp2px(1), rectHeight);
            //如果是线框的形式
            if (temp.isStroke) {
                //因为线粗2dp，所以startX向右挪1dp，startY向下挪1dp，endX向左挪2dp，endY向左挪2dp
                rect.set(x1 + dp2px(1), dp2px(1), x2 - dp2px(1), rectHeight - dp2px(2));
            }
            //画框框
            canvas.drawRect(rect, temp.bgPaint);
            //画图标
            if (temp.isDrawIcon()){
//            Rect mSrcRect = new Rect(0, 0, dp2px(10), dp2px(10));
//            Rect mDestRect = new Rect(x2 - dp2px(12), rectHeight - dp2px(12), x2 - dp2px(2), rectHeight - dp2px(2));
                //图片大小矩形
                rect.set(0, 0, dp2px(10), dp2px(10));
                //图片位置矩形
                rect2.set(x2 - dp2px(12), rectHeight - dp2px(12), x2 - dp2px(2), rectHeight - dp2px(2));
                canvas.drawBitmap(lock, rect, rect2, lockPaint);
            }
            //画文字
            canvas.drawText(temp.text, x1 + (x2 - x1) / 2, rectHeight / 2 + dp2px(7) / 2, centerTextPaint);

        }

    }

    /**
     * 对时间进行格式化
     *
     * @param hour 小时
     * @param min  分钟
     * @param sec  秒
     * @return 字符串数字
     */
    public String formatString(int hour, int min, int sec) {
        StringBuilder builder = new StringBuilder();
        if (hour < 10) {
            builder.append("0").append(hour).append(":");
        } else {
            builder.append(hour).append(":");
        }
        if (min < 10 && min >= 0) {
            builder.append("0").append(min)
//                    .append(":")
            ;
        } else {
            builder.append(min)
//                    .append(":")
            ;
        }
//        if (sec < 10 && sec >= 0) {
//            builder.append("0").append(sec);
//        } else {
//            builder.append(sec);
//        }
        //TODO 精确到分 秒不要了
        return builder.toString();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (scroller != null && !scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                lastX = x;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dataX = lastX - x;
                int finalx = scroller.getFinalX();
                //右边
                if (dataX < 0) {
                    if (finalx < -viewWidth / 2) {
                        return super.onTouchEvent(event);
                    }
                }
                if (dataX > 0) {
                    if (finalx > timeScale * 12 * 21) {
                        return super.onTouchEvent(event);
                    }
                }
//                Log.d("--startScroll--","getFinalX "+scroller.getFinalX()+"getFinalY "+scroller.getFinalY());
                scroller.startScroll(scroller.getFinalX(), scroller.getFinalY(), (int) dataX, 0);
                lastX = x;
                postInvalidate();
                return true;
            case MotionEvent.ACTION_UP:
                int finalx1 = scroller.getFinalX();
                if (finalx1 < -viewWidth / 2) {
                    scroller.setFinalX(-viewWidth / 2);
                }
                if (finalx1 > timeScale * 12 * 21) {
                    scroller.setFinalX(timeScale * 12 * 21);
                }
                if (scrollListener != null) {
                    int finalX = scroller.getFinalX();
                    //表示每一个屏幕刻度的一半的总秒数，每一个屏幕有6格
                    int sec = 3 * 3600;
                    //滚动的秒数
                    int temsec = (int) Math.rint((double) finalX / (double) (timeScale * 12) * 3600);
                    sec += temsec;
                    //获取的时分秒
                    int thour = sec / 3600;
                    int tmin = (sec - thour * 3600) / 60;
                    int tsec = sec - thour * 3600 - tmin * 60;
                    scrollListener.onScrollFinish(thour, tmin, tsec);
                }
                postInvalidate();
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            invalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        viewWidth = getWidth();
        viewHeight = getHeight();
        //每小时的刻度一个屏幕分成3格
        timeScale = viewWidth / 36;
        //总的时间刻度距离
        totalTime = timeScale * 12 * 24;

        rectHeight = (int) (viewHeight * 0.5);


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBg(canvas);
        //选中的时间
        drawTimeRect(canvas);
        drawLines(canvas);
    }


    /**
     * 提供滑动到时间点的功能
     * @param time  滑到的时间
     */
    public void scrollTimeTo(int time) {
        if (0 < time && time < 24) {
            scroller.startScroll(0, 0, timeScale * 12 * time, 0);
            postInvalidate();
        }
    }

    //滚动监听类
    public interface OnScrollListener {
        public void onScroll(int hour, int min, int sec);

        public void onScrollFinish(int hour, int min, int sec);
    }

    public OnScrollListener getScrollListener() {
        return scrollListener;
    }

    public void setScrollListener(OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    //添加时间片段到容器中
    public void addTimePart(List<TimePart> temp) {
        if (temp != null) {
            data.addAll(temp);
            postInvalidate();
        }
    }

    //清除所有的时间片段数据
    public void clearData() {
        data.clear();
        postInvalidate();
    }

    //时间片段 用于标记选中的时间
    public static class TimePart {
        //开始的时间
        public int sHour, sMinute, sSeconds;
        //结束的时间
        public int eHour, eMinute, eSeconds;

        //用来画背景的画笔
        private Paint bgPaint;

        //是否允许画右下角的小锁图标
        private boolean isDrawIcon = false;

        //区块中间的文字
        private String text = "";

        //TODO
        public boolean isStroke = false;


        public TimePart(int sHour, int sMinute, int sSeconds, int eHour, int eMinute, int eSeconds) {
            this(sHour, sMinute, sSeconds, eHour, eMinute, eSeconds, "#aaaaaa");
        }

        public TimePart(int sHour, int sMinute, int sSeconds, int eHour, int eMinute, int eSeconds, String color) {
            this(sHour, sMinute, sSeconds, eHour, eMinute, eSeconds, color, "休息", false);
        }

        public TimePart(int sHour, int sMinute, int sSeconds, int eHour, int eMinute, int eSeconds, String color, String text, boolean isDrawIcon) {
            this.sHour = sHour;
            this.sMinute = sMinute;
            this.sSeconds = sSeconds;
            this.eHour = eHour;
            this.eMinute = eMinute;
            this.eSeconds = eSeconds;
            this.text = text;
            this.isDrawIcon = isDrawIcon;

            this.bgPaint = new Paint();
            this.bgPaint.setAntiAlias(true);
            this.bgPaint.setColor(Color.parseColor(color));
        }

        //TODO　传上下文是为了设置线框的宽度进行适配
        public TimePart(int sHour, int sMinute, int sSeconds, int eHour, int eMinute, int eSeconds, String color, String text, boolean isDrawIcon, boolean isStroke, Context context) {
            this(sHour, sMinute, sSeconds, eHour, eMinute, eSeconds, color, text, isDrawIcon);
            this.isStroke = isStroke;
            if (isStroke) {
                float scale = context.getResources().getDisplayMetrics().density;
                this.bgPaint.setStyle(Paint.Style.STROKE);
                this.bgPaint.setStrokeWidth((int) (2 * scale + 0.5f));
            }

        }


        public int getStartSec() {
            return sHour * 3600 + sMinute * 60 + sSeconds;
        }

        public int getEndSec() {
            return eHour * 3600 + eMinute * 60 + eSeconds;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Paint getBgPaint() {
            return bgPaint;
        }

        public void setBgPaint(Paint bgPaint) {
            this.bgPaint = bgPaint;
        }

        public boolean isDrawIcon() {
            return isDrawIcon;
        }

        public void setDrawIcon(boolean drawIcon) {
            isDrawIcon = drawIcon;
        }

    }

    private int dp2px(float dp) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private int sp2px(float spValue) {
        float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
}
