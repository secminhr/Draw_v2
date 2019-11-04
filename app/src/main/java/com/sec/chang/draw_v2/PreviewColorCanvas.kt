package com.sec.chang.draw_v2

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class PreviewColorCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val colorRGB = intArrayOf(0, 0, 0)

    fun previewColor(r: Int, g: Int, b: Int) {
        colorRGB[0] = r
        colorRGB[1] = g
        colorRGB[2] = b
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRGB(colorRGB[0], colorRGB[1], colorRGB[2])
    }
}
