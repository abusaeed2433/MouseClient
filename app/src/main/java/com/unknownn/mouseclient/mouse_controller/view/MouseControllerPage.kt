package com.unknownn.mouseclient.mouse_controller.view

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.unknownn.mouseclient.screen_share_activity.view.ScreenShareActivity
import com.unknownn.mouseclient.databinding.ActivityMouseControllerPageBinding
import com.unknownn.mouseclient.mouse_controller.viewmodel.HomePageViewModel

class MouseControllerPage : AppCompatActivity() {

    private lateinit var binding:ActivityMouseControllerPageBinding
    private val viewmodel:HomePageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMouseControllerPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupTouchPad()
        setClickListener()
    }

    private fun observeViewModel(){
        viewmodel.screenSize.observe(this){
            if(it == null) return@observe

            binding.myTouchPad.setScreenInfo(it.first,it.second)
        }
        viewmodel.showMessage.observe(this){
            binding.tvTouchInfo.text = it
        }

    }

    private fun setupTouchPad(){
        val touchPad = binding.myTouchPad
        touchPad.touchPadListener = viewmodel.getTouchListener()
        viewmodel.setupScreenShareListener()
        viewmodel.requestScreenInfo()
//        binding.myTouchPad.setScreenInfo(1300f,720f)
    }


    private fun setClickListener(){
        
        binding.ivScreenShare.setOnClickListener{
            startActivity(Intent(this@MouseControllerPage, ScreenShareActivity::class.java))
            @Suppress("Deprecation")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
