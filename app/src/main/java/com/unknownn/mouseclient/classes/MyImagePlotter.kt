package com.unknownn.mouseclient.classes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MyImagePlotter : View {

    private var scaleGestureDetector: ScaleGestureDetector? = null

    private val redPaintBrush = Paint()
    private val greenPaintBrush = Paint()
    private val bluePaintBrush = Paint()

    private var boundaryWidth = 0f
    private var boundaryHeight = 0f
    private var widthPad = 0f
    private var heightPad = 0f

    private val screenBoundary = Path()
    private val dummyPath = Path()

    constructor(context: Context?) : super(context) {
        if (isInEditMode) return
        initializeAll()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        if (isInEditMode) return
        initializeAll()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {
        if (isInEditMode) return
        initializeAll()
    }


    @SuppressLint("ClickableViewAccessibility")


    @Override
    public
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            scaleGestureDetector?.onTouchEvent(event)
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor, myPivotX, myPivotY)

        canvas.drawPath(screenBoundary,redPaintBrush)
        canvas.drawPath(dummyPath,greenPaintBrush)

        canvas.restore()
    }

    private fun initializeAll() {

        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

        val list = listOf(redPaintBrush,greenPaintBrush,bluePaintBrush)

        for(paintBrush in list){
            paintBrush.isAntiAlias = true
            paintBrush.style = Paint.Style.STROKE
            paintBrush.strokeCap = Paint.Cap.ROUND
            paintBrush.strokeJoin = Paint.Join.ROUND
            paintBrush.strokeWidth = 8f
        }

        redPaintBrush.color = Color.RED
        greenPaintBrush.color = Color.GREEN
        bluePaintBrush.color = Color.BLUE
    }

    fun setScreenInfo(width:Float, height:Float) {

        val padPercent = 0.05f

        val curWidth = (1 - padPercent) * getWidth()
        val widthExcluded = (getWidth() - curWidth)

        val curHeight = (1 - padPercent) * getHeight()
        val heightExcluded = (getHeight() - curHeight)

        val widthRatio = curWidth / width
        val heightRatio = curHeight / height

        val mn = min(widthRatio,heightRatio)

        this.boundaryWidth = mn * width
        this.boundaryHeight = mn * height

        widthPad = ( curWidth - this.boundaryWidth + widthExcluded) / 2
        heightPad = ( curHeight - this.boundaryHeight + heightExcluded) / 2

        updateBoundary(this.boundaryWidth,this.boundaryHeight)
    }

    private fun updateBoundary(width: Float, height: Float){

        screenBoundary.reset()
        screenBoundary.moveTo(widthPad,heightPad)
        screenBoundary.lineTo(width + widthPad,heightPad)

        screenBoundary.lineTo(width + widthPad,height + heightPad)

        screenBoundary.lineTo(widthPad,height + heightPad)

        screenBoundary.lineTo(widthPad,heightPad)

        dummyPath.reset()
        dummyPath.moveTo(10f,10f)
        dummyPath.lineTo(200f,200f)
        dummyPath.lineTo(200f,40f)
        dummyPath.lineTo(50f,320f)

        invalidate()
    }

    private var scaleFactor = 1f
    var myPivotX = 0f
    private var myPivotY = 0f
    private var scalingInProgress = false
    inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            scalingInProgress = true
            myPivotX = detector.focusX
            myPivotY = detector.focusY
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            scalingInProgress = false
            super.onScaleEnd(detector)
            invalidate()
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(1f, max(0.1f, min(scaleFactor, 5.0f)))
            invalidate()
            return true
        }
    }


}
