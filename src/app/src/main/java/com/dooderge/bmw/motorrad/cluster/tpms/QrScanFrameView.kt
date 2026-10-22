package com.dooderge.bmw.motorrad.cluster.tpms

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * QR 스캔 뷰파인더: 반투명 딤 + 중앙 프레임(모서리 브래킷).
 */
class QrScanFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.SQUARE
    }

    private val holePath = Path()
    private val holeRect = RectF()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val frameSize = min(w, h) * 0.42f
        val left = (w - frameSize) / 2f
        val top = (h - frameSize) / 2f
        holeRect.set(left, top, left + frameSize, top + frameSize)

        holePath.reset()
        holePath.fillType = Path.FillType.EVEN_ODD
        holePath.addRect(0f, 0f, w, h, Path.Direction.CW)
        holePath.addRoundRect(holeRect, 8f * density, 8f * density, Path.Direction.CCW)
        canvas.drawPath(holePath, dimPaint)

        val corner = frameSize * 0.18f
        val stroke = framePaint.strokeWidth
        val l = holeRect.left
        val t = holeRect.top
        val r = holeRect.right
        val b = holeRect.bottom

        // 좌상
        canvas.drawLine(l, t + corner, l, t, framePaint)
        canvas.drawLine(l - stroke / 2f, t, l + corner, t, framePaint)
        // 우상
        canvas.drawLine(r - corner, t, r + stroke / 2f, t, framePaint)
        canvas.drawLine(r, t, r, t + corner, framePaint)
        // 좌하
        canvas.drawLine(l, b - corner, l, b, framePaint)
        canvas.drawLine(l - stroke / 2f, b, l + corner, b, framePaint)
        // 우하
        canvas.drawLine(r - corner, b, r + stroke / 2f, b, framePaint)
        canvas.drawLine(r, b - corner, r, b, framePaint)
    }
}
