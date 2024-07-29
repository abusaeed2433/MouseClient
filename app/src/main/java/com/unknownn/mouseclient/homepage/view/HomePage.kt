package com.unknownn.mouseclient.homepage.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.unknownn.mouseclient.MainActivity
import com.unknownn.mouseclient.classes.showSafeToast
import com.unknownn.mouseclient.databinding.ActivityHomePageBinding
import com.unknownn.mouseclient.service.MyForeGroundService
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream




class HomePage : AppCompatActivity() {

    private lateinit var binding:ActivityHomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkShareRequest()
        startMyService()
        setClickListener()
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

}
