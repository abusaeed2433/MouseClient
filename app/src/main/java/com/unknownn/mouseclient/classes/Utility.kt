package com.unknownn.mouseclient.classes

import android.app.Activity
import android.widget.Toast


private var mToast: Toast? = null
fun showSafeToast(activity:Activity, message:String){
    try{
        if(mToast != null) mToast!!.cancel()

        mToast = Toast.makeText(activity,message,Toast.LENGTH_SHORT)
        mToast!!.show()
    }catch (ignored:Exception){}
}
