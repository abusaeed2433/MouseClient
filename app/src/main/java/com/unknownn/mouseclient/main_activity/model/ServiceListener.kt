package com.unknownn.mouseclient.main_activity.model

interface ServiceListener {
    fun onConnected()
    fun onMessage(message:String?)
    fun onError(error:String?)
    fun onTextReceived(text:String)
    fun onFileStarted(fileName:String)
    fun onFileSizeReceived(sizeInByte:Int)
    fun onFileProgressChanged(byteReceived:Int)
    fun onFileReceived()
}
