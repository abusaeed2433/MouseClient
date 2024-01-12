package com.unknownn.mouseclient

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import com.unknownn.mouseclient.classes.MyImagePlotter
import com.unknownn.mouseclient.classes.ScreenShareListener
import com.unknownn.mouseclient.classes.showSafeToast
import com.unknownn.mouseclient.databinding.ActivityScreenShareBinding

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
        binding.myImagePlotter.updateFrame(bitmap)
    }

}
