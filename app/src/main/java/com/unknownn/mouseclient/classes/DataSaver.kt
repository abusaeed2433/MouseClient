package com.unknownn.mouseclient.classes

import android.content.Context
import android.content.SharedPreferences

class DataSaver(val context:Context) {

    fun getPreviousIps():List<String>{
        val sp: SharedPreferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE)

        val list:MutableList<String> = ArrayList()
        val pairs:MutableList<Pair<String,Int>> = ArrayList()

        val size = sp.getInt("size",0)
        for(i in 1..size){
            val ip:String = sp.getString("$i", null) ?: continue

            val count = sp.getInt("${ip}_count",0)

            pairs.add( Pair(ip,count) )

        }

        pairs.sortWith { p0, p1 -> p1.second.compareTo(p0.second) }

        for(pair in pairs){
            list.add( pair.first )
        }

        return list
    }

    fun saveIp(ip:String?){
        if(ip == null) return

        val sp: SharedPreferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE)

        val editor = sp.edit()
        if(!sp.contains("${ip}_count")){
            val size = sp.getInt("size",0)
            editor.putString("${size+1}",ip)
            editor.putInt("size",size+1)
        }

        val curCount = sp.getInt("${ip}_count",0)
        editor.putInt("${ip}_count", curCount+1)

        editor.apply()
    }

}
