package com.unknownn.mouseclient.classes

import com.google.gson.Gson
import com.unknownn.mouseclient.main_activity.model.ServiceListener
import com.unknownn.mouseclient.mouse_controller.model.DataListener
import com.unknownn.mouseclient.mouse_controller.model.SharedCommand
import com.unknownn.mouseclient.screen_share_activity.model.ScreenShareListener
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.math.min

class WebSocketClient private constructor(
    private val host: String,
    private val port: Int,
    private val socketListener: SocketListener)
{
    private var outputStream: DataOutputStream? = null
    private val service: ExecutorService = Executors.newSingleThreadExecutor()
    private val gson = Gson()

    private var dataListener: DataListener? = null
    private var screenShareListener: ScreenShareListener? = null
    private var textAndFileListener: TextAndFileListener? = null
    private var serviceListener: ServiceListener? = null

    var isSocketRunning: Boolean = false

    private var inputStream: DataInputStream? = null


    fun createManualClient() {
        val service = Executors.newFixedThreadPool(2)
        service.execute {
            while (true) {
                try {
                    val soc = Socket(host, port)

                    println("Websocket reading output stream")

                    outputStream = DataOutputStream(soc.getOutputStream())
                    inputStream = DataInputStream(soc.getInputStream())

                    isSocketRunning = true
                    socketListener.onConnected()
                    break
                } catch (e: Exception) {
                    println("Creating client error: " + e.message)
                    try {
                        Thread.sleep(3000)
                    } catch (ignored: InterruptedException) {
                    }
                }
            }
            while (true) {
                try {
                    val messageID = inputStream!!.readInt()

                    when (messageID) {
                        Type.CLIP_TEXT.id -> {
                            val bytes = readBytes(inputStream)
                            textAndFileListener?.onTextReceived(bytes)
                            serviceListener?.onTextReceived( String(bytes, StandardCharsets.UTF_8) )
                        }
                        Type.SCREEN_INFO.id -> {
                            val bytes = readBytes(inputStream)
                            val str =
                                String(bytes, StandardCharsets.UTF_8)

                            println("Received screen info: $str")
                            val regex = "(\\d+),(\\d+)"
                            val pattern =
                                Pattern.compile(regex)
                            val matcher = pattern.matcher(str)

                            val w = matcher.group(1)
                            val h = matcher.group(2)

                            if (w == null || h == null) return@execute

                            val width = w.toInt()
                            val height = h.toInt()

                            screenShareListener?.onScreenSizeReceived(width, height)
                            serviceListener?.onMessage("Received screen size")
                        }
                        Type.SCREEN_SHARE.id -> {
                            val bytesImage = readBytes(inputStream)
                            screenShareListener?.onCommandReceived(bytesImage)
                        }
                        Type.FILE.id -> {
                            val bytesName = readBytes(inputStream)
                            serviceListener?.onFileStarted( String(bytesName, StandardCharsets.UTF_8) )

                            val bytesContent = readBytes(inputStream, showFileProgress = true) // file progress here

                            textAndFileListener?.onFileReceived(bytesName, bytesContent)
                            serviceListener?.onFileReceived()
                        }
                    }
                } catch (e: Exception) {
                    println("Error receiving: " + e.message)
                }
            }
        }
        service.shutdown()
    }

    @Throws(IOException::class)
    private fun readBytes(dataInputStream: DataInputStream?, showFileProgress:Boolean = false): ByteArray {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var bytesRead: Int
        var bytesReceived = 0

        dataInputStream!!.readInt() // start code
        val totalBytes = dataInputStream.readInt()

        if(showFileProgress) serviceListener?.onFileSizeReceived(totalBytes)

        while (bytesReceived < totalBytes) {
            val bytesLeft = totalBytes - bytesReceived
            val toRead = min(buffer.size.toDouble(), bytesLeft.toDouble()).toInt()

            bytesRead = dataInputStream.read(buffer, 0, toRead)
            if (bytesRead > 0) {
                baos.write(buffer, 0, bytesRead)
                bytesReceived += bytesRead
            }

            if(showFileProgress) serviceListener?.onFileProgressChanged(bytesReceived)
        }

        dataInputStream.readInt() // end code
        println("File end reached")
        if(showFileProgress) serviceListener?.onFileReceived()
        return baos.toByteArray()
    }

    fun setDataListener(dataListener: DataListener?) {
        this.dataListener = dataListener
    }

    fun setScreenShareListener(shareListener: ScreenShareListener?) {
        this.screenShareListener = shareListener
    }

    fun setTextAndFileListener(textAndFileListener: TextAndFileListener?) {
        this.textAndFileListener = textAndFileListener
    }

    fun setServiceListener(serviceListener: ServiceListener) {
        this.serviceListener = serviceListener
    }

    fun clearScreenShareListener() {
        this.screenShareListener = null
    }

    fun requestScreenInfo() {
        val command = SharedCommand(SharedCommand.Type.SCREEN_INFO_REQUEST, null)
        sendMessage(command)
    }

    fun requestScreenShare() {
        val command = SharedCommand(SharedCommand.Type.SCREEN_SHARE_START_REQUEST, null)
        sendMessage(command)
    }

    fun stopScreenShare() {
        val command = SharedCommand(SharedCommand.Type.SCREEN_SHARE_STOP_REQUEST, null)
        sendMessage(command)
    }

    @Throws(IOException::class)
    fun shareClipText(text: String) {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        sendBytes(Type.CLIP_TEXT.id, bytes)
    }

    @Throws(IOException::class)
    fun sendFile(fileName: String, bytes: ByteArray) {
        val bytesName = fileName.toByteArray(StandardCharsets.UTF_8)
        sendBytes(Type.FILE.id, bytesName, bytes)
    }

    @Suppress("unused")
    private fun interpretCommand(strCommand: String) {
        val command = gson.fromJson(strCommand, SharedCommand::class.java)
        dataListener?.onMessageReceived(command)
    }

    // order: BYTE_START_CODE SIZE [chunk1, chunk2, ...], BYTE_END_CODE
    private fun sendBytes(id: Int?, vararg allBytes: ByteArray) {
        service.execute {
            try {
                if (outputStream == null) return@execute

                if (id != null) {
                    outputStream!!.writeInt(id)
                }

                for (bytes in allBytes) {
                    outputStream!!.writeInt(BYTE_START_CODE)

                    val totalBytes = bytes.size
                    outputStream!!.writeInt(totalBytes)

                    var bytesSent = 0

                    while (bytesSent < totalBytes) {
                        val bytesLeft = totalBytes - bytesSent
                        val toSend = min(
                            CHUNK_SIZE.toDouble(),
                            bytesLeft.toDouble()
                        )
                            .toInt()
                        outputStream!!.write(bytes, bytesSent, toSend)
                        bytesSent += toSend
                        outputStream!!.flush()
                    }

                    outputStream!!.writeInt(BYTE_END_CODE)
                }
                outputStream!!.flush()
            } catch (e: IOException) {
                println("Error is: " + e.message)
                outputStream = null
            }
        }
    }

    fun sendMessage(command: SharedCommand?) {
        if (outputStream == null) return
        service.execute {
            try {
                val gson = Gson()
                val json = gson.toJson(command)

                outputStream!!.writeInt(Type.SHARED_COMMAND.id)

                outputStream!!.writeUTF(json)
            } catch (e: IOException) {
                println("Send command failed: " + e.localizedMessage)
            }
        }
    }

    internal enum class Type(val id: Int) {
        // 5 digits
        SCREEN_INFO(11111), SCREEN_SHARE(13571), CLIP_TEXT(55555),
        SHARED_COMMAND(66666), FILE(77777)
    }

    interface SocketListener {
        fun onConnected()
    }

    interface TextAndFileListener {
        fun onTextReceived(bytes: ByteArray)
        fun onFileReceived(bytesName: ByteArray, bytesContent: ByteArray)
    }

    companion object {
        private const val CHUNK_SIZE = 4096
        private const val BYTE_START_CODE = 5555
        private const val BYTE_END_CODE = 7777

        private var instance: WebSocketClient? = null
        fun getInstance(ip: String, port: Int, socketListener: SocketListener): WebSocketClient? {
            if (instance == null) {
                instance = WebSocketClient(ip, port, socketListener)
            }
            return instance
        }
    }
}
