package com.unknownn.mouseclient.classes

import java.io.Serializable

class SharedCommand(val type:Type, vararg args:Float):Serializable {

    var points: Array<Float>
    init {
        points = args.toTypedArray()
    }

    enum class Type{
        MOVE, // dx, dy
        SINGLE_CLICK, // x,y
        DOUBLE_CLICK, // x,y
        SCROLL, // dx, dy
        CLICK_AND_SCROLL // dx, dy
    }
}
