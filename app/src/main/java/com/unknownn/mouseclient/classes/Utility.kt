package com.unknownn.mouseclient.classes

import android.app.Activity
import android.widget.Toast
import java.util.Locale


private var mToast: Toast? = null
fun showSafeToast(activity:Activity, message:String){
    try{
        if(mToast != null) mToast!!.cancel()

        mToast = Toast.makeText(activity,message,Toast.LENGTH_SHORT)
        mToast!!.show()
    }catch (ignored:Exception){}
}


fun formatTime(timeInSec:Int):String {
    if(timeInSec < 60){
        return "${timeInSec}s"
    }

    val timeInMin = timeInSec / 60
    if(timeInMin < 60){
        return "${timeInMin}m ${timeInSec%60}s"
    }

    val timeInHour = timeInMin / 60
    if(timeInHour < 10) {
        return "${timeInHour}h ${timeInMin % 60}m ${timeInSec % 60}s"
    }

    return "infinite"
}

fun formatSize(sizeInByte:Int):String {
    if(sizeInByte < 1024){
        return "<1KB"
    }

    val sizeInKB = sizeInByte / 1024 // KB

    if(sizeInKB < 1024){ // < 1MB
        return "$sizeInKB KB"
    }

    val sizeInMB = sizeInKB / 1024f
    val mb = String.format(Locale.US,"%.2f", sizeInMB)

    return "$mb MB"
}
