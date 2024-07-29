package com.unknownn.mouseclient.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ServiceCompat
import com.unknownn.mouseclient.main_activity.view.MainActivity
import com.unknownn.mouseclient.notification.NotificationsHelper
import com.unknownn.mouseclient.service.model.ClipManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MyForeGroundService:Service() {

    companion object {
        const val SERVICE_ID = 1
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        startCommunication()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForegroundService()
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        return super.stopService(name)
    }

    private fun startAsForegroundService() {
        NotificationsHelper.createNotificationChannel(this)
        val notification = NotificationsHelper.buildNotification(applicationContext)

        ServiceCompat.startForeground(this, SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun startCommunication() {
        val service:ExecutorService = Executors.newSingleThreadExecutor()

        service.submit{
            if(MainActivity.socketClient == null){
                Toast.makeText(applicationContext, "socket is null", Toast.LENGTH_SHORT).show()
                service.shutdown()
                return@submit
            }

            val clipManager = ClipManager(applicationContext)
//            MainActivity.socketClient.observeClipBoard()
        }
    }

}
