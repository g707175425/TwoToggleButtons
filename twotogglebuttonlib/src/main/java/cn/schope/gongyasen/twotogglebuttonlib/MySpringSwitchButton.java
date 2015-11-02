package cn.schope.gongyasen.twotogglebuttonlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;

/**
 * Created by Administrator on 2015/10/29 0029.
 */
public class MySpringSwitchButton extends View implements ViewTreeObserver.OnGlobalLayoutListener {
    private final int DEFAULT_TEXTSIZE_SP = 16;
    private final int DEFAULT_TEXTPADDING = 10;
    private final int DEFAULT_TEXTCHECKED_COLOR = 0xFFFFFFFF;
    private final int DEFAULT_TEXTUNCHECKED_COLOR = 0xffa668e4;
    private final int DEFAULT_BACKRECT_COLOR = 0xffa668e4;

    private SpringSystem springSystem;
    private Paint backPaint;
    private Paint textPaint;
    private String leftText = "是";
    private String rightText = "否";
    private boolean isSwitchLeft = true;
    private float textPadding;

    private float textWidth;
//    private float rightTextWidth;
    private float textHeight;
    private int textCheckedColor = DEFAULT_TEXTCHECKED_COLOR;
    private int textUnCheckedColor = DEFAULT_TEXTUNCHECKED_COLOR;
    private int baseline;
    private RectF backRect = new RectF();
    private float leftTextWidth;
    private float rightTextWidth;
    private long downTime;
    private boolean leftIsInited = false;//currentLeft是否被初始化过了
    private float currentLeft;
    private double backScaleX = 1;
    private boolean isAnimating = false;
    private Spring spring;
    private OnToggleListener onToggleListener;

    public MySpringSwitchButton(Context context) {
        this(context, null);
    }

    public MySpringSwitchButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MySpringSwitchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        springSystem = SpringSystem.create();
        setBackgroundColor(Color.TRANSPARENT);
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        backPaint = new Paint();
        backPaint.setAntiAlias(true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MySwitchButton);
        String leftText = a.getString(R.styleable.MySwitchButton_leftText);
        if(!TextUtils.isEmpty(leftText)){
            setLeftText(leftText);
        }
        String rightText = a.getString(R.styleable.MySwitchButton_rightText);
        if(!TextUtils.isEmpty(rightText)){
            setRightText(rightText);
        }
        setTextSize(a.getDimension(R.styleable.MySwitchButton_textSize, dp2px(DEFAULT_TEXTSIZE_SP)));
        setTextPadding(a.getDimension(R.styleable.MySwitchButton_textPadding, dp2px(DEFAULT_TEXTPADDING)));

        setTextCheckedColor(a.getColor(R.styleable.MySwitchButton_textCheckedColor, DEFAULT_TEXTCHECKED_COLOR));
        setTextUnCheckedColor(a.getColor(R.styleable.MySwitchButton_textUnCheckedColor, DEFAULT_TEXTUNCHECKED_COLOR));
        setBackRectColor(a.getColor(R.styleable.MySwitchButton_backRectColor, DEFAULT_BACKRECT_COLOR));

        a.recycle();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * 设置状态
     * @param isSwitchLeft
     */
    public void setSwitchState(boolean isSwitchLeft){
        this.isSwitchLeft = isSwitchLeft;
        invalidate();
    }

    /**
     * 设置左侧显示的文字
     * @param leftText
     */
    public void setLeftText(String leftText){
        this.leftText = leftText;
    }

    /**
     * 设置右侧显示的文字
     * @param rightText
     */
    public void setRightText(String rightText){
        this.rightText = rightText;
    }


    /**
     * 设置按钮开关的监听
     */
    public void setOnToggleListener(OnToggleListener onToggleListener){
        this.onToggleListener = onToggleListener;
    }

