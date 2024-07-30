package com.unknownn.mouseclient.homepage.view

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.unknownn.mouseclient.R
import com.unknownn.mouseclient.classes.WebSocketClient.TextAndFileListener
import com.unknownn.mouseclient.classes.formatSize
import com.unknownn.mouseclient.classes.formatTime
import com.unknownn.mouseclient.databinding.ActivityHomePageBinding
import com.unknownn.mouseclient.main_activity.model.ServiceListener
import com.unknownn.mouseclient.main_activity.view.MainActivity
import com.unknownn.mouseclient.service.MyForeGroundService
import com.unknownn.mouseclient.service.MyForeGroundService.Companion.DEF_ERROR
import com.unknownn.mouseclient.service.MyForeGroundService.Companion.DEF_MESSAGE
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Arrays


class HomePage : AppCompatActivity(), ServiceListener{

    private lateinit var binding:ActivityHomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        processPassedData()
        setClickListener()
        addObserver()
        MainActivity.socketClient.addServiceListener(this)
    }

    private fun setClickListener(){
        binding.buttonExit.setOnClickListener{
            stopService( Intent(this, MyForeGroundService::class.java) )
            MainActivity.socketClient?.addServiceListener(this@HomePage)
        }
    }

    private fun processPassedData() {
        val text = intent.getStringExtra("text")

        if(text != null){
            MainActivity.socketClient.shareClipText(text)
        }

        val strUri = intent.getStringExtra("data_uri") ?: return

        val dataUri:Uri = Uri.parse(strUri)

        val nameWithExtension = DocumentFile.fromSingleUri(this, dataUri)?.name ?: return

        try {
            val inputStream = contentResolver.openInputStream(dataUri) ?: return
            val bytes: ByteArray = getBytes(inputStream)
            inputStream.close()
            MainActivity.socketClient.sendFile(nameWithExtension, bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)

        var len: Int
        while ((inputStream.read(buffer).also { len = it }) != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private fun addObserver(){
        if(MainActivity.socketClient == null) return

        var outputStream:OutputStream? = null

        MainActivity.socketClient.setTextAndFileListener(object : TextAndFileListener{
            override fun onTextReceived(bytes: ByteArray) {
                mHandler.post { binding.textView.text = String(bytes, StandardCharsets.UTF_8) }
            }

            override fun onFileNameReceived(nameWithExtension: String) {
                outputStream = createOutputStream(nameWithExtension)
            }

            override fun onFilePartReceived( bytesContent: ByteArray) {
                println("Written to file: ${outputStream != null}")
                outputStream?.apply {
                    write(bytesContent)
                    flush()
                }
            }

            override fun onFileCompleted(lastBytes: ByteArray) {
                println("Written to file: ${outputStream != null}")
                outputStream?.apply {
                    write(lastBytes)
                    flush()
                }
                outputStream?.close()
            }
        })
    }

    private fun createOutputStream(name: String):OutputStream?{
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        //values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)

        val fileUri: Uri = contentResolver.insert(collection,values) ?: return null

        try {
            return contentResolver.openOutputStream(fileUri)
        } catch (_: IOException) { }
        return null
    }

    private var totalBytes = 0
    private var totalBytesStr = ""
    private val timeArr = IntArray(5){0}

    private var startIndex = 0
    private var endIndex = 4
    private var prevTime = 0L

    private var prevPercent = 0
    override fun onConnected() {
        setTransferText(getString(R.string.ph_only,"Connection is established"))
    }

    override fun onMessage(message: String?) {
        setTransferText(message ?: DEF_MESSAGE)
    }

    override fun onError(error: String?) {
        setTransferText(error ?: DEF_ERROR)
    }

    override fun onTextReceived(text: String) {
        setTransferText(getString(R.string.ph_only,"A text received from laptop"))
    }

    override fun onFileStarted(fileName: String) {
        setTransferText(getString(R.string.ph_only,"Started receiving $fileName"))
    }

    override fun onFileSizeReceived(sizeInByte: Int) {
        totalBytes = sizeInByte
        totalBytesStr = formatSize(sizeInByte)

        prevTime = System.currentTimeMillis()
    }

    override fun onFileProgressChanged(byteReceived: Int) {
        val formatted = formatSize(byteReceived)
        val percent = (100*byteReceived) / totalBytes

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

        val timeNeeded = (totalBytes - byteReceived) / speed
        val timeStr = formatTime(timeNeeded)

        setTransferText(getString(R.string.ph_only,"${percent}% received \nReceived $formatted / $totalBytesStr. Time needed: $timeStr. Speed: ${speed/1024}KBps"))
    }

    override fun onFileReceived() {
        totalBytes = 0
        totalBytesStr = ""
        Arrays.fill(timeArr,0)
        startIndex = 0
        prevTime = 0L
        prevPercent = 0
        endIndex = 4

        setTransferText(getString(R.string.ph_only,"File saved to Downloads folder"))
    }

    private fun setTransferText(text: String){
        mHandler.post {
            binding.tvTranferInfo.text = text
        }
    }

}
