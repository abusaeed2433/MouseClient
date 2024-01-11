package com.unknownn.mouseclient

import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.unknownn.mouseclient.classes.MyImagePlotter
import com.unknownn.mouseclient.classes.ScreenShareListener
import com.unknownn.mouseclient.classes.SharedCommand
import com.unknownn.mouseclient.classes.showSafeToast
import com.unknownn.mouseclient.databinding.ActivityScreenShareBinding

class ScreenShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreenShareBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListener()

        Handler(Looper.getMainLooper()).postDelayed({
            initImagePlotter(1200f,720f)
        }, 2000)
    }

    private fun setListener(){
        val mHandler = Handler(Looper.getMainLooper())

        MainActivity.socketClient?.setScreenShareListener(object : ScreenShareListener{
            override fun onCommandReceived(command: SharedCommand) {
                mHandler.post{
                    initImagePlotter(command.points[0],command.points[1])
                }
            }
        })
        MainActivity.socketClient?.requestScreenInfo()
    }

    private fun initImagePlotter(width:Float, height:Float){
        binding.myImagePlotter.setImageResolution(180,160)

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

}
