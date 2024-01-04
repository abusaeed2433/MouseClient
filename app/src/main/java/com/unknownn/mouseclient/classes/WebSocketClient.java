package com.unknownn.mouseclient.classes;

import androidx.annotation.NonNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient {
    //private static final String WEBSOCKET_URL = "ws://27.147.190.170:8000";
    //private static final String WEBSOCKET_URL = "ws://192.168.29.91:8000";
    //private static final String WEBSOCKET_URL = "ws://localhost:8000";
    private static final String WEBSOCKET_URL = "http://localhost:8000";
    private WebSocket webSocket = null;
    private DataOutputStream outputStream = null;
    private final SocketListener socketListener;
    ExecutorService service = Executors.newSingleThreadExecutor();

    public WebSocketClient(SocketListener socketListener){
        this.socketListener = socketListener;
        createManualClient();
        //createClientObject();
    }

    private void createManualClient(){

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            while (true) {
                try {
                    String host = "192.168.0.104";
                    int port = 4275;

                    System.out.println("Trying to connect to "+host+":"+port);

                    Socket soc = new Socket(host, port);

                    System.out.println("Websocket reading output stream");
                    outputStream = new DataOutputStream(soc.getOutputStream());
                    System.out.println("Websocket ready to send data");
                    socketListener.onConnected();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    try{
                        Thread.sleep(3000);
                    }catch (InterruptedException ignored){}
                }
            }
        });
        service.shutdown();
    }

    public void sendMessage(String message){
        if(outputStream == null) return;

        service.execute(() -> {
            try {
                System.out.println("Websocket trying to write");
                outputStream.writeUTF(message);
                System.out.println("Websocket Written");
            }catch (IOException e){
                e.printStackTrace();
            }
        });
    }

    public interface SocketListener{
        void onConnected();
    }

}
