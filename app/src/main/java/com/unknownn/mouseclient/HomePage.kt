package com.unknownn.mouseclient

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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
        setBackListener()

    }

    private fun setupTouchPad(){
        val touchPad = binding.myTouchPad
        touchPad.touchPadListener = object : MyTouchPad.TouchPadListener{
            override fun onMoveRequest(dx: Float, dy: Float) {
                //println("Sent requested with $dx and $dy")
                val command = SharedCommand(SharedCommand.Type.MOVE,dx,dy)
                sendData(command)
            }

        }
    }

    private fun sendData(message: SharedCommand) {
        MainActivity.socketClient.sendMessage(message)
    }

    private fun setBackListener(){
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedDispatcher.onBackPressed()
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,android.R.anim.fade_in, android.R.anim.fade_out)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,android.R.anim.fade_in, android.R.anim.fade_out)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
