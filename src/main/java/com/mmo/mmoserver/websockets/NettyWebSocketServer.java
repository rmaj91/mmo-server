package com.mmo.mmoserver.websockets;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyWebSocketServer {

    public NettyWebSocketServer() {
        // Set up configuration
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(3000); // Set the desired port

        // Create Socket.IO server
        SocketIOServer server = new SocketIOServer(config);

        // Set up event listeners and other server logic
        setupEventListeners(server);

        // Start the server
        server.start();

        // Stop the server gracefully on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    private static void setupEventListeners(SocketIOServer server) {
        server.addConnectListener(client -> {
            System.out.println("Client connected: " + client.getSessionId());
        });

        server.addDisconnectListener(client -> {
            System.out.println("Client disconnected: " + client.getSessionId());
        });

        server.addEventListener("client-message", String.class, (client, data, ackSender) -> {
            System.out.println("Received message from client: " + data);
            // Send an acknowledgment if needed
            ackSender.sendAckData("Message received!");
        });
    }
}
