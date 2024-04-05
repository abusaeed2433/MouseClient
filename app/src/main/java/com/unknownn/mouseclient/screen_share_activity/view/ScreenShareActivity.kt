package com.unknownn.mouseclient.screen_share_activity.view

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.unknownn.mouseclient.MainActivity
import com.unknownn.mouseclient.screen_share_activity.model.MyImagePlotter
import com.unknownn.mouseclient.classes.showSafeToast
import com.unknownn.mouseclient.databinding.ActivityScreenShareBinding
import com.unknownn.mouseclient.screen_share_activity.viewmodel.ScreenShareViewModel


class ScreenShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreenShareBinding
    private val viewModel:ScreenShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setBackPressListener()
        observeViewModel()
        setupViewModel()
    }

    private fun observeViewModel(){

        viewModel.tvReceivedLD.observe(this){
            binding.tvReceived.text = it
        }
        viewModel.tvFpsLD.observe(this){
            binding.tvFps.text = it
        }

        viewModel.processedFrameLD.observe(this){bitmap ->
            if(bitmap == null) return@observe
            binding.myImagePlotter.updateFrame(bitmap)
        }

        viewModel.screenSizeLD.observe(this){
            if( it == null) return@observe

            println("${it.first} - ${it.second}")
            initImagePlotter(it.first,it.second)
            viewModel.requestScreenShare()
        }
    }

    private fun setupViewModel(){
        viewModel.setupScreenShareListener()
        viewModel.startFrameUpdater()
        viewModel.requestScreenInfo()
    }

    private fun setBackPressListener(){
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MainActivity.socketClient?.stopScreenShare()
                viewModel.destroy()
                finish()
            }
        })
    }

    private fun initImagePlotter(width:Float, height:Float){
        binding.myImagePlotter.plotListener = object : MyImagePlotter.ImagePlotListener{
            override fun onMessageFound(message: String) {
                showSafeToast(this@ScreenShareActivity,message)
            }
        }

        binding.myImagePlotter.setScreenInfo(
            width - 0.1f*width,
            height - 0.1f*height
        )
    }

}
