package com.unknownn.mouseclient.classes

interface DataListener {
    fun onMessageReceived(command:SharedCommand)
}