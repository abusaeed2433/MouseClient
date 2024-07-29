package com.unknownn.mouseclient.classes;

import com.google.gson.Gson;
import com.unknownn.mouseclient.mouse_controller.model.DataListener;
import com.unknownn.mouseclient.mouse_controller.model.SharedCommand;
import com.unknownn.mouseclient.screen_share_activity.model.ScreenShareListener;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketClient {

    private static final int CHUNK_SIZE = 4096;
    private static final int BYTE_START_CODE = 5555;
    private static final int BYTE_END_CODE = 7777;

    private DataOutputStream outputStream = null;
    private final SocketListener socketListener;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private DataListener dataListener = null;
    private ScreenShareListener screenShareListener = null;
    private TextAndFileListener textAndFileListener = null;
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

    private DataInputStream inputStream = null;
    @SuppressWarnings("all")
    private void createManualClient(){
        final ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(() -> {
            while (true) {
                try{
                    Socket soc = new Socket(host,port);

                    System.out.println("Websocket reading output stream");

                    outputStream = new DataOutputStream(soc.getOutputStream());
                    inputStream = new DataInputStream(soc.getInputStream());

                    socketListener.onConnected();
                    break;
                }
                catch (Exception e) {
                    System.out.println("Creating client error: "+e.getMessage());
                    try{
                        Thread.sleep(3000);
                    }catch (InterruptedException ignored){}
                }
            }

            while (true) {
                try {
                    int messageID = inputStream.readInt();

                    if(messageID == Type.CLIP_TEXT.id){
                        byte[] bytes = readBytes(inputStream);
                        textAndFileListener.onTextReceived(bytes);
                    }
                    else if(messageID == Type.SCREEN_INFO.id){
                        final byte[] bytes = readBytes(inputStream);
                        final String str = new String(bytes, StandardCharsets.UTF_8);

                        System.out.println("Received screen info: "+str);
                        final String regex = "(\\d+),(\\d+)";
                        final Pattern pattern = Pattern.compile(regex);
                        final Matcher matcher = pattern.matcher(str);

                        String w = matcher.group(1);
                        String h = matcher.group(2);

                        if(w == null || h == null) return;

                        int width = Integer.parseInt(w);
                        int height = Integer.parseInt(h);

                        screenShareListener.onScreenSizeReceived(width,height);
                    }
                    else if(messageID == Type.SCREEN_SHARE.id){
                        byte[] bytesImage = readBytes(inputStream);
                        screenShareListener.onCommandReceived(bytesImage);
                    }
                    else if(messageID == Type.FILE.id){
                        byte[] bytesName = readBytes(inputStream);
                        byte[] bytesContent = readBytes(inputStream);

                        textAndFileListener.onFileReceived(bytesName, bytesContent);
                    }

                }catch (Exception e){
                    System.out.println("Error receiving: "+e.getMessage());
                }
            }

        });
        service.shutdown();
    }

    private byte[] readBytes(DataInputStream dataInputStream) throws IOException{
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        int bytesReceived = 0;

        dataInputStream.readInt();// start code
        final int totalBytes = dataInputStream.readInt();

        while (bytesReceived < totalBytes) {
            int bytesLeft = totalBytes - bytesReceived;
            int toRead = Math.min(buffer.length, bytesLeft);

            bytesRead = dataInputStream.read(buffer, 0, toRead);
            if (bytesRead > 0) {
                baos.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
            }
        }

        dataInputStream.readInt(); // end code
        return baos.toByteArray();
    }

    public void setDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
    }
    public void setScreenShareListener(ScreenShareListener shareListener){
        this.screenShareListener = shareListener;
    }

    public void setTextAndFileListener(TextAndFileListener textAndFileListener) {
        this.textAndFileListener = textAndFileListener;
    }

    public void clearScreenShareListener(){
        this.screenShareListener = null;
    }

    public void requestScreenInfo(){
        SharedCommand command = new SharedCommand(SharedCommand.Type.SCREEN_INFO_REQUEST,null);
        sendMessage(command);
    }

    public void requestScreenShare(){
        SharedCommand command = new SharedCommand(SharedCommand.Type.SCREEN_SHARE_START_REQUEST,null);
        sendMessage(command);
    }

    public void stopScreenShare(){
        SharedCommand command = new SharedCommand(SharedCommand.Type.SCREEN_SHARE_STOP_REQUEST,null);
        sendMessage(command);
    }

    public void shareClipText(String text) throws IOException {
        final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        sendBytes(Type.CLIP_TEXT.id,bytes);
    }

    public void sendFile(String fileName, byte[] bytes) throws IOException{
        final byte[] bytesName = fileName.getBytes(StandardCharsets.UTF_8);
        sendBytes(Type.FILE.id, bytesName, bytes);
    }

    @SuppressWarnings("unused")
    private void interpretCommand(String strCommand){
        SharedCommand command = gson.fromJson(strCommand, SharedCommand.class);
        if (dataListener != null) dataListener.onMessageReceived(command);
    }

    // order: BYTE_START_CODE SIZE [chunk1, chunk2, ...], BYTE_END_CODE
    private void sendBytes(Integer id, byte[]... allBytes){
        service.execute(() -> {
            try{
                if(outputStream == null) return;

                if(id != null) { outputStream.writeInt(id); }

                for(byte[] bytes : allBytes) {
                    outputStream.writeInt(BYTE_START_CODE);

                    final int totalBytes = bytes.length;
                    outputStream.writeInt(totalBytes);

                    int bytesSent = 0;

                    while (bytesSent < totalBytes) {
                        int bytesLeft = totalBytes - bytesSent;
                        int toSend = Math.min(CHUNK_SIZE, bytesLeft);
                        outputStream.write(bytes, bytesSent, toSend);
                        bytesSent += toSend;
                        outputStream.flush();
                    }

                    outputStream.writeInt(BYTE_END_CODE);
                }
                outputStream.flush();
            }catch (IOException e){
                System.out.println("Error is: "+e.getMessage());
                outputStream = null;
            }
        });
    }

    public void sendMessage(SharedCommand command){
        if(outputStream == null) return;
        service.execute(() -> {
            try {
                Gson gson = new Gson();
                String json = gson.toJson(command);

                outputStream.writeInt(Type.SHARED_COMMAND.id);

                outputStream.writeUTF(json);
            }catch (IOException e){
                System.out.println("Send command failed: "+e.getLocalizedMessage());
            }
        });
    }

    enum Type{ // 5 digits
        SCREEN_INFO(11111), SCREEN_SHARE(13571), CLIP_TEXT(55555),
        SHARED_COMMAND(66666), FILE(77777);

        final int id;

        Type(int id) {
            this.id = id;
        }
    }

    public interface SocketListener{
        void onConnected();
    }
    public interface TextAndFileListener{
        void onTextReceived(byte[] bytes);
        void onFileReceived(byte[] bytesName, byte[] bytesContent);
    }
}
