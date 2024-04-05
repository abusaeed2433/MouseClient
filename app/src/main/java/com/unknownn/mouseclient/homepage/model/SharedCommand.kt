package com.unknownn.mouseclient.homepage.model

import java.io.Serializable

class SharedCommand(val type: Type, vararg args:Float): Serializable {

    var byteArrayOfImage:ByteArray? = null

    var points: Array<Float>
    init {
        points = args.toTypedArray()
    }

    enum class Type{
        MOVE, // dx, dy
        SINGLE_CLICK, // x,y
        DOUBLE_CLICK, // x,y
        SCROLL, // dx, dy
        CLICK_AND_SCROLL, // dx, dy
        SCREEN_INFO,
        SCREEN_INFO_REQUEST,
        SCREEN_SHARE_START_REQUEST,
        SCREEN_SHARE_STOP_REQUEST
    }
}
