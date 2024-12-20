package com.unknownn.mouseclient.mouse_controller.model

import java.io.Serializable

class SharedCommand(val type:Type, val text:String?, vararg args:Float): Serializable {

    var byteArrayOfImage:ByteArray? = null

    var points: Array<Float>
    init {
        points = args.toTypedArray()
    }

    enum class Type{
        MOVE, // dx, dy
        SINGLE_CLICK,
        MOVE_THEN_CLICK, // x,y -> new position
        DOUBLE_CLICK, // x,y
        SCROLL, // dx, dy
        CLICK_AND_SCROLL, // dx, dy
        SCREEN_INFO,
        SCREEN_INFO_REQUEST,
        SCREEN_SHARE_START_REQUEST,
        SCREEN_SHARE_STOP_REQUEST,
        CLIPBOARD_FROM_PHONE
    }
}
