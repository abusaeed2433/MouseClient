package com.unknownn.mouseclient.classes;

import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketClient {
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

    public void sendMessage(SharedCommand command){
        if(outputStream == null) return;
        service.execute(() -> {
            try {
                Gson gson = new Gson();
                String json = gson.toJson(command);

                System.out.println("Sent data");
                outputStream.writeUTF(json);
                System.out.println("Sent data done");
            }catch (IOException e){
                e.printStackTrace();
            }
        });
    }

    public interface SocketListener{
        void onConnected();
    }

}
