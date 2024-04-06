package com.unknownn.mouseclient.classes

import android.content.Context
import android.content.SharedPreferences

class DataSaver(val context:Context) {

    fun getPreviousIp():String?{
        val sp: SharedPreferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE)
        return sp.getString("saved_ip", null)
    }

    fun savePreviousIp(ip:String?){
        val sp: SharedPreferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString("saved_ip", ip)
        editor.apply()
    }

}
