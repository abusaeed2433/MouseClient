package com.unknownn.mouseclient.main_activity.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.unknownn.mouseclient.classes.DataSaver
import com.unknownn.mouseclient.classes.WebSocketClient
import com.unknownn.mouseclient.main_activity.view.MainActivity
import com.unknownn.mouseclient.service.MyForeGroundService

class MainActivityViewModel(private val application: Application): AndroidViewModel(application) {

    private var dataSaver: DataSaver? = null

    val ips:MutableLiveData<List<String>> = MutableLiveData(ArrayList())
    val buttonText:MutableLiveData<String> = MutableLiveData(null)
    val progressBar:MutableLiveData<Boolean> = MutableLiveData(null)
    val activitySwitch:MutableLiveData<Boolean> = MutableLiveData(null)


    init {
        readIps()
    }

    private fun readIps(){
        this.ips.value = getDataSaver().getPreviousIps()
    }

    fun connect(ip: String) {
        getDataSaver().saveIp(ip)
        readIps()

        buttonText.value = ""
        progressBar.value = true
        startMyService(ip)
    }

    private fun startMyService(ip:String) {
        if(MainActivity.socketClient != null && !MainActivity.socketClient.isSocketRunning) return

        MainActivity.socketClient = WebSocketClient.getInstance(ip, 4275, object : WebSocketClient.SocketListener{
            override fun onConnected() {
                Handler(Looper.getMainLooper()).post {
                    activitySwitch.value = true
                }
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(application, MyForeGroundService::class.java)
            intent.putExtra("ip", ip)
            application.startService(intent)
        }, 1200L)
    }

    private fun getDataSaver(): DataSaver {
        if (dataSaver == null) {
            dataSaver = DataSaver(application)
        }
        return dataSaver!!
    }

}
