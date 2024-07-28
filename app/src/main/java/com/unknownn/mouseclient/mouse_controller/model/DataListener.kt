package com.unknownn.mouseclient.mouse_controller.model

interface DataListener {
    fun onMessageReceived(command: SharedCommand)
}