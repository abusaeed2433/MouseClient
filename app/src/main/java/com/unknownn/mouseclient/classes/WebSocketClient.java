package com.unknownn.mouseclient.classes;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketClient {
    private DataOutputStream outputStream = null;
    private final SocketListener socketListener;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private DataListener dataListener = null;
    private ScreenShareListener screenShareListener = null;

    public WebSocketClient(SocketListener socketListener){
        this.socketListener = socketListener;
        createManualClient();
        //createClientObject();
    }

    private void createManualClient(){

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            DataInputStream dataInputStream = null;

            while (true) {
                try {
                    String host = "192.168.0.104";
                    int port = 4275;

                    System.out.println("Trying to connect to "+host+":"+port);

                    Socket soc = new Socket(host, port);

                    System.out.println("Websocket reading output stream");
                    outputStream = new DataOutputStream(soc.getOutputStream());

                    dataInputStream = new DataInputStream(soc.getInputStream());

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

            while (true) {
                try {
                    String strCommand = dataInputStream.readUTF();
                    interpretCommand(strCommand);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        });
        service.shutdown();
    }

    public void setDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
    }
    public void setScreenShareListener(ScreenShareListener shareListener){
        this.screenShareListener = shareListener;
    }

    public void requestScreenInfo(){
        SharedCommand command = new SharedCommand(SharedCommand.Type.SCREEN_INFO_REQUEST);
        sendMessage(command);
    }

    private void interpretCommand(String strCommand){
        SharedCommand command = gson.fromJson(strCommand, SharedCommand.class);
        if(command.getType() == SharedCommand.Type.SCREEN_INFO){
            if(screenShareListener != null) screenShareListener.onCommandReceived(command);
        }
        else {
            if (dataListener != null) dataListener.onMessageReceived(command);
        }
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
