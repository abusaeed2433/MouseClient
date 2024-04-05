package com.unknownn.mouseclient.screen_share_activity.model

interface ScreenShareListener {
    fun onCommandReceived(byteArray:ByteArray)
    fun onScreenSizeReceived(width:Int, height:Int)
}
