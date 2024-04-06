package com.unknownn.mouseclient.screen_share_activity.model

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import com.unknownn.mouseclient.homepage.model.MyTouchPad
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MyImagePlotter : View {

    private var scaleGestureDetector: ScaleGestureDetector? = null

    private val redPaintBrush = Paint()
    private val greenPaintBrush = Paint()
    private val bluePaintBrush = Paint()
    private val whitePaintBrush = Paint()
    private val grayPaintBrush = Paint()
    private val textPaintBrush = Paint()

    private var boundaryWidth = 0f
    private var boundaryHeight = 0f
    private var widthPad = 0f
    private var heightPad = 0f

    private val screenBoundary = Path()

    var plotListener : ImagePlotListener? = null

    private var isDrawingRequested = false
    private var fullRect:RectF? = null
    private var curBitmap:Bitmap? = null

    // for touch
    private var lastDrawTime = 0L
    private var lastDownTime = 0L
    private var prevClickDownTime = 0L

    private var isFingerLifted = true

    private var prevPosition = Pair(0f,0f)
    private var clickPrevPosition = Pair(0f,0f)
    private var curPosition = Pair(0f,0f)

    private val currentPath = Path()
    var touchPadListener: MyTouchPad.TouchPadListener? = null

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

    private fun initAll(){
        var count = 1
        var isSecondTime = false
        val service = Executors.newFixedThreadPool(1)
        val mHandler = Handler(Looper.getMainLooper())

        // mouse move
        service.execute{
            while (true) {
                count = (count+1) % 4
                Thread.sleep(MyTouchPad.COMMAND_SEND_INTERVAL)

                if(count % 4 == 0) {
                    val timeSpent = System.currentTimeMillis() - lastDrawTime

                    if (timeSpent > MyTouchPad.MAX_TIMEOUT && isFingerLifted) {
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

                if(abs(dx) < MyTouchPad.TOLERANCE_DISTANCE && abs(dy) < MyTouchPad.TOLERANCE_DISTANCE){
                    continue
                }

                mHandler.post {
                    if(isSecondTime) {
                        isSecondTime = false
                        touchPadListener?.onScrollRequest(dy/2)
                    }
                    else{
                        isSecondTime = true
                    }
                }
            }
        }

        service.shutdown()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x - widthPad
        val y = event.y - heightPad

        if(x < 0 || y < 0 || x > boundaryWidth || y > boundaryHeight) return false

        scaleGestureDetector?.onTouchEvent(event)

        curPosition = Pair(x,y)

        lastDrawTime = System.currentTimeMillis()

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastDownTime = System.currentTimeMillis()
                currentPath.reset()
                isFingerLifted = false
                true
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
                invalidate()
                true
            }

            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                isFingerLifted = true
                invalidate()
                checkForClick(x,y)
                prevPosition = Pair(0f,0f)
                true
            }

            else -> false
        }
    }

    private fun checkForClick(x:Float, y:Float) {
        val curTime = System.currentTimeMillis()

        // not any click
        if( (curTime - lastDownTime) > MyTouchPad.SINGLE_CLICK_TIMEOUT) return

        println(this.boundaryWidth)
        println(this.boundaryHeight)
        val serverX = x * widthMultiplier
        val serverY = y * heightMultiplier

        println("App position: $x,$y. Server: $serverX, $serverY")

        touchPadListener?.onMoveThenClick(serverX, serverY)
        //prevClickDownTime = lastDownTime
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.scale(scaleFactor, scaleFactor, myPivotX, myPivotY)

        canvas.drawPath(screenBoundary, grayPaintBrush)

//        for(x in widthPad.toInt()..boundaryWidth.toInt() step 100){
//            for(y in heightPad.toInt()..boundaryHeight.toInt() step 100){
//                canvas.drawCircle(x.toFloat(),y.toFloat(),2f,redPaintBrush)
//                canvas.drawText("$x,$y",x.toFloat(),y.toFloat(),textPaintBrush)
//                canvas.drawText("${((x-widthPad)*widthMultiplier).toInt()},${((y-heightPad)*heightMultiplier).toInt()}",x+1f,y+30f,textPaintBrush)
//            }
//        }

//        canvas.drawLine(0f,10f,widthPad,10f,redPaintBrush)
//        canvas.drawLine(0f,20f,100f,20f,redPaintBrush)
//        canvas.drawLine(0f,50f,width.toFloat(),50f,redPaintBrush)

        if(curBitmap != null && !curBitmap!!.isRecycled && fullRect != null) {
            canvas.drawBitmap(curBitmap!!,null, fullRect!!,null);
        }

    }

    private fun initializeAll() {

        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

        val list = listOf(redPaintBrush,greenPaintBrush,bluePaintBrush,whitePaintBrush, grayPaintBrush)

        for(paintBrush in list){
            paintBrush.isAntiAlias = true
            paintBrush.style = Paint.Style.FILL
            paintBrush.strokeCap = Paint.Cap.ROUND
            paintBrush.strokeJoin = Paint.Join.ROUND
            paintBrush.strokeWidth = 8f
        }

        redPaintBrush.color = Color.RED
        greenPaintBrush.color = Color.GREEN
        bluePaintBrush.color = Color.BLUE
        whitePaintBrush.color = Color.WHITE
        grayPaintBrush.color = Color.GRAY

        textPaintBrush.color = Color.BLACK;
        textPaintBrush.style = Paint.Style.FILL;
        textPaintBrush.textSize = 20f;

        initAll()

    }

    private var widthMultiplier = 1f
    private var heightMultiplier = 1f
    fun setScreenInfo(serverWidth:Float, serverHeight:Float) {
        post {
            val padPercentWidth = 0.05f
            val padPercentHeight = 0.1f

            val curWidth = (1 - padPercentWidth) * width
            val widthExcluded = (width - curWidth)

            val curHeight = (1 - padPercentHeight) * height
            val heightExcluded = (height - curHeight)

            val widthRatio = curWidth / serverWidth
            val heightRatio = curHeight / serverHeight

            val mn = min(widthRatio, heightRatio)

            this.boundaryWidth = mn * serverWidth
            this.boundaryHeight = mn * serverHeight

            widthMultiplier = serverWidth / this.boundaryWidth
            heightMultiplier = serverHeight / this.boundaryHeight

            widthPad = (curWidth - this.boundaryWidth + widthExcluded) / 2
            heightPad = (curHeight - this.boundaryHeight + heightExcluded) / 2

            updateBoundary(this.boundaryWidth, this.boundaryHeight)
            invalidate()
            //drawPixel()
        }
    }

    private fun updateBoundary(width: Float, height: Float){
        val extra = 20f

        screenBoundary.reset()
        screenBoundary.moveTo(widthPad-extra,heightPad-extra)
        screenBoundary.lineTo(width + widthPad+extra,heightPad-extra)

        screenBoundary.lineTo(width + widthPad+extra,height + heightPad+extra)

        screenBoundary.lineTo(widthPad-extra,height + heightPad+extra)

        screenBoundary.lineTo(widthPad-extra,heightPad-extra)

        fullRect = RectF(widthPad,heightPad,width+widthPad,height+heightPad)
        invalidate()
    }

    private var tempPrev:Bitmap? = null
    fun updateFrame(bitmap: Bitmap){
        if(scalingInProgress) {
            return
        }

        tempPrev = curBitmap
        curBitmap = bitmap
        //tempPrev?.recycle()

        isDrawingRequested = true
        invalidate()
    }

    interface ImagePlotListener{
        fun onMessageFound(message: String)
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
