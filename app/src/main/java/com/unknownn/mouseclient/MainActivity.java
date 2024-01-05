package com.unknownn.mouseclient;

import static com.unknownn.mouseclient.classes.UtilityKt.showSafeToast;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.unknownn.mouseclient.classes.WebSocketClient;
import com.unknownn.mouseclient.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding = null;
    public static WebSocketClient socketClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonConnect.setOnClickListener(view -> connect());
    }

    private void connect(){

        String text = binding.buttonConnect.getText().toString().trim();

        if(text.isEmpty()){
            showSafeToast(this,"Please wait...");
            return;
        }

        binding.buttonConnect.setText("");
        binding.progressBar.setVisibility(View.VISIBLE);
        startWebsocketClient();
    }

    private void startWebsocketClient(){
        if(socketClient == null){
            socketClient = new WebSocketClient(() -> {
                startActivity(new Intent(MainActivity.this,HomePage.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

}
