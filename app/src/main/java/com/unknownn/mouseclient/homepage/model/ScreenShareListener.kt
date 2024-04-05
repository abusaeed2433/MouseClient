package com.unknownn.mouseclient.homepage.model

interface ScreenShareListener {
    fun onCommandReceived(byteArray:ByteArray)
    fun onScreenSizeReceived(width:Int, height:Int)
}
