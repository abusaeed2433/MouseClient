package com.unknownn.mouseclient.homepage.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.unknownn.mouseclient.MainActivity
import com.unknownn.mouseclient.R
import com.unknownn.mouseclient.homepage.model.DataListener
import com.unknownn.mouseclient.homepage.model.MyTouchPad
import com.unknownn.mouseclient.screen_share_activity.model.ScreenShareListener
import com.unknownn.mouseclient.homepage.model.SharedCommand

class HomePageViewModel(application: Application):AndroidViewModel(application) {
    private var application:Application

    private lateinit var touchListener: MyTouchPad.TouchPadListener
    private lateinit var screenShareListener: ScreenShareListener

    val screenSize:MutableLiveData<Pair<Float,Float>?> = MutableLiveData(null)
    val showMessage:MutableLiveData<String?> = MutableLiveData(null)

    init {
        this.application = application
        initTouchListeners()
    }

    fun getTouchListener(): MyTouchPad.TouchPadListener{ return touchListener }

    fun requestScreenInfo(){
        MainActivity.socketClient?.requestScreenInfo()
    }

    private fun sendData(message: SharedCommand) {
        if(message.type == SharedCommand.Type.MOVE) {
            showMessage.postValue(
                application.getString(
                    R.string.action_type_values_ph,
                    message.type.name, message.points[0], message.points[1]
                )
            )
        }
        else{
            showMessage.postValue(message.type.name)

        }
        MainActivity.socketClient?.sendMessage(message)
    }

    fun setupScreenShareListener(){
        MainActivity.socketClient?.setScreenShareListener( screenShareListener )
    }

    private fun initTouchListeners(){
        MainActivity.socketClient?.setDataListener(object: DataListener {
            override fun onMessageReceived(command: SharedCommand) {

            }
        })

        screenShareListener = object : ScreenShareListener {
            override fun onCommandReceived(byteArray: ByteArray) { }
            override fun onScreenSizeReceived(width: Int, height: Int) {
                screenSize.postValue( Pair(width.toFloat(), height.toFloat()) )
                MainActivity.socketClient?.clearScreenShareListener()
            }
        }

        touchListener = object : MyTouchPad.TouchPadListener{
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
    }

}
