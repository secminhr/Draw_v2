package com.sec.chang.draw_v2

import android.content.Context
import android.graphics.*
import android.os.AsyncTask
import android.os.Environment
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.File

class CanvasView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    internal var isUsingEraser = false
    private var drawPath = Path()
    private var drawPaint = Paint()
    private var canvasPaint: Paint? = null
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    var paintLineWidth = 10 //dp
    var eraserLineWidth = 10 //dp
        set(value) {
            drawPath.reset()
            field = value
        }
    private var saveTask = saveImageAsyncTask()

    internal var colorR: Int = 0
    internal var colorG: Int = 0
    internal var colorB: Int = 0

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = paintLineWidth.toFloat()
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)
    }

    fun cleanCanvas() {
        val width = canvasBitmap!!.width
        val height = canvasBitmap!!.height
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawPath.reset()
        drawCanvas = Canvas(canvasBitmap!!)
        drawCanvas!!.drawColor(Color.WHITE)
        isUsingEraser = false
        invalidate()
        saveImage()
    }

    private fun saveImage() {
        try {
            saveTask.execute(canvasBitmap)
        } catch (e: IllegalStateException) {
            if (saveTask.status == AsyncTask.Status.FINISHED) {
                saveTask = saveImageAsyncTask()
                saveTask.execute(canvasBitmap)
            }
        }

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        if (!isInEditMode) {
            val picFile = File(Environment.getExternalStorageDirectory().absolutePath + "/Draw_v2/Draw_v2.png")
            if (picFile.exists() && picFile.canRead()) {
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                val bitmap = BitmapFactory.decodeFile(picFile.absolutePath, options)
                if (bitmap != null) {
                    canvasBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                }
                drawCanvas = Canvas(canvasBitmap!!)
            } else {
                drawCanvas = Canvas(canvasBitmap!!)
                drawCanvas!!.drawColor(Color.WHITE)
            }
            invalidate()
            saveImage()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        drawPaint.color = if(isUsingEraser) Color.WHITE else Color.rgb(colorR, colorG, colorB)
        drawPaint.strokeWidth = (if(isUsingEraser) eraserLineWidth else paintLineWidth *
                resources.displayMetrics.density.toInt()).toFloat()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(x, y)
                drawPath.lineTo(x + 0.1f, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                drawCanvas!!.drawPath(drawPath, drawPaint)
                drawPath.reset()
                invalidate()
                saveImage()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)
        canvas.drawPath(drawPath, drawPaint)
    }
}