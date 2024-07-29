package com.unknownn.mouseclient.main_activity.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.unknownn.mouseclient.classes.DataSaver
import com.unknownn.mouseclient.classes.WebSocketClient
import com.unknownn.mouseclient.main_activity.view.MainActivity

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
        startWebsocketClient(ip)
    }

    private fun startWebsocketClient(ip: String) {
        if (MainActivity.socketClient != null) return

        MainActivity.socketClient = WebSocketClient(ip, 4275) {
            Handler(Looper.getMainLooper()).post {
                activitySwitch.value = true
            }
        }
    }

    private fun getDataSaver(): DataSaver {
        if (dataSaver == null) {
            dataSaver = DataSaver(application)
        }
        return dataSaver!!
    }

}