    /**
     * 选中的是否在左侧
     * @return
     */
    public boolean isSwitchLeft(){
        return isSwitchLeft;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        textHeight = Math.abs(textPaint.ascent() + textPaint.descent());
        leftTextWidth = textPaint.measureText(leftText);
        rightTextWidth = textPaint.measureText(rightText);
        textWidth = leftTextWidth > rightTextWidth? leftTextWidth:rightTextWidth;
        float width = 4*textPadding + 2*textWidth;
        float height = 2*textPadding + textHeight;
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        baseline = (int) ((height - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top);
        setMeasuredDimension((int) width, (int) height);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(!leftIsInited){
            backRect.left = isSwitchLeft?0:getMeasuredWidth()/2;
            currentLeft = backRect.left;
            leftIsInited = true;
        }else{
            backRect.left = currentLeft;
        }
//        if(backRect.left < 0)backRect.left = 0;
        backRect.top = 0;
        backRect.right = backRect.left+2 * textPadding + textWidth;
//        if(backRect.right > getMeasuredWidth())backRect.right = getMeasuredWidth();
        backRect.bottom = 2 * textPadding + textHeight;
        canvas.save();
        canvas.scale((float) backScaleX,1,isSwitchLeft?0:getMeasuredWidth(), 0);
        canvas.drawRoundRect(backRect, getMeasuredHeight() / 2, getMeasuredHeight() / 2, backPaint);
        canvas.restore();
        textPaint.setColor(isSwitchLeft ? textCheckedColor : textUnCheckedColor);
        canvas.drawText(leftText, textPadding + (textWidth - leftTextWidth) / 2, baseline, textPaint);
        textPaint.setColor(!isSwitchLeft ? textCheckedColor : textUnCheckedColor);
        canvas.drawText(rightText, 3 * textPadding + textWidth + (textWidth - rightTextWidth) / 2, baseline, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isEnabled())return false;
        if(isAnimating)return false;
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            downTime = System.currentTimeMillis();
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            if(downTime - System.currentTimeMillis() < 500){
                if(event.getX() > getMeasuredWidth()/2){
                    if(isSwitchLeft){
                        isSwitchLeft = false;
                        if(onToggleListener != null){
                            onToggleListener.onToggle(false);
                        }
                        MyLeftRightAnim myLeftRightAnim = new MyLeftRightAnim(true);
                        startAnimation(myLeftRightAnim);
                    }
                }else{
                    if(!isSwitchLeft){
                        isSwitchLeft = true;
                        if(onToggleListener != null){
                            onToggleListener.onToggle(true);
                        }
                        MyLeftRightAnim myLeftRightAnim = new MyLeftRightAnim(false);
                        startAnimation(myLeftRightAnim);
                    }
                }

            }
        }
        return true;
    }

    /**
     * dp转换为px
     * @param dp
     * @return
     */
    private float dp2px(float dp){
        return getContext().getResources().getDisplayMetrics().density * dp;
    }

    public void setTextSize(float textSize) {
        textPaint.setTextSize(textSize);
    }

    public void setTextPadding(float textPadding) {
        this.textPadding = textPadding;
    }

    /**
     * 文字选中时的颜色
     * @param color
     */
    public void setTextCheckedColor(int color){
        this.textCheckedColor = color;
    }

    /**
     * 文字未被选中的状态
     * @param color
     */
    public void setTextUnCheckedColor(int color){
        this.textUnCheckedColor = color;
    }

    /**
     * 设置背景矩形的颜色
     * @param color
     */
    public void setBackRectColor(int color){
        backPaint.setColor(color);
    }

    @Override
    public void onGlobalLayout() {
        if(getParent() != null && getParent() instanceof ViewGroup){
            ((ViewGroup) getParent()).setClipChildren(false);
            ((ViewGroup) getParent()).setClipToPadding(false);
        }
    }

    /**
     * 从左到右的动画
     */
    private class MyLeftRightAnim extends Animation implements Animation.AnimationListener {
        private float initLeft;
        private float distance;

        public MyLeftRightAnim(boolean leftToRight){
            flushInitLeft(leftToRight);
            setDuration(100);
            setAnimationListener(this);
            setInterpolator(new LinearInterpolator());
        }

        /**
         * 刷新初试值
         * @param leftToRight
         */
        public void flushInitLeft(boolean leftToRight){
            this.initLeft = currentLeft;
//            this.leftToRight = leftToRight;
            if(leftToRight){
                distance = getMeasuredWidth() / 2 - initLeft;
            }else{
                distance = -initLeft;
            }
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            currentLeft = initLeft + interpolatedTime * distance;
            invalidate();
        }

        @Override
        public void onAnimationStart(Animation animation) {
            isAnimating = true;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            isAnimating = false;
            if(spring != null){
                spring.removeAllListeners();
            }
            spring = springSystem.createSpring();
            SpringConfig sc = new SpringConfig(100,6);
            spring.setSpringConfig(sc);
            spring.setEndValue(1);
            spring.addListener(new SimpleSpringListener() {
                @Override
                public void onSpringUpdate(Spring spring) {
                    // You can observe the updates in the spring
                    // state by asking its current value in onSpringUpdate.
//                    backScaleX = 1+(spring.getCurrentValue()-1)*0.2f;
                    backScaleX = SpringUtil.mapValueFromRangeToRange(spring.getCurrentValue(), 0, 1, 0.7, 1);
                    invalidate();
//                    if(backScaleX >= 1)spring.setAtRest();
                }
            });
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    /**
     * 开关状态改变监听
     */
    public interface OnToggleListener{
        void onToggle(boolean left);
    }
}
