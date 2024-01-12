package com.unknownn.mouseclient

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.unknownn.mouseclient.classes.DataListener
import com.unknownn.mouseclient.classes.MyTouchPad
import com.unknownn.mouseclient.classes.ScreenShareListener
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

            override fun onScrollRequest(dy: Float) {
                val command = SharedCommand(SharedCommand.Type.SCROLL,dy)
                sendData(command)
            }
        }

        MainActivity.socketClient?.setScreenShareListener(object : ScreenShareListener{
            override fun onCommandReceived(byteArray: ByteArray) { }
            override fun onScreenSizeReceived(width: Int, height: Int) {
                binding.myTouchPad.setScreenInfo(width.toFloat(),height.toFloat())
                MainActivity.socketClient?.clearScreenShareListener()
            }
        })

        MainActivity.socketClient?.setDataListener(object: DataListener{
            override fun onMessageReceived(command: SharedCommand) {

            }
        })

        MainActivity.socketClient?.requestScreenInfo()

        binding.myTouchPad.setScreenInfo(1300f,720f)
    }

    private fun sendData(message: SharedCommand) {
        if(message.type == SharedCommand.Type.MOVE) {
            binding.tvTouchInfo.text = getString(
                com.unknownn.mouseclient.R.string.action_type_values_ph,
                message.type.name, message.points[0], message.points[1]
            )
        }
        else{
            binding.tvTouchInfo.text = message.type.name
        }
        MainActivity.socketClient?.sendMessage(message)
    }

    private fun setClickListener(){
        
        binding.ivScreenShare.setOnClickListener{
            startActivity(Intent(this@HomePage, ScreenShareActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
