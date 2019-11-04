package com.sec.chang.draw_v2

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.MotionEvent
import kotlinx.android.synthetic.main.activity_main.view.*

class RootView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val eventX = ev.x
        val eventY = ev.y
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (!(eventX >= mainFloatingButton.x && eventX <= mainFloatingButton.x + mainFloatingButton.width) || !(eventY >= mainFloatingButton.y && eventY <= mainFloatingButton.y + mainFloatingButton.height)) {
                mainFloatingButton.hide()
                paintFloatingButton.hide()
                eraserFloatingButton.hide()
            }
        } else if (ev.action == MotionEvent.ACTION_UP) {
            if (canvasView.isUsingEraser) {
                mainFloatingButton.setImageResource(R.drawable.ic_erase_white_24dp)
            } else {
                mainFloatingButton.setImageResource(R.drawable.ic_create_white_24dp)
            }
            mainFloatingButton.show()
        }
        onTouchEvent(ev)
        return false
    }
}
