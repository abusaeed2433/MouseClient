package com.unknownn.mouseclient.main_activity.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.unknownn.mouseclient.classes.WebSocketClient;
import com.unknownn.mouseclient.databinding.ActivityMainBinding;
import com.unknownn.mouseclient.homepage.view.HomePage;
import com.unknownn.mouseclient.main_activity.viewmodel.MainActivityViewModel;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding = null;
    public static WebSocketClient socketClient = null;
    private MainActivityViewModel viewModel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        initRV();
        observeViewModel();
        setClickListener();
        checkShareRequest();
    }

    private IpAdapter adapter = null;
    private void initRV(){
        adapter = new IpAdapter(ip -> viewModel.connect(ip));
        binding.rvRecentIps.setAdapter(adapter);
    }

    private void setClickListener(){
        binding.buttonConnect.setOnClickListener(view -> {
            String ip = String.valueOf(binding.editTextIP.getText()).trim();
            viewModel.connect(ip);
        });
    }

    private void observeViewModel(){
        viewModel.getButtonText().observe(this, s -> binding.buttonConnect.setText( ((s==null) ? "Connect" : s) ));

        viewModel.getProgressBar().observe(this, show -> {
            if(show == null) return;

            binding.progressBar.setVisibility( (show ? View.VISIBLE : View.INVISIBLE));
        });

        viewModel.getIps().observe(this, strings -> {
            adapter.submitList(strings);
        });

        viewModel.getActivitySwitch().observe(this, aBoolean -> {
            if(aBoolean == null || !aBoolean) return;

            startActivity( new Intent(MainActivity.this, HomePage.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void checkShareRequest() {
        if(MainActivity.socketClient == null) return;

        final Intent intent = getIntent();
        final Intent myIntent = new Intent(this, HomePage.class);

        if (Objects.equals(intent.getAction(), Intent.ACTION_SEND) && Objects.equals(intent.getType(), "text/plain")) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if(text == null) return;

            myIntent.putExtra("text",text);
        }
        else {
            Bundle bundle = intent.getExtras();
            if (bundle == null) return;

            Uri dataUri = (Uri) bundle.get(Intent.EXTRA_STREAM);
            if(dataUri == null) return;

            myIntent.putExtra("data_uri", dataUri.toString());
        }

        startActivity(myIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

}
