package com.unknownn.mouseclient.mouse_controller.model

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.min


class MyTouchPad : View {

    companion object {
        const val MAX_TIMEOUT = 200L // double of tap timeout
        const val SINGLE_CLICK_TIMEOUT = 150L
        const val TOLERANCE_DISTANCE = 2f // 12f was first used
        const val COMMAND_SEND_INTERVAL = 20L
        var staticMinLength = 12f
    }

    private val service = Executors.newFixedThreadPool(2)

    private val currentPath = Path()
    private val paintBrush = Paint()
    private val separatorBrush = Paint()
    private val scrollBrush = Paint()
    private val mHandler = Handler(Looper.getMainLooper())

    private var lastDrawTime = 0L
    private var lastDownTime = 0L
    private var prevClickDownTime = 0L

    private var isFingerLifted = true

    private var prevPosition = Pair(0f,0f)
    private var curPosition = Pair(0f,0f)

    var touchPadListener: TouchPadListener? = null

    private val separatorPath = Path()
    private val scrollPath = Path()

    private var isInScrollArea = false

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


        var count = 1
        var isSecondTime = false

        // mouse move
        service.execute{
            while (true) {
                count = (count+1) % 4
                Thread.sleep(COMMAND_SEND_INTERVAL)

                if(count % 4 == 0) {
                    val timeSpent = System.currentTimeMillis() - lastDrawTime

                    if (timeSpent > MAX_TIMEOUT && isFingerLifted) {
                        mHandler.post {
                            currentPath.reset()
                            invalidate()
                        }
                    }
                }

                if(isFingerLifted) continue

                if(prevPosition.first == 0f){
                    prevPosition = curPosition
                    continue
                }

                val dx = (curPosition.first - prevPosition.first) * widthMultiplier
                val dy = (curPosition.second - prevPosition.second) * heightMultiplier

                prevPosition = curPosition

                if(abs(dx) < TOLERANCE_DISTANCE && abs(dy) < TOLERANCE_DISTANCE){
                    continue
                }

                mHandler.post {
                    if(isInScrollArea){
                        setupScrollPath(dy)
                        if(isSecondTime) {
                            isSecondTime = false
                            touchPadListener?.onScrollRequest(dy/2)
                        }
                        else{
                            isSecondTime = true
                        }
                    }
                    else {
                        touchPadListener?.onMoveRequest(dx, dy)
                    }
                }
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
                lastDownTime = System.currentTimeMillis()
                currentPath.reset()
                if(x <= widthToUse) {
                    isInScrollArea = false
                    currentPath.moveTo(x, y)
                }
                else{
                    isInScrollArea = true
                }
                isFingerLifted = false
                true
            }

            MotionEvent.ACTION_MOVE -> {
                if(x <= widthToUse && !isInScrollArea) {
                    currentPath.lineTo(x, y)
                }
                invalidate()
                true
            }

            MotionEvent.ACTION_UP -> {
                if(x <= widthToUse && !isInScrollArea) {
                    currentPath.lineTo(x, y)
                }
                isFingerLifted = true
                prevPosition = Pair(0f,0f)
                invalidate()
                checkForClick()
                true
            }

            else -> false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(currentPath, paintBrush)
        canvas.drawPath(separatorPath,separatorBrush)
        canvas.drawPath(scrollPath,scrollBrush)
    }

    private fun checkForClick(){
        val curTime = System.currentTimeMillis()

        // not any click
        if( (curTime - lastDownTime) > SINGLE_CLICK_TIMEOUT ) return

        touchPadListener?.onSingleClickRequest()
        prevClickDownTime = lastDownTime
    }

    private fun setBrushProperty() {

        val list = listOf(paintBrush,separatorBrush, scrollBrush)

        for(brush in list) {
            brush.isAntiAlias = true
            brush.color = Color.BLACK
            brush.style = Paint.Style.STROKE
            brush.strokeCap = Paint.Cap.ROUND
            brush.strokeJoin = Paint.Join.ROUND
            brush.strokeWidth = 8f
        }
        separatorBrush.color = Color.RED
        separatorBrush.strokeWidth = 4f

        scrollBrush.strokeWidth = 2f
        scrollBrush.color = Color.GRAY
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


    private var widthMultiplier = 1f
    private var heightMultiplier = 1f
    private var widthToUse = width * 0.8f
    fun setScreenInfo(serverWidth:Float, serverHeight:Float) {
        post {
            widthToUse = width * 0.8f
            val heightToUse = height * 0.95f

            widthMultiplier = serverWidth / widthToUse
            heightMultiplier = serverHeight / heightToUse

            val heightPad = (height - heightToUse) / 2f

            // separator path
            separatorPath.moveTo(widthToUse, heightPad)
            separatorPath.lineTo(widthToUse, heightToUse + heightPad)
            setupScrollPath ()
        }
    }

    private fun setupScrollPath(dy:Float = 0f){

        scrollPath.reset()
        val percent = 6f // 6%
        val scrollBarWidth = width * (percent/100f)
        val x = width * ( 0.80f + (20-percent)/200)

        scrollPath.moveTo(x,0f)
        scrollPath.lineTo(x,width.toFloat())

        scrollPath.moveTo(x+scrollBarWidth,width.toFloat())
        scrollPath.lineTo(x+scrollBarWidth,0f)

        val noOfLines = 50
        val lineGap = width / noOfLines.toFloat()

        val lineGapServer = lineGap * heightMultiplier
        val offset = min(dy, lineGapServer)

        for(i in 0 until noOfLines){
            scrollPath.moveTo(x,(lineGap * i)+offset)
            scrollPath.lineTo(x+scrollBarWidth,(lineGap * i)+offset)
        }
    }

    interface TouchPadListener{
        fun onMoveRequest(dx:Float,dy:Float)
        fun onScrollRequest(dy:Float)
        fun onSingleClickRequest()
        fun onMoveThenClick(x:Float,y:Float)
        fun onDoubleClickRequest()
    }

}
