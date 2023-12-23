package com.mmo.mmoserver.websockets;

import com.corundumstudio.socketio.ClientOperations;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.mmo.mmoserver.auth.SessionService;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyWebSocketServer {

    @Autowired
    private SessionService sessionService;


    private final SocketIOServer server;

    public SocketIOServer getServer() {
        return server;
    }

    public NettyWebSocketServer() {
        // Set up configuration
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(3000); // Set the desired port

        // Create Socket.IO server
        this.server = new SocketIOServer(config);

        // Set up event listeners and other server logic
        setupEventListeners(server);

        // Start the server
        server.start();

        // Stop the server gracefully on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.getAllClients().forEach(ClientOperations::disconnect);
            server.stop();
        }));
    }

    private void setupEventListeners(SocketIOServer server) {
        server.addConnectListener(client -> {
            HttpHeaders httpHeaders = client.getHandshakeData().getHttpHeaders();

            String cookies = httpHeaders.get("Cookie");
            String sessionPrefix = "session=";
            int startIndex = cookies.indexOf(sessionPrefix);
            int sessionStartIndex = startIndex + sessionPrefix.length();
            int uuidLengthWithHyphens = 36;
            String session = cookies.substring(sessionStartIndex, sessionStartIndex + uuidLengthWithHyphens);
            //todo later use this io cookie + bearer header to auth
            String username = this.sessionService.getUsername(session);
            sessionService.putClientIdToUsername(client.getSessionId().toString(), username);
            sessionService.putUsernameToClientId(username, client.getSessionId().toString());
            log.info("\"{}\" connected. session: {}", username, session);
        });

        server.addDisconnectListener(client -> {
            System.out.println("Client disconnected: " + client.getSessionId());
            String removedUsername = sessionService.removeClientIdToUsername(client.getSessionId().toString());
            sessionService.removeUsernameToClientId(removedUsername);
        });

        server.addEventListener("client-message", String.class, (client, data, ackSender) -> {
            System.out.println("Received message from client: " + data);
            // Send an acknowledgment if needed
//            ackSender.sendAckData("Message received!");
        });
    }

    public void broadcastEvent(String event, Object data) {
        server.getBroadcastOperations().sendEvent(event, data);
    }
}
