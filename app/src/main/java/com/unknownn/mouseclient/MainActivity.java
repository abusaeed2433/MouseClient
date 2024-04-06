package com.unknownn.mouseclient;

import static com.unknownn.mouseclient.classes.UtilityKt.showSafeToast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.unknownn.mouseclient.classes.DataSaver;
import com.unknownn.mouseclient.classes.WebSocketClient;
import com.unknownn.mouseclient.databinding.ActivityMainBinding;
import com.unknownn.mouseclient.homepage.view.HomePage;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding = null;
    public static WebSocketClient socketClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.editTextIP.setText( getDataSaver().getPreviousIp() );
        setClickListener();
    }

    private void setClickListener(){
        binding.buttonConnect.setOnClickListener(view -> {
            String ip = binding.editTextIP.getText().toString().trim();
            connect(ip);
            getDataSaver().savePreviousIp(ip);
        });
    }

    private void connect(String ip){

        String text = binding.buttonConnect.getText().toString().trim();

        if(text.isEmpty()){
            showSafeToast(this,"Please wait...");
            return;
        }

        binding.buttonConnect.setText("");
        binding.progressBar.setVisibility(View.VISIBLE);
        startWebsocketClient(ip);
    }

    private void startWebsocketClient(String ip){
        if(socketClient == null){
            if(ip.isEmpty()) {
                socketClient = new WebSocketClient(() -> {
                    startActivity(new Intent(MainActivity.this, HomePage.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });
            }
            else{
                socketClient = new WebSocketClient(ip,4275,() -> {
                    startActivity(new Intent(MainActivity.this, HomePage.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });
            }
        }
    }

    private DataSaver dataSaver = null;
    private DataSaver getDataSaver(){
        if( dataSaver == null ){
            dataSaver = new DataSaver(this);
        }
        return dataSaver;
    }

}
