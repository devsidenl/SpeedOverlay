package nl.jouwpakket.speedoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.min

class SpeedOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = spToPx(24f)
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private var speedValue: Float = 0f
    private var scaleFactor: Float = 1f

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setSpeed(speed: Float) {
        speedValue = speed
        invalidate()
    }

    fun setOverlayAlpha(alphaValue: Int) {
        val clamped = alphaValue.coerceIn(0, 255)
        alpha = clamped / 255f
        invalidate()
    }

    fun setScaleFactor(scale: Float) {
        scaleFactor = scale
        requestLayout()
        invalidate()
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val baseSize = dpToPx(120f * scaleFactor)
        val w = resolveSize(baseSize.toInt(), widthMeasureSpec)
        val h = resolveSize(baseSize.toInt(), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = min(width, height) / 2f * 0.9f
        canvas.drawCircle(width / 2f, height / 2f, radius, circlePaint)
        val speedText = speedValue.toInt().toString()
        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(speedText, width / 2f, textY, textPaint)
    }
}
