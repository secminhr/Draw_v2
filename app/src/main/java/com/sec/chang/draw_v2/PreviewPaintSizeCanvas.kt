package com.sec.chang.draw_v2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class PreviewPaintSizeCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var drawPaint: Paint? = null
    private var canvasWidth: Int = 0
    private var canvasHeight: Int = 0
    private var lineWidth: Int = 0
    private var isEraserMode: Boolean = false

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint = Paint()
        drawPaint!!.isAntiAlias = true
        drawPaint!!.style = Paint.Style.STROKE
        drawPaint!!.strokeJoin = Paint.Join.ROUND
        drawPaint!!.strokeCap = Paint.Cap.ROUND
        drawPaint!!.color = Color.BLACK
    }

    fun previewSize(DPSize: Int, isEraser: Boolean, R: Int, G: Int, B: Int) {
        if (isEraser) {
            isEraserMode = true
            drawPaint!!.color = Color.WHITE
        } else {
            isEraserMode = false
            drawPaint!!.color = Color.rgb(R, G, B)
        }
        drawPaint!!.strokeWidth = (DPSize * resources.displayMetrics.density.toInt()).toFloat()
        lineWidth = DPSize
        invalidate() //go onDraw
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasWidth = w
        canvasHeight = h
    }

    override fun onDraw(canvas: Canvas) {
        if (!isEraserMode) {
            canvas.drawLine((24 + lineWidth / 2).toFloat(), (canvasHeight / 2).toFloat(), (canvasWidth - 24 - lineWidth / 2).toFloat(), canvasHeight.toFloat() / 2, drawPaint!!)
        } else {
            canvas.drawColor(Color.BLACK)
            canvas.drawLine((24 + lineWidth / 2).toFloat(), (canvasHeight / 2).toFloat(), (canvasWidth - 24 - lineWidth / 2).toFloat(), canvasHeight.toFloat() / 2, drawPaint!!)
        }
    }
}
