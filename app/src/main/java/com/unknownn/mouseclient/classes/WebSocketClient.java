package com.unknownn.mouseclient.classes;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketClient {

    private DataOutputStream outputStream = null;
    private final SocketListener socketListener;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private DataListener dataListener = null;
    private ScreenShareListener screenShareListener = null;
    private String host = "192.168.0.104";
    private int port = 4275;

    public WebSocketClient(String ip, int port,SocketListener socketListener){
        this.socketListener = socketListener;
        this.host = ip;
        this.port = port;

        createManualClient();
    }

    public WebSocketClient(SocketListener socketListener){
        this.socketListener = socketListener;
        createManualClient();
    }



    private DataInputStream dataInputStream = null;
    private void createManualClient(){

        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(() -> {
            while (true) {
                try {

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

            int screenShareID = -1357; // Don't change here, change in desktop also
            while (true) {
                try {
                    int totalBytesOrId = dataInputStream.readInt();

                    if(totalBytesOrId == screenShareID){
                        int width = dataInputStream.readInt();
                        int height = dataInputStream.readInt();
                        screenShareListener.onScreenSizeReceived(width,height);
                    }
                    else {

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        int bytesReceived = 0;

                        while (bytesReceived < totalBytesOrId) {
                            int bytesLeft = totalBytesOrId - bytesReceived;
                            int toRead = Math.min(buffer.length, bytesLeft);

                            bytesRead = dataInputStream.read(buffer, 0, toRead);
                            if (bytesRead > 0) {
                                baos.write(buffer, 0, bytesRead);
                                bytesReceived += bytesRead;
                            }
                        }

                        byte[] imageBytes = baos.toByteArray();
                        screenShareListener.onCommandReceived(imageBytes);
                    }
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
    public void clearScreenShareListener(){
        this.screenShareListener = null;
    }

    public void requestScreenInfo(){
        SharedCommand command = new SharedCommand(SharedCommand.Type.SCREEN_INFO_REQUEST);
        sendMessage(command);
    }

    public void requestScreenShare(){
        SharedCommand command = new SharedCommand(SharedCommand.Type.SCREEN_SHARE_START_REQUEST);
        sendMessage(command);
    }

    public void stopScreenShare(){
        SharedCommand command = new SharedCommand(SharedCommand.Type.SCREEN_SHARE_STOP_REQUEST);
        sendMessage(command);
    }

    private void interpretCommand(String strCommand){
        SharedCommand command = gson.fromJson(strCommand, SharedCommand.class);
        if (dataListener != null) dataListener.onMessageReceived(command);
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
