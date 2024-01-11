package com.unknownn.mouseclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.unknownn.mouseclient.classes.ScreenShareListener
import com.unknownn.mouseclient.classes.SharedCommand

class ScreenShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_share)

        setListener()
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

    }

}