package cn.schope.lightning.component.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import cn.schope.lightning.R
import com.facebook.rebound.*

/**
 * Spring切换按钮
 * Created by Administrator on 2015/10/29 0029.
 */
class SpringSwitchButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr), ViewTreeObserver.OnGlobalLayoutListener {
    private val DEFAULT_TEXTSIZE_SP = 16
    private val DEFAULT_TEXTPADDING = 10f
    private val DEFAULT_BORDERPADDING = 0f
    private val DEFAULT_TEXTCHECKED_COLOR = 0xFFFFFFFF.toInt()
    private val DEFAULT_TEXTUNCHECKED_COLOR = 0xffa668e4.toInt()
    private val DEFAULT_SWTICH_COLOR = 0xffa668e4.toInt()
    private val DEFAULT_BACKGROUND_COLOR = 0x00000000

    private val springSystem: SpringSystem = SpringSystem.create()
    private val switchPaint: Paint
    private val backgroundPaint: Paint
    private val textPaint: Paint
    private var leftText = ""
    private var rightText = ""
    /**
     * 选中的是否在左侧
     * @return
     */
    var isSwitchLeft = true
        private set
    private var textPadding: Float = 0.toFloat()

    private var textWidth: Float = 0.toFloat()
    //    private float rightTextWidth;
    private var textHeight: Float = 0.toFloat()
    private var textCheckedColor = DEFAULT_TEXTCHECKED_COLOR
    private var textUnCheckedColor = DEFAULT_TEXTUNCHECKED_COLOR
    private var baseLine = 0
    private val switchRect = RectF()
    private var leftTextWidth: Float = 0.toFloat()
    private var rightTextWidth: Float = 0.toFloat()
    private var downTime: Long = 0
    private var leftIsInited = false//currentLeft是否被初始化过了
    private var currentLeft: Float = 0.toFloat()
    private var backScaleX = 1.0
    private var isAnimating = false
    private var spring: Spring? = null
    private var onToggleListener: OnToggleListener? = null
    private val backgroundRectF = RectF()
    var borderPadding = DEFAULT_BORDERPADDING


