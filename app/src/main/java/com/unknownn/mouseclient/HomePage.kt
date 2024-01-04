package com.unknownn.mouseclient

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        setBackListener()
    }

    private fun setBackListener(){
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedDispatcher.onBackPressed()
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,android.R.anim.fade_in, android.R.anim.fade_out)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,android.R.anim.fade_in, android.R.anim.fade_out)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
