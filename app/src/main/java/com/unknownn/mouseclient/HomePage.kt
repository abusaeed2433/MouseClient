package com.unknownn.mouseclient

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.unknownn.mouseclient.classes.DataListener
import com.unknownn.mouseclient.classes.MyTouchPad
import com.unknownn.mouseclient.classes.SharedCommand
import com.unknownn.mouseclient.databinding.ActivityHomePageBinding

class HomePage : AppCompatActivity() {

    private lateinit var binding:ActivityHomePageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTouchPad()
        setClickListener()

    }

    private fun setupTouchPad(){
        val touchPad = binding.myTouchPad
        touchPad.touchPadListener = object : MyTouchPad.TouchPadListener{
            override fun onMoveRequest(dx: Float, dy: Float) {
                //println("Sent requested with $dx and $dy")
                val command = SharedCommand(SharedCommand.Type.MOVE,dx,dy)
                sendData(command)
            }

            override fun onSingleClickRequest() {
                val command = SharedCommand(SharedCommand.Type.SINGLE_CLICK)
                sendData(command)
            }

            override fun onDoubleClickRequest() {
                val command = SharedCommand(SharedCommand.Type.DOUBLE_CLICK)
                sendData(command)
            }
        }

        MainActivity.socketClient?.setDataListener(object: DataListener{
            override fun onMessageReceived(command: SharedCommand) {

            }
        })
    }

    private fun sendData(message: SharedCommand) {
        MainActivity.socketClient?.sendMessage(message)
    }

    private fun setClickListener(){
        
        binding.ivScreenShare.setOnClickListener{
            startActivity(Intent(this@HomePage, ScreenShareActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
