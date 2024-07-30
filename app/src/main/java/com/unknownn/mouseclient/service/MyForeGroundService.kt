package com.unknownn.mouseclient.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.unknownn.mouseclient.classes.formatSize
import com.unknownn.mouseclient.classes.formatTime
import com.unknownn.mouseclient.main_activity.model.ServiceListener
import com.unknownn.mouseclient.main_activity.view.MainActivity
import com.unknownn.mouseclient.notification.NotificationsHelper
import java.util.Arrays

class MyForeGroundService:Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val DEF_MESSAGE = "New message found"
        const val DEF_ERROR = "Something went wrong"
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForegroundService()
        startWebsocketClient()
        syncWithSocket()

        return START_STICKY
    }

    private fun startWebsocketClient() {
        if (MainActivity.socketClient.isSocketRunning) return

        MainActivity.socketClient.createManualClient()
    }

    private fun startAsForegroundService() {
        NotificationsHelper.createNotificationChannel(this)
        val notification = NotificationsHelper.buildNotification(
            applicationContext,
            "Running",
            "Something is happening"
        )

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun syncWithSocket(){
        var totalBytes:Int = 0
        var totalBytesStr:String = ""
        val timeArr = IntArray(5){0}

        var startIndex = 0
        var endIndex = 4
        var prevTime = 0L

        var prevPercent = 0

        MainActivity.socketClient.addServiceListener(object: ServiceListener{
            override fun onConnected() {
                NotificationsHelper.updateNotification(
                    this@MyForeGroundService,
                    message = "Connection is established"
                )
            }

            override fun onMessage(message: String?) {
                NotificationsHelper.updateNotification(
                    this@MyForeGroundService,
                    message = message ?: DEF_MESSAGE
                )
            }

            override fun onError(error: String?) {
                NotificationsHelper.updateNotification(
                    this@MyForeGroundService,
                    message = error ?: DEF_ERROR
                )
            }

            override fun onTextReceived(text: String) {
                NotificationsHelper.updateNotification(
                    this@MyForeGroundService,
                    message = "A text received from laptop"
                )
            }

            override fun onFileStarted(fileName: String) {
                NotificationsHelper.updateNotification(
                    this@MyForeGroundService,
                    title = fileName,
                    message = "Started receiving $fileName"
                )
            }

            override fun onFileSizeReceived(sizeInByte: Int) {
                totalBytes = sizeInByte
                totalBytesStr = formatSize(sizeInByte)

                prevTime = System.currentTimeMillis()
            }

            override fun onFileProgressChanged(byteReceived: Int) {
                val formatted = formatSize(byteReceived)
                val percent = (100*byteReceived) / totalBytes

                //println("Byte received: ${byteReceived}, total: ${totalBytes}, percent: $percent")

                val curTime = System.currentTimeMillis()
                if(curTime - prevTime > 1000L){ // increase index since > 1s
                    startIndex = (startIndex+1) % timeArr.size
                    endIndex = (endIndex+1) % timeArr.size
                    prevTime = curTime
                }

                timeArr[endIndex] = byteReceived

                if(percent - prevPercent < 3) return
                prevPercent = percent

                val receivedIn5S = (timeArr[endIndex] - timeArr[startIndex])
                val speed = if(receivedIn5S > 0) (receivedIn5S / 5) else 1

                println("Updating notification, received: ${receivedIn5S}, ${timeArr.contentToString()}")

                val timeNeeded = (totalBytes - byteReceived) / speed
                val timeStr = formatTime(timeNeeded)

                NotificationsHelper.updateNotification(
                    this@MyForeGroundService,
                    title = "${percent}% received",
                    message = "Received $formatted / $totalBytesStr. Time needed: $timeStr. Speed: ${speed/1024}KBps"
                )
            }

            override fun onFileReceived() {
                totalBytes = 0
                totalBytesStr = ""
                Arrays.fill(timeArr,0)
                startIndex = 0
                prevTime = 0L
                prevPercent = 0
                endIndex = 4

                NotificationsHelper.updateNotification(
                    this@MyForeGroundService,
                    title = "Running",
                    message = "File saved to Downloads folder"
                )
            }

        })
    }

    override fun stopService(name: Intent?): Boolean {
        return super.stopService(name)
    }

}
