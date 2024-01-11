package com.unknownn.mouseclient.classes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class MyImagePlotter : View {


    private val redPaintBrush = Paint()
    private val greenPaintBrush = Paint()
    private val bluePaintBrush = Paint()

    private var boundaryWidth = 0f
    private var boundaryHeight = 0f
    private var widthPad = 0f
    private var heightPad = 0f

    private val screenBoundary = Path()

    constructor(context: Context?) : super(context) {
        if (isInEditMode) return
        setBrushProperty()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        if (isInEditMode) return
        setBrushProperty()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {
        if (isInEditMode) return
        setBrushProperty()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //canvas.drawPath(currentPath, paintBrush)
        canvas.drawPath(screenBoundary,redPaintBrush)
    }

    private fun setBrushProperty() {
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

        println( "width: $width and height: $height" )

        invalidate()
    }

    private fun updateBoundary(width: Float, height: Float){

        screenBoundary.reset()
        screenBoundary.moveTo(widthPad,heightPad)
        screenBoundary.lineTo(width + widthPad,heightPad)

        screenBoundary.lineTo(width + widthPad,height + heightPad)

        screenBoundary.lineTo(widthPad,height + heightPad)

        screenBoundary.lineTo(widthPad,heightPad)
    }

}
