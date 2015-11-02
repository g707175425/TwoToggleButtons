package cn.schope.gongyasen.twotogglebuttonlib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by 宫亚森 on 2015/10/19.
 */
public class MyToggleButton extends View {
    private static final int ANIM_TIME = 500;

    private float roundRadius = 0;
    private Paint backgroundPaint = new Paint();
    private Paint foregroundPaint = new Paint();
    private Paint circleBackgroundPaint = new Paint();
    private RectF backgroundRectF = new RectF();
    private int backgroundColor = 0xFFB0B1B1;
    private int foregroundColor = 0xFF78C807;
    private Drawable checkerDrawable;//对钩的图片资源
    private boolean isOpen = true;
    private float checkerScrollPercent = isOpen?0f:1.0f;
    private Drawable XDrawable;
    private float startX = 0;
    private OnToggleChangedListener onToggleChangedListener;
    private long downTime;
    private boolean isMove = false;


    public MyToggleButton(Context context) {
        this(context, null);
    }

    public MyToggleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);
        foregroundPaint.setAntiAlias(true);
        foregroundPaint.setColor(foregroundColor);
        circleBackgroundPaint.setAntiAlias(true);
        circleBackgroundPaint.setColor(Color.WHITE);
        checkerDrawable = getResources().getDrawable(R.drawable.duigou);
        XDrawable = getResources().getDrawable(R.drawable.cha);
//        animateScroll();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        backgroundRectF.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        roundRadius = getMeasuredHeight()/2;
    }

    public void setToggleState(boolean isOpen){
//        if(this.isOpen == isOpen)return;
        this.isOpen = isOpen;
        if(isOpen){
            MyOpenAnimation myScrollAnimation = new MyOpenAnimation(checkerScrollPercent);
            myScrollAnimation.setDuration(ANIM_TIME);
            startAnimation(myScrollAnimation);
            myScrollAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    MyToggleButton.this.isOpen = true;
                    if(onToggleChangedListener != null){
                        onToggleChangedListener.onToggleChanged(true);
                    }
//                    System.out.println("状态:" + true);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }else{
            MyCloseAnimation myScrollAnimation = new MyCloseAnimation(checkerScrollPercent);
            myScrollAnimation.setDuration(ANIM_TIME);
            startAnimation(myScrollAnimation);
            myScrollAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    MyToggleButton.this.isOpen = false;
                    if(onToggleChangedListener != null){
                        onToggleChangedListener.onToggleChanged(false);
                    }
                    System.out.println("状态:" + false);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }

    /**
     * dp转换为px
     * @param dp
     * @return
     */
    private float dp2px(float dp){
        return getContext().getResources().getDisplayMetrics().density * dp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(backgroundRectF, roundRadius, roundRadius, backgroundPaint);
        int foreAlpha = 0;
        int backAlpha = 0;
        if(checkerScrollPercent <= 0.7 && checkerScrollPercent > 0.3){
            //
            foreAlpha = (int) (255*(1 - (checkerScrollPercent - 0.3)/0.4));
            backAlpha = (int) (255*(checkerScrollPercent - 0.3)/0.4);
        }else if(checkerScrollPercent > 0.7){
            foreAlpha = 0;
            backAlpha = 255;
        }else{
            foreAlpha = 255;
            backAlpha = 0;
        }

        foregroundPaint.setAlpha((int) (255*(1-checkerScrollPercent)));
        circleBackgroundPaint.setAlpha(foreAlpha);
        canvas.drawRoundRect(backgroundRectF,roundRadius,roundRadius, foregroundPaint);
        //圆圈距离边界的距离
        int circleMargin = 2;
        //圆圈的直径
        int circleWidth = getMeasuredHeight()-circleMargin*2;
        float scrollLength = (getMeasuredWidth() - circleWidth - circleMargin * 2) * checkerScrollPercent;
        //绘制绿色的对钩
        int checkerLeft = circleMargin;
        int cx = checkerLeft + circleWidth / 2;//圆形的中心点
        int cy = getMeasuredHeight() / 2;//圆形的中心点
        canvas.save();
        canvas.translate(scrollLength, 0);
        canvas.rotate(360 * checkerScrollPercent, cx, cy);
        canvas.drawCircle(cx, cy, circleWidth / 2, circleBackgroundPaint);
        checkerDrawable.setBounds(checkerLeft, circleMargin, checkerLeft + circleWidth, circleWidth);
        checkerDrawable.setAlpha(foreAlpha);
        checkerDrawable.draw(canvas);
        canvas.restore();
        //绘制灰色的差
        int XLeft = 0;//getMeasuredWidth() - circleMargin - circleWidth;
        int xcx = XLeft + circleWidth / 2;//圆形的中心点
        int xcy = getMeasuredHeight() / 2;//圆形的中心点

        circleBackgroundPaint.setAlpha(backAlpha);
        canvas.save();
        canvas.translate(scrollLength, 0);
        canvas.rotate(360 * checkerScrollPercent, xcx, xcy);
        canvas.drawCircle(xcx, xcy, circleWidth / 2, circleBackgroundPaint);
        XDrawable.setBounds(XLeft, circleMargin, XLeft + circleWidth, circleWidth);
        XDrawable.setAlpha(backAlpha);
        XDrawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                clearAnimation();
                downTime = System.currentTimeMillis();
                isMove = false;
                break;
            case MotionEvent.ACTION_MOVE:
                isMove = true;
                float newX = event.getX();
                float distanceX = newX - startX;
                checkerScrollPercent += distanceX / getMeasuredWidth();
                if(checkerScrollPercent > 1){
                    checkerScrollPercent = 1;
                }else if(checkerScrollPercent < 0){
                    checkerScrollPercent = 0;
                }
                invalidate();

                startX = newX;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if(System.currentTimeMillis() - downTime < 500 && !isMove){
                    setToggleState(event.getX() < getMeasuredWidth()/2);
                }else{
                    setToggleState(checkerScrollPercent < 0.5f);
                }
                break;
        }
        return true;
    }

    /**
     * 执行滚动动画
     */
    public void animateScroll(){
        MyScrollAnimation myScrollAnimation = new MyScrollAnimation();
        myScrollAnimation.setDuration(ANIM_TIME);
        myScrollAnimation.setRepeatCount(Animation.INFINITE);
        myScrollAnimation.setRepeatMode(Animation.REVERSE);
        startAnimation(myScrollAnimation);
    }

    public void setOnToggleChangedListener(OnToggleChangedListener onToggleChangedListener){
        this.onToggleChangedListener = onToggleChangedListener;
    }


    private class MyOpenAnimation extends Animation{
        private float initPercent = 0;

        public MyOpenAnimation(float initPercent){
            this.initPercent = initPercent;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            checkerScrollPercent = initPercent*(1-interpolatedTime);
            invalidate();
        }
    }

    private class MyCloseAnimation extends Animation{
        private float initPercent = 0;
        public MyCloseAnimation(float initPercent){
            this.initPercent = initPercent;
        }
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            checkerScrollPercent = initPercent+ (1-initPercent)*interpolatedTime;
            invalidate();
        }
    }

    private class MyScrollAnimation extends Animation{
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            checkerScrollPercent = interpolatedTime;
            invalidate();
        }
    }

    public interface OnToggleChangedListener{
        void onToggleChanged(boolean isOpen);
    }

    /**
     * 当前开关状态
     * @return
     */
    public boolean isOpen(){
        return isOpen;
    }
}
