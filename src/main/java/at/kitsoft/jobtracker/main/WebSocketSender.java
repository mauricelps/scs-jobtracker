package at.kitsoft.jobtracker.main;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import at.kitsoft.jobtracker.util.JobTrackerUtils;

public class WebSocketSender extends WebSocketClient {

    public JobTrackerUtils jtu;

    {
        jtu = new JobTrackerUtils();
    }

    public WebSocketSender(String targetURL){
        super(URI.create(targetURL));
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        // TODO Auto-generated method stub
        System.out.println("[REMOTE] WebSocket Opened");
        send("Tracker connected - ID:" + jtu.getInstallationId().toString() + ".");
    }

    @Override
    public void onMessage(String message) {
        // TODO Auto-generated method stub
        System.out.println("[REMOTE] WebSocket Message: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // TODO Auto-generated method stub
        System.out.println("[REMOTE] WebSocket Closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        // TODO Auto-generated method stub
        System.out.println("[REMOTE] WebSocket Error: " + ex.getMessage());
    }

    public void sendMessage(String msg) {
        if(!isOpen()){
            throw new IllegalStateException("WebSocket is not open - cannot send message.");
        }
        send(msg);
        System.out.println("[REMOTE] WebSocket Sent: " + msg);
    }
}