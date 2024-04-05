package com.unknownn.mouseclient.homepage.model

interface DataListener {
    fun onMessageReceived(command: SharedCommand)
}