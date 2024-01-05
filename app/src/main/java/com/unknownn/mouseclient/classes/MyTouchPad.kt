package com.unknownn.mouseclient.classes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MyTouchPad : View {

    companion object {
        private const val MAX_TIMEOUT = 200L // double of tap timeout
        private const val PATH_TIMEOUT = 1200L
        private const val TOLERANCE_DISTANCE = 12f
        var staticMinLength = 12f
    }

    private val currentPath = Path()
    private val paintBrush = Paint()
    private val mHandler = Handler(Looper.getMainLooper())
    private var lastDrawTime = 0L

    private var isFingerLifted = true

    private var prevPosition = Pair(0f,0f)
    private var curPosition = Pair(0f,0f)

    var touchPadListener:TouchPadListener? = null

    constructor(context: Context?) : super(context) {
        if (isInEditMode) return
        initAll()
        setBrushProperty()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        if (isInEditMode) return
        initAll()
        setBrushProperty()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {
        if (isInEditMode) return
        initAll()
        setBrushProperty()
    }

    private fun initAll(){
        val service = Executors.newSingleThreadExecutor()

        var count = 1

        service.execute{
            while (true) {
                count = (count+1) % 4
                Thread.sleep(50)

                if(prevPosition.first == 0f){
                    prevPosition = curPosition
                    continue
                }

                val dx = curPosition.first - prevPosition.first
                val dy = curPosition.second - prevPosition.second

                if(dx < TOLERANCE_DISTANCE && dy < TOLERANCE_DISTANCE){
                    continue
                }

                mHandler.post {
                    touchPadListener?.onMoveRequest(dx, dy)
                }

                if(count % 4 == 0) {
                    val timeSpent = System.currentTimeMillis() - lastDrawTime

                    if (timeSpent > MAX_TIMEOUT && isFingerLifted) {
                        mHandler.post {
                            currentPath.reset()
                            invalidate()
                        }
                    }
                }

                count = (count+1) % 4
                Thread.sleep(50)
            }
        }
        service.shutdown()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        curPosition = Pair(x,y)

        lastDrawTime = System.currentTimeMillis()

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.reset()
                currentPath.moveTo(x, y)
                isFingerLifted = false
                true
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
                invalidate()
                true
            }

            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x,y)
                isFingerLifted = true
                invalidate()
                true
            }

            else -> false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(currentPath, paintBrush)
    }

    private fun setBrushProperty() {
        paintBrush.isAntiAlias = true
        paintBrush.color = Color.BLACK
        paintBrush.style = Paint.Style.STROKE
        paintBrush.strokeCap = Paint.Cap.ROUND
        paintBrush.strokeJoin = Paint.Join.ROUND
        paintBrush.strokeWidth = 8f
    }

    fun setColor(color: Int) {
        paintBrush.color = color
    }

    fun setMinLength(minLength: Float) {
        staticMinLength = minLength
    }

    fun setBrushSize(brushSize: Float) {
        paintBrush.strokeWidth = brushSize
    }


    interface TouchPadListener{
        fun onMoveRequest(dx:Float,dy:Float)
    }

}

