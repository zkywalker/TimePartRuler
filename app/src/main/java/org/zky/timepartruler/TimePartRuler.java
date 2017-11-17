package org.zky.timepartruler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间尺控件
 * 参考
 * overScroller:
 * http://www.jianshu.com/p/293d0c2f56cb
 * scroller：
 * http://blog.csdn.net/guolin_blog/article/details/48719871
 * <p>
 * Created by kun on 2017/5/18.
 */

public class TimePartRuler extends View {
    private static final String TAG = "TimePartRuler";

    private int viewWidth;

    private int viewHeight;

    private int topRulerHeight;
    /**
     * 上面矩形高度 0.5 height
     */
    private int rectHeight;
    /**
     * 尺子和矩形的间隔 dp
     */
    private int spaceHeight = dp2px(2);

    private Paint linePaint = new Paint();

    private Paint bgPaint = new Paint();

    private Paint lockPaint = new Paint();

    private Paint centerTextPaint = new Paint();

    private Paint centerTextPaint2 = new Paint();

    private Paint midPaint = new Paint();

    /**
     * 最小刻度（5min）长度：一页显示 三小时 最小刻度为5分钟 也就是一页有36个刻度
     */
    private int timeScale = 1;
    /**
     * 时间的长度 一天有24小时 也就是长度是 一小时长度*24
     */
    private int totalTime = 1;
    /**
     * 滚动
     */
    private OverScroller scroller;

    /**
     * 加速度
     */
    private VelocityTracker tracker;

    private int mMaximumVelocity, mMinimumVelocity;

    float mlastX = 0;
    /**
     * 缓存x
     */
    float downX = 0;
    /**
     * 选择的x
     */
    float chooseX = 0;
    /**
     * 选中的数据
     */
    private List<TimePart> data;
    /**
     * 矩形
     */
    private Rect rect;
    /**
     * 矩形2
     */
    private Rect rect2;

    /**
     * 背景颜色，可以修改
     */
    private String bgColor = "#20000000";

    private Bitmap lock;

    /**
     * 是否画红色竖线
     */
    private boolean isShowMidLine = true;

    /**
     * 当前是否在操作刻度尺
     */
    private boolean isDraging = false;

    /**
     * 滚动监听
     */
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

        midPaint.setAntiAlias(true);
        midPaint.setStrokeWidth(3);
        midPaint.setColor(Color.RED);

        centerTextPaint.setAntiAlias(true);
        centerTextPaint.setColor(Color.WHITE);
        centerTextPaint.setTextAlign(Paint.Align.CENTER);
        centerTextPaint.setTextSize(sp2px(7));

        centerTextPaint2.setAntiAlias(true);
        centerTextPaint2.setColor(Color.WHITE);
        centerTextPaint2.setTextAlign(Paint.Align.CENTER);
        centerTextPaint2.setTextSize(sp2px(7));

        bgPaint.setAntiAlias(true);
        bgPaint.setColor(Color.parseColor(bgColor));

        rect = new Rect();
        rect2 = new Rect();

