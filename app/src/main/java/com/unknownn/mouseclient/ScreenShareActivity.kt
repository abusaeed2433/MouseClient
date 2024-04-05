package com.unknownn.mouseclient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.unknownn.mouseclient.classes.MyImagePlotter
import com.unknownn.mouseclient.homepage.model.ScreenShareListener
import com.unknownn.mouseclient.classes.showSafeToast
import com.unknownn.mouseclient.databinding.ActivityScreenShareBinding
import java.lang.Thread.sleep
import java.util.concurrent.Executors


class ScreenShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreenShareBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeConnection()
        setListener()
        startFrameUpdater()

/*        Handler(Looper.getMainLooper()).postDelayed({
            initImagePlotter(1200f,720f)
        }, 2000)*/
    }

    private fun initializeConnection(){
        val mHandler = Handler(Looper.getMainLooper())

        MainActivity.socketClient?.setScreenShareListener(object : ScreenShareListener {
            override fun onCommandReceived(byteArray: ByteArray) {
                mHandler.post{
                    updateFrame(byteArray)
                }
            }

            override fun onScreenSizeReceived(width: Int, height: Int) {
                initImagePlotter(width.toFloat(),height.toFloat())
                println("screen dimension received $width & $height")
                MainActivity.socketClient?.requestScreenShare()
            }
        })
        MainActivity.socketClient?.requestScreenInfo()
    }

    private fun setListener(){
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MainActivity.socketClient?.stopScreenShare()
                interpolatorService.shutdownNow()
                frameUpdaterService.shutdownNow()
                stopFrameHandler = true
                finish()
            }
        })
    }

    private fun initImagePlotter(width:Float, height:Float){
        //binding.myImagePlotter.setImageResolution(80,60)

        binding.myImagePlotter.plotListener = object : MyImagePlotter.ImagePlotListener{
            override fun onMessageFound(message: String) {
                showSafeToast(this@ScreenShareActivity,message)
            }
        }

        binding.myImagePlotter.setScreenInfo(
            width - 0.1f*width,
            height - 0.1f*height
        )
    }

    private var frameCounter = 0
    private var fpsStartTime = 0L
    private var fpsEndTime = 0L

    companion object{
        const val FPS_INTERVAL = 5_000 // 5 s
        const val FRAME_QUEUE_SIZE = 57
    }

    var frameReceived = 0
    private fun updateFrame(imageBytes:ByteArray){
        synchronized(this) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            fpsEndTime = System.currentTimeMillis()
            frameReceived++

            val strReceived = "Received: $frameReceived"
            binding.tvReceived.text = strReceived

            if ((fpsEndTime - fpsStartTime) >= FPS_INTERVAL) {

                val fps = (frameCounter / ((fpsEndTime - fpsStartTime) / 1000.0))
                val strFps = "FPS:${String.format("%.2f", fps)}"
                binding.tvFps.text = strFps

                println("Fps calculate with counter $frameCounter")
                frameCounter = 0
                fpsStartTime = System.currentTimeMillis()
            }

            if (bitmap == null) return
            //interpolateAndUpdate(prevBitmap,bitmap)
            updateFrame(bitmap)
            prevBitmap = bitmap
        }
    }


    private val frameQueue = arrayOfNulls<Bitmap?>(FRAME_QUEUE_SIZE)
    private var curFrameIndex = 0

    private val frameUpdaterService = Executors.newSingleThreadExecutor()
    private var stopFrameHandler = false

    private val mHandler = Handler(Looper.getMainLooper())
    private fun startFrameUpdater(){
        frameUpdaterService.execute {
            var sleepTime: Long

            while (true){
                if(stopFrameHandler) break

                if( frameQueue[curFrameIndex] == null ){
                    sleepTime = 30
                }
                else{
                    val bitmap = frameQueue[curFrameIndex]!!
                    frameQueue[curFrameIndex] = null
                    curFrameIndex = (curFrameIndex + 1) % FRAME_QUEUE_SIZE

                    println("Frame index $curFrameIndex")

                    updateFrame(bitmap)
                    sleepTime = 40
                }

                try {
                    sleep(sleepTime)
                }catch (ignored:InterruptedException){}
            }
        }
    }

    private fun updateFrame(bitmap:Bitmap){
        mHandler.post {
            frameCounter++
            binding.myImagePlotter.updateFrame(bitmap)
        }
    }

    private val noOfFramesBetween = 2
    private val interpolatorService = Executors.newFixedThreadPool(FRAME_QUEUE_SIZE)
    private var prevBitmap:Bitmap? = null
    private var isRunning = false
    private var frameIndex = 0
    private var locker = Any()

    private fun interpolateAndUpdate(tempPrevBitmap: Bitmap?, curBitmap: Bitmap) {
        if(interpolatorService.isShutdown) return

        interpolatorService.execute{
            println("Frame submitted for interpolation")
            if(stopFrameHandler) return@execute

            val started = System.currentTimeMillis()

            var prevBitmap = tempPrevBitmap ?: curBitmap

            var myIndex:Int
            synchronized(locker){
                myIndex = frameIndex
                frameIndex = (frameIndex +  noOfFramesBetween + 1) % FRAME_QUEUE_SIZE
            }
            // myIndex, myIndex+1, myIndex+2

            val width: Int = prevBitmap.width
            val height: Int = curBitmap.height
            val pixels1 = IntArray(width * height)
            val pixels2 = IntArray(width * height)

            prevBitmap.getPixels(pixels1, 0, width, 0, 0, width, height)
            curBitmap.getPixels(pixels2, 0, width, 0, 0, width, height)

            val interpolatedPixels = IntArray(width * height)

            fun getIntermediateFrame(fraction:Float):Bitmap {
                for (i in pixels1.indices) {
                    val color1 = pixels1[i]
                    val color2 = pixels2[i]

                    val red: Int = lerp(Color.red(color1), Color.red(color2), fraction)
                    val green: Int = lerp(Color.green(color1), Color.green(color2), fraction)
                    val blue: Int = lerp(Color.blue(color1), Color.blue(color2), fraction)
                    interpolatedPixels[i] = Color.rgb(red, green, blue)
                }

                val bitmap = Bitmap.createBitmap(width, height, prevBitmap.config)
                bitmap.setPixels(interpolatedPixels, 0, width, 0, 0, width, height)
                return bitmap
            }

            var fraction = 0f
            for (i in 0 until noOfFramesBetween) { // 2
                fraction += 0.3f
                val bitmap = getIntermediateFrame(fraction)
                prevBitmap = bitmap
                frameQueue[myIndex++] = bitmap
            }
            frameQueue[myIndex++] = curBitmap
            val ended = System.currentTimeMillis()
            println( "Interpolation: Total: ${(ended-started)} ms" )
        }

    }

    fun lerp(startValue:Int, endValue:Int, fraction:Float):Int {
        return (startValue + fraction * (endValue - startValue)).toInt()
    }

}
