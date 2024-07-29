package com.unknownn.mouseclient.homepage.view

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.unknownn.mouseclient.MainActivity
import com.unknownn.mouseclient.classes.WebSocketClient.TextAndFileListener
import com.unknownn.mouseclient.classes.showSafeToast
import com.unknownn.mouseclient.databinding.ActivityHomePageBinding
import com.unknownn.mouseclient.service.MyForeGroundService
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets


class HomePage : AppCompatActivity() {

    private lateinit var binding:ActivityHomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkShareRequest()
        startMyService()
        setClickListener()
        addObserver()
    }

    private fun setClickListener(){
        binding.ivDownload.setOnClickListener{
            showSafeToast(this,"Not ready yet")
            MainActivity.socketClient?.shareClipText("sharing from phone")
        }

        binding.buttonExit.setOnClickListener{
            stopService( Intent(this, MyForeGroundService::class.java) )
        }
    }

    private fun startMyService(){
        Handler(Looper.getMainLooper()).postDelayed({
            val intent=Intent(this,MyForeGroundService::class.java)
            intent.putExtra("name","Geek for Geeks")
            startService(intent)
        },5000)
    }

    private fun checkShareRequest() {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                showSafeToast(this, it)
                MainActivity.socketClient?.shareClipText(it)
            }
            return
        }

        val bundle = intent.extras ?: return
        val dataUri = bundle[Intent.EXTRA_STREAM] as Uri? ?: return

        val nameWithExtension = DocumentFile.fromSingleUri(this, dataUri)?.name ?: return
        showSafeToast(this,nameWithExtension)

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

        MainActivity.socketClient.setTextAndFileListener(object : TextAndFileListener{
            override fun onTextReceived(bytes: ByteArray) {
                mHandler.post { binding.textView.text = String(bytes, StandardCharsets.UTF_8) }
            }

            override fun onFileReceived(bytesName: ByteArray, bytesContent: ByteArray) {
                val nameWithExtension = String(bytesName, StandardCharsets.UTF_8)
                saveFile(nameWithExtension, bytesContent)
            }
        })
    }

    private fun saveFile(name:String, bytes:ByteArray){
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        //values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)

        val fileUri: Uri = contentResolver.insert(collection,values) ?: return

        try {
            contentResolver.openOutputStream(fileUri).use { outputStream -> outputStream?.write(bytes) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
