package com.unknownn.mouseclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.unknownn.mouseclient.classes.WebSocketClient;
import com.unknownn.mouseclient.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding = null;
    private WebSocketClient socketClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        startWebsocketClient();

        binding.tvHudai.setOnClickListener(view -> sendData(String.valueOf(System.currentTimeMillis())));
    }

    private void sendData(String message){
        //if(socketClient == null) return;
        System.out.println("Websocket button clicked");
        socketClient.sendMessage(message);
    }

    private void startWebsocketClient(){
        if(socketClient == null){
            socketClient = new WebSocketClient();
        }
    }
}