        scroller = new OverScroller(context);
        tracker = VelocityTracker.obtain();
        mMaximumVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context)
                .getScaledMinimumFlingVelocity();
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
        rect.set(-1, topRulerHeight + rectHeight + spaceHeight, timeScale * 12 * 24 + 1, (int) (viewHeight * 0.9));
        canvas.drawRect(rect, bgPaint);
    }

    /**
     * 画刻度尺刻度
     *
     * @param canvas 画布
     */
    public void drawLines(Canvas canvas) {
        //底部的线
//        canvas.drawLine(0, (float) (viewHeight * 0.9), totalTime, (float) (viewHeight * 0.9), linePaint);
        for (int i = 0; i <= totalTime; i++) {
            int fiveMin = timeScale;
            //最小刻度五分钟来算  半小时刻度 正小时刻度
            if (i % fiveMin == 0) {
                if (i % (fiveMin * 6) == 0) {
                    if (i % (fiveMin * 12) == 0) {
                        //整小时刻度
                        canvas.drawLine(i, (float) (topRulerHeight + rectHeight + spaceHeight), i, (float) (viewHeight * 0.9), linePaint);
                        //画刻度值
                        canvas.drawText(formatString(i / (timeScale * 12), 0, 0), i, (float) (viewHeight * 0.97), linePaint);
                    } else {
                        //否则画半小时刻度(上方的height+刻度尺的一半)
                        canvas.drawLine(i, (float) ((float) (topRulerHeight + rectHeight + spaceHeight) + viewHeight * 0.10), i, (float) (viewHeight * 0.9), linePaint);
                    }
                } else {
                    //5分钟刻度的六倍就是半小时刻度 ，包括了整小时刻度(上方的height+刻度尺的一半的一半)
                    canvas.drawLine(i, (float) ((float) (topRulerHeight + rectHeight + spaceHeight) + viewHeight * 0.15), i, (float) (viewHeight * 0.9), linePaint);
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
            if (temp.type == TimePart.TYPE_RECT) {
                rect.set(x1, topRulerHeight, x2 - dp2px(1), topRulerHeight + rectHeight);
                //如果是线框的形式
                if (temp.isStroke) {
                    //因为线粗2dp，所以startX向右挪1dp，startY向下挪1dp，endX向左挪2dp，endY向左挪2dp
                    rect.set(x1 + dp2px(1), topRulerHeight + dp2px(1), x2 - dp2px(1), topRulerHeight + rectHeight - dp2px(2));
                }
                //画框框
                canvas.drawRect(rect, temp.bgPaint);
                //画图标
                if (temp.isDrawIcon()) {
//            Rect mSrcRect = new Rect(0, 0, dp2px(10), dp2px(10));
//            Rect mDestRect = new Rect(x2 - dp2px(12), rectHeight - dp2px(12), x2 - dp2px(2), rectHeight - dp2px(2));
                    //图片大小矩形
                    rect.set(0, 0, dp2px(10), dp2px(10));
                    //图片位置矩形
                    rect2.set(x2 - dp2px(12), topRulerHeight + rectHeight - dp2px(12), x2 - dp2px(2), topRulerHeight + rectHeight - dp2px(2));
                    canvas.drawBitmap(lock, rect, rect2, lockPaint);
                }
                //画文字
                canvas.drawText(temp.text, x1 + (x2 - x1) / 2, topRulerHeight + rectHeight / 2 + dp2px(7) / 2, centerTextPaint);
            } else if (temp.type == TimePart.TYPE_TOP) {
                //画左边竖线
//                canvas.drawLine(x1,0,x1,topRulerHeight,linePaint);
                //画右边竖线
//                canvas.drawLine(x2,0,x2,topRulerHeight,linePaint);

                rect.set(x1, 0, x2 - dp2px(1), topRulerHeight - spaceHeight);

                //画框框
                canvas.drawRect(rect, temp.bgPaint);

                //画文字
                centerTextPaint2.setColor(Color.parseColor("#4c98f6"));
                canvas.drawText(temp.text, x1 + (x2 - x1) / 2, topRulerHeight / 2 + dp2px(7) / 2, centerTextPaint2);

            }


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
        // TODO 精确到分 秒不要了
        return builder.toString();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float currentX = event.getX();
        if (tracker == null) {
            tracker = VelocityTracker.obtain();
        }
        tracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                mlastX = currentX;
                downX = currentX;
                return true;
            case MotionEvent.ACTION_MOVE:
                isDraging = true;

                float moveX = mlastX - currentX;
                mlastX = currentX;
                scrollBy((int) (moveX), 0);

//                postInvalidate();
                return true;
            case MotionEvent.ACTION_UP:
                isDraging = false;

                //当手势是点击的时候
                if ((int) currentX / 10 == (int) downX / 10) {

                    float totalX = getScrollX() + currentX;
                    int totalMin = (int) (totalX / timeScale) * 5;
                    int realH = totalMin / 60;
                    int realM = totalMin - realH * 60;

                    chooseX = (int) (currentX / timeScale) * timeScale;

                    if (scrollListener != null) {
                        scrollListener.onChoose(realH, realM);
                    }

                }
                //当手指立刻屏幕时，获得速度，作为fling的初始速度
                tracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) tracker.getXVelocity();
                if (Math.abs(initialVelocity) > mMinimumVelocity) {

                    fling(-initialVelocity);

                }

                //VelocityTracker回收
                if (tracker != null) {
                    tracker.recycle();
                    tracker = null;
                }

                postInvalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                isDraging = false;
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                //VelocityTracker回收
                if (tracker != null) {
                    tracker.recycle();
                    tracker = null;
                }
                break;
            default:
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * 带滑行的滑动，左边滑到 - 1各半小时(中间)，右边同理
     *
     * @param i 加速度
     */
    private void fling(int i) {
        scroller.fling(getScrollX(), 0, i, 0, -(timeScale * 6 * 3), totalTime - (timeScale * 6 * 3), 0, 30);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            invalidate();
        }

    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        //当在拖动或者滑行的时候的时候才调用这个回调，要不然会和绑定联动的seekbar有冲突
        if ((!scroller.isFinished() || isDraging) && scrollListener != null) {
            int m = x / (timeScale / 5);
            int h = m / 60;
            scrollListener.onScroll(h, m % 60, 0);
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

        topRulerHeight = (int) (viewHeight * 0.2);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        drawBg(canvas);
        //选中的时间
        drawTimeRect(canvas);
        drawLines(canvas);
        drawMidLine(canvas);
    }

    /**
     * 画指针
     *
     * @param canvas 画布
     */
    private void drawMidLine(Canvas canvas) {
        //画指针
        if (isShowMidLine) {
            canvas.drawLine(chooseX + scroller.getFinalX(), 0,
                    chooseX + scroller.getFinalX(), viewHeight, midPaint);
        }


    }


    /**
     * 提供滑动到时间点的功能
     *
     * @param time 滑到的时间
     */
    public void scrollTimeTo(int time) {
        if (0 < time && time < 24) {
            scroller.startScroll(0, 0, timeScale * 12 * time, 0);
            postInvalidate();
        }
    }

    /**
     * 滑动到时间
     *
     * @param time 带小数的小时
     */
    public void scrollTimeTo(float time) {
        if (scroller.isFinished() && !isDraging) {
            scrollTo((int) (timeScale * 12 * time), 0);
            postInvalidate();
        }

    }

    public boolean isShowMidLine() {
        return isShowMidLine;
    }

    public void setShowMidLine(boolean showMidLine) {
        isShowMidLine = showMidLine;
    }

    /**
     * 当尺子滑动的时候 seekbar是不能动的
     *
     * @return 是否可以动
     */
    public boolean canSeekBarCall() {
        if (!scroller.isFinished() || isDraging) {
            return false;
        }
        return true;
    }


    /**
     * 滚动监听类
     */
    public interface OnScrollListener {
        void onScroll(int hour, int min, int sec);

        void onScrollFinish(int hour, int min, int sec);

        void onChoose(int hour, int min);
    }

    public OnScrollListener getScrollListener() {
        return scrollListener;
    }

    public void setScrollListener(OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    /**
     * 添加时间片段到容器中
     *
     * @param temp
     */
    public void addTimePart(List<TimePart> temp) {
        if (temp != null) {
            data.addAll(temp);
            postInvalidate();
        }
    }

    /**
     * 清除所有的时间片段数据
     */
    public void clearData() {
        data.clear();
        postInvalidate();
    }

    /**
     * 时间片段 用于标记选中的时间
     */
    public static class TimePart {
        public static int TYPE_RECT = 0;

        public static int TYPE_TOP = 1;

        public int type = 0;
        /**
         * 开始的时间
         */
        public int sHour, sMinute, sSeconds;
        /**
         * 结束的时间
         */
        public int eHour, eMinute, eSeconds;

        /**
         * 用来画背景的画笔
         */
        private Paint bgPaint;

        /**
         * 是否允许画右下角的小锁图标
         */
        private boolean isDrawIcon = false;

        /**
         * 区块中间的文字
         */
        private String text = "";

        //TODO
        public boolean isStroke = false;


        public TimePart(int sHour, int sMinute, int sSeconds, int eHour, int eMinute, int eSeconds) {
            this(sHour, sMinute, sSeconds, eHour, eMinute, eSeconds, "#aaaaaa");
        }

        public TimePart(int sHour, int sMinute, int sSeconds, int eHour, int eMinute, int eSeconds, String color) {
            this(sHour, sMinute, sSeconds, eHour, eMinute, eSeconds, color, "休息", false);
        }

        public TimePart(int sHour, int sMinute, int sSeconds, int eHour, int eMinute, int eSeconds, String color, String text, int type) {
            this(sHour, sMinute, sSeconds, eHour, eMinute, eSeconds, color, text, false);
            this.type = type;
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

        /**
         * 起始时间
         *
         * @param sHour      起始时间
         * @param sMinute    起始时间
         * @param sSeconds   起始时间
         * @param lenMin     间隔长度
         * @param color      框的颜色
         * @param text       中间文字
         * @param isDrawIcon 画不画图案
         * @param isStroke
         * @param context
         */
        public TimePart(int sHour, int sMinute, int sSeconds, int lenMin, String color, String text, boolean isDrawIcon, boolean isStroke, Context context) {
            this(sHour, sMinute, sSeconds, 0, 0, 0, color, text, isDrawIcon, isStroke, context);
            this.isStroke = isStroke;

            //当大于一个小时时进位
            if (sMinute + lenMin > 60) {
                //不允许超过当天
                if (sHour + 1 > 24) {
                    eHour = 24;
                } else {
                    eHour = sHour + 1;
                }
                eMinute = sMinute + lenMin - 60;
            } else {
                eHour = sHour;
                eMinute = sMinute + lenMin;
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

        @Override
        public String toString() {
            return "TimePart{" +
                    "sHour=" + sHour +
                    ", sMinute=" + sMinute +
                    ", sSeconds=" + sSeconds +
                    ", eHour=" + eHour +
                    ", eMinute=" + eMinute +
                    ", eSeconds=" + eSeconds +
                    ", bgPaint=" + bgPaint +
                    ", isDrawIcon=" + isDrawIcon +
                    ", text='" + text + '\'' +
                    ", isStroke=" + isStroke +
                    '}';
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
