package com.unknownn.mouseclient.service.model

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.util.Log

class ClipManager(val context: Context) {

    init {
        observeClipBoard()
    }

    private fun observeClipBoard(){
        val manager: ClipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager? ?: return

        manager.addPrimaryClipChangedListener {
            if (manager.hasPrimaryClip() &&
                manager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ) {

                val item = manager.primaryClip?.getItemAt(0)
                val copiedText = item?.text.toString()

                Log.d("Clip changed", copiedText)
            }
        }
    }

}
