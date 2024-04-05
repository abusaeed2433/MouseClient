package com.unknownn.mouseclient.screen_share_activity.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.unknownn.mouseclient.MainActivity
import com.unknownn.mouseclient.homepage.model.MyTouchPad
import com.unknownn.mouseclient.homepage.viewmodel.HomePageViewModel
import com.unknownn.mouseclient.screen_share_activity.model.ScreenShareListener
import java.util.concurrent.Executors

class ScreenShareViewModel(application: Application):HomePageViewModel(application) {
    private var application:Application

    val screenSizeLD: MutableLiveData<Pair<Float, Float>?> = MutableLiveData(null)
    val tvReceivedLD: MutableLiveData<String?> = MutableLiveData(null)
    val tvFpsLD: MutableLiveData<String?> = MutableLiveData(null)
    val processedFrameLD: MutableLiveData<Bitmap?> = MutableLiveData(null)

    init {
        this.application = application
    }


    override fun setupScreenShareListener(){
        MainActivity.socketClient?.setScreenShareListener( object : ScreenShareListener {
            override fun onCommandReceived(byteArray: ByteArray) {
                updateFrame(byteArray)
            }
            override fun onScreenSizeReceived(width: Int, height: Int) {
                screenSizeLD.postValue( Pair(width.toFloat(), height.toFloat()) )
//                MainActivity.socketClient?.clearScreenShareListener()
            }
        } )
    }


    fun requestScreenShare(){
        MainActivity.socketClient?.requestScreenShare()
    }

    private var frameCounter = 0
    private var fpsStartTime = 0L
    private var fpsEndTime = 0L

    companion object{
        const val FPS_INTERVAL = 5_000 // 5 s
        const val FRAME_QUEUE_SIZE = 57
    }

    private var frameReceived = 0
    private val any:Any = Any()
    private fun updateFrame(imageBytes:ByteArray){
        synchronized(any) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            fpsEndTime = System.currentTimeMillis()
            frameReceived++

            val strReceived = "Received: $frameReceived"
            tvReceivedLD.postValue(strReceived)

            if ((fpsEndTime - fpsStartTime) >= FPS_INTERVAL) {

                val fps = (frameCounter / ((fpsEndTime - fpsStartTime) / 1000.0))
                val strFps = "FPS:${String.format("%.2f", fps)}"
                tvFpsLD.postValue(strFps)

                println("Fps calculate with counter $frameCounter")
                frameCounter = 0
                fpsStartTime = System.currentTimeMillis()
            }

            if (bitmap == null) return
            updateFrame(bitmap)
        }
    }


    private val frameQueue = arrayOfNulls<Bitmap?>(FRAME_QUEUE_SIZE)
    private var curFrameIndex = 0

    private val frameUpdaterService = Executors.newSingleThreadExecutor()
    private var stopFrameHandler = false

    fun startFrameUpdater(){
        frameUpdaterService.execute {
            var sleepTime: Long

            while (true){
                if(stopFrameHandler) break

                if( frameQueue[curFrameIndex] == null ){
                    sleepTime = 30
                }
                else{
                    val bitmap = frameQueue[curFrameIndex]!!
                    frameQueue[curFrameIndex] = null
                    curFrameIndex = (curFrameIndex + 1) % FRAME_QUEUE_SIZE

                    println("Frame index $curFrameIndex")

                    updateFrame(bitmap)
                    sleepTime = 40
                }

                try {
                    Thread.sleep(sleepTime)
                }catch (ignored:InterruptedException){}
            }
        }
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private fun updateFrame(bitmap: Bitmap){
        mHandler.post {
            frameCounter++
            processedFrameLD.postValue(bitmap)
        }
    }

    fun destroy(){
        frameUpdaterService.shutdownNow()
        stopFrameHandler = true
    }

}