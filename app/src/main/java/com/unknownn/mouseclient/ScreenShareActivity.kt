package com.unknownn.mouseclient

import android.R.fraction
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.unknownn.mouseclient.classes.MyImagePlotter
import com.unknownn.mouseclient.classes.ScreenShareListener
import com.unknownn.mouseclient.classes.showSafeToast
import com.unknownn.mouseclient.databinding.ActivityScreenShareBinding
import java.util.concurrent.Executors


class ScreenShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreenShareBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeConnection()
        setListener()

/*        Handler(Looper.getMainLooper()).postDelayed({
            initImagePlotter(1200f,720f)
        }, 2000)*/
    }

    private fun initializeConnection(){
        val mHandler = Handler(Looper.getMainLooper())

        MainActivity.socketClient?.setScreenShareListener(object : ScreenShareListener{
            override fun onCommandReceived(byteArray: ByteArray) {
                mHandler.post{
                    updateFrame(byteArray)
                }
            }

            override fun onScreenSizeReceived(width: Int, height: Int) {
                initImagePlotter(width.toFloat(),height.toFloat())
            }
        })
        MainActivity.socketClient?.requestScreenInfo()
        MainActivity.socketClient?.requestScreenShare()
    }

    private fun setListener(){
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MainActivity.socketClient?.stopScreenShare()
                interpolator.shutdownNow()
                finish()
            }
        })
    }

    private fun initImagePlotter(width:Float, height:Float){
        binding.myImagePlotter.setImageResolution(80,60)

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
        const val FPS_INTERVAL = 10_000 // 10 s
    }
    private fun updateFrame(imageBytes:ByteArray){
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        if(frameCounter == 0){
            fpsStartTime = System.currentTimeMillis()
        }

        frameCounter++

        fpsEndTime = System.currentTimeMillis()

        if( (fpsEndTime - fpsStartTime) >= FPS_INTERVAL ){

            val fps = (frameCounter / ((fpsEndTime - fpsStartTime)/1000.0) )
            val strFps = "FPS:${String.format("%.2f",fps)}"
            binding.tvFps.text = strFps

            frameCounter = 0
        }

        if(bitmap == null) return
        interpolateAndUpdate(bitmap)
    }


    private val mHandler = Handler(Looper.getMainLooper())
    private fun updateFrame(bitmap: Bitmap){
        mHandler.post{
            binding.myImagePlotter.updateFrame(bitmap)
        }
    }

    private val noOfFramesBetween = 2
    //private val interpolator = Executors.newFixedThreadPool(noOfFramesBetween)
    private val interpolator = Executors.newSingleThreadExecutor()
    private var prevBitmap:Bitmap? = null
    private var isRunning = false
    private fun interpolateAndUpdate(curBitmap: Bitmap) {
        //if(isRunning) return

        if(interpolator.isShutdown) return

        //isRunning = true
        interpolator.execute{
            if (prevBitmap == null) {
                prevBitmap = curBitmap
                updateFrame(curBitmap)
            }
            else {

                val width: Int = prevBitmap!!.width
                val height: Int = curBitmap.height
                val pixels1 = IntArray(width * height)
                val pixels2 = IntArray(width * height)

                prevBitmap!!.getPixels(pixels1, 0, width, 0, 0, width, height)
                curBitmap.getPixels(pixels2, 0, width, 0, 0, width, height)

                val interpolatedPixels = IntArray(width * height)

                fun lerp(startValue:Int, endValue:Int, fraction:Float):Int {
                    return (startValue + fraction * (endValue - startValue)).toInt()
                }

                val fraction = 0.75f
                fun getIntermediateFrame(alpha:Float):Bitmap {
                    val started = System.currentTimeMillis()

                    for (i in pixels1.indices) {
                        val color1 = pixels1[i]
                        val color2 = pixels2[i]

                        val red: Int = lerp(Color.red(color1), Color.red(color2), fraction)
                        val green: Int = lerp(Color.green(color1), Color.green(color2), fraction)
                        val blue: Int = lerp(Color.blue(color1), Color.blue(color2), fraction)

                        /*val red = ((1 - alpha) * Color.red(color1) + alpha * Color.red(color2)).toInt()
                        val green = ((1 - alpha) * Color.green(color1) + alpha * Color.green(color2)).toInt()
                        val blue = ((1 - alpha) * Color.blue(color1) + alpha * Color.blue(color2)).toInt()*/
                        interpolatedPixels[i] = Color.rgb(red, green, blue)
                    }

                    val interpolatedBitmap = Bitmap.createBitmap(width, height, prevBitmap!!.config)
                    interpolatedBitmap.setPixels(interpolatedPixels, 0, width, 0, 0, width, height)

                    val ended = System.currentTimeMillis()
                    println( "Interpolation: Total: ${(ended-started)} ms" )
                    return interpolatedBitmap
                }

                var alpha = 0.6f
                for (i in 1 until 4) {
                    alpha += 0.1f
                    val bitmap = getIntermediateFrame(alpha)
                    updateFrame(bitmap)
                    println("Bitmap interpolated: $i")
                }
                prevBitmap!!.recycle()
                prevBitmap = curBitmap
            }
            //isRunning = false
        }

    }

}