    init {
        setBackgroundColor(Color.TRANSPARENT)
        textPaint = Paint()
        textPaint.isAntiAlias = true
        switchPaint = Paint()
        switchPaint.isAntiAlias = true
        backgroundPaint = Paint()
        backgroundPaint.isAntiAlias = true
        val a = context.obtainStyledAttributes(attrs, R.styleable.SpringSwitchButton)
        leftText = a.getString(R.styleable.SpringSwitchButton_leftText)
        if (!TextUtils.isEmpty(leftText)) {
            setLeftText(leftText)
        }
        rightText = a.getString(R.styleable.SpringSwitchButton_rightText)
        if (!TextUtils.isEmpty(rightText)) {
            setRightText(rightText)
        }
        setTextSize(a.getDimension(R.styleable.SpringSwitchButton_textSize, dp2px(DEFAULT_TEXTSIZE_SP.toFloat())))
        setTextPadding(a.getDimension(R.styleable.SpringSwitchButton_textPadding, dp2px(DEFAULT_TEXTPADDING)))
        borderPadding = a.getDimension(R.styleable.SpringSwitchButton_borderPadding, dp2px(DEFAULT_BORDERPADDING))

        setTextCheckedColor(a.getColor(R.styleable.SpringSwitchButton_textCheckedColor, DEFAULT_TEXTCHECKED_COLOR))
        setTextUnCheckedColor(a.getColor(R.styleable.SpringSwitchButton_textUnCheckedColor, DEFAULT_TEXTUNCHECKED_COLOR))
        setSwitchColor(a.getColor(R.styleable.SpringSwitchButton_switchColor, DEFAULT_SWTICH_COLOR))
        setBackgroundRectColor(a.getColor(R.styleable.SpringSwitchButton_backgroundColor, DEFAULT_BACKGROUND_COLOR))

        a.recycle()
        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    /**
     * 设置状态
     */
    fun setSwitchState(isSwitchLeft: Boolean) {
        this.leftIsInited = false
        this.isSwitchLeft = isSwitchLeft
        invalidate()
    }

    /**
     * 设置左侧显示的文字
     */
    fun setLeftText(leftText: String) {
        this.leftText = leftText
    }

    /**
     * 设置右侧显示的文字
     */
    fun setRightText(rightText: String) {
        this.rightText = rightText
    }


    /**
     * 设置按钮开关的监听
     */
    fun setOnToggleListener(onToggleListener: OnToggleListener) {
        this.onToggleListener = onToggleListener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        textHeight = Math.abs(textPaint.ascent() + textPaint.descent())
        leftTextWidth = textPaint.measureText(leftText)
        rightTextWidth = textPaint.measureText(rightText)
        textWidth = if (leftTextWidth > rightTextWidth) leftTextWidth else rightTextWidth
        val width = 4 * textPadding + 2 * textWidth + 2 * borderPadding
        val height = 2 * textPadding + textHeight + 2 * borderPadding
        val fontMetrics = textPaint.fontMetrics
        baseLine = ((height - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top).toInt()
        setMeasuredDimension(width.toInt(), height.toInt())

    }

    override fun onDraw(canvas: Canvas) {
        val SWITCH_RADIUS = measuredHeight / 2f - borderPadding
        val LEFT_TEXT_X = borderPadding + textPadding + (textWidth - leftTextWidth) / 2
        val TEXT_Y = baseLine.toFloat()
        val RIGHT_TEXT_X = borderPadding + 3 * textPadding + textWidth + (textWidth - rightTextWidth) / 2

        switchRect.apply {
            if (!leftIsInited) {
                left = if (isSwitchLeft) borderPadding else measuredWidth / 2f
                currentLeft = left
                leftIsInited = true
            } else {
                left = currentLeft
            }
            top = borderPadding
            right = left + 2 * textPadding + textWidth
            bottom = top + 2 * textPadding + textHeight
        }


        backgroundRectF.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(
                backgroundRectF,
                (measuredHeight / 2).toFloat(), (measuredHeight / 2).toFloat(), backgroundPaint)

        canvas.save()
        canvas.scale(backScaleX.toFloat(), 1f, if (isSwitchLeft) borderPadding else measuredWidth.toFloat() - borderPadding, 0f)
        canvas.drawRoundRect(switchRect, SWITCH_RADIUS, SWITCH_RADIUS, switchPaint)
        canvas.restore()
        //左侧文字
        textPaint.color = if (isSwitchLeft) textCheckedColor else textUnCheckedColor
        canvas.drawText(
                leftText, LEFT_TEXT_X,
                TEXT_Y, textPaint
        )
        //右侧文字
        textPaint.color = if (!isSwitchLeft) textCheckedColor else textUnCheckedColor
        canvas.drawText(
                rightText, RIGHT_TEXT_X,
                TEXT_Y, textPaint
        )

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        if (isAnimating) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
        } else if (event.action == MotionEvent.ACTION_UP) {
            if (downTime - System.currentTimeMillis() < 500) {
                if (event.x > measuredWidth / 2) {
                    if (isSwitchLeft) {
                        isSwitchLeft = false
                        if (onToggleListener != null) {
                            onToggleListener!!.onToggle(false)
                        }
                        val myLeftRightAnim = MyLeftRightAnim(true)
                        startAnimation(myLeftRightAnim)
                    }
                } else {
                    if (!isSwitchLeft) {
                        isSwitchLeft = true
                        if (onToggleListener != null) {
                            onToggleListener!!.onToggle(true)
                        }
                        val myLeftRightAnim = MyLeftRightAnim(false)
                        startAnimation(myLeftRightAnim)
                    }
                }

            }
        }
        return true
    }

    /**
     * dp转换为px
     * @param dp
     * @return
     */
    private fun dp2px(dp: Float): Float = context.resources.displayMetrics.density * dp

    fun setTextSize(textSize: Float) {
        textPaint.textSize = textSize
    }

    fun setTextPadding(textPadding: Float) {
        this.textPadding = textPadding
    }

    fun setBackgroundRectColor(color: Int) {
        backgroundPaint.color = color
    }


    /**
     * 文字选中时的颜色
     * @param color
     */
    fun setTextCheckedColor(color: Int) {
        this.textCheckedColor = color
    }

    /**
     * 文字未被选中的状态
     * @param color
     */
    fun setTextUnCheckedColor(color: Int) {
        this.textUnCheckedColor = color
    }

    /**
     * 设置背景矩形的颜色
     * @param color
     */
    fun setSwitchColor(color: Int) {
        switchPaint.color = color
    }

    override fun onGlobalLayout() {
        if (parent != null && parent is ViewGroup) {
            (parent as ViewGroup).clipChildren = false
            (parent as ViewGroup).clipToPadding = false
        }
    }

    /**
     * 从左到右的动画
     */
    private inner class MyLeftRightAnim(leftToRight: Boolean) : Animation(), Animation.AnimationListener {
        private var initLeft: Float = 0.toFloat()
        private var distance: Float = 0.toFloat()

        init {
            flushInitLeft(leftToRight)
            duration = 100
            setAnimationListener(this)
            interpolator = LinearInterpolator()
        }

        /**
         * 刷新初试值
         * @param leftToRight
         */
        fun flushInitLeft(leftToRight: Boolean) {
            this.initLeft = currentLeft
            distance = if (leftToRight) {
                measuredWidth / 2 - initLeft
            } else {
                -initLeft + borderPadding
            }
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            currentLeft = initLeft + interpolatedTime * distance
            invalidate()
        }

        override fun onAnimationStart(animation: Animation) {
            isAnimating = true
        }

        override fun onAnimationEnd(animation: Animation) {
            isAnimating = false
            if (spring != null) {
                spring!!.removeAllListeners()
            }
            spring = springSystem.createSpring()
            val sc = SpringConfig(100.0, 6.0)
            spring!!.springConfig = sc
            spring!!.endValue = 1.0
            spring!!.addListener(object : SimpleSpringListener() {
                override fun onSpringUpdate(spring: Spring?) {
                    // You can observe the updates in the spring
                    // state by asking its current value in onSpringUpdate.
                    //                    backScaleX = 1+(spring.getCurrentValue()-1)*0.2f;
                    backScaleX = SpringUtil.mapValueFromRangeToRange(spring!!.currentValue, 0.0, 1.0, 0.7, 1.0)
                    invalidate()
                    //                    if(backScaleX >= 1)spring.setAtRest();
                }
            })
        }

        override fun onAnimationRepeat(animation: Animation) {

        }
    }

    /**
     * 开关状态改变监听
     */
    interface OnToggleListener {
        fun onToggle(left: Boolean)
    }
}
