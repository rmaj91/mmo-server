package com.mmo.mmoserver.chat;

import com.mmo.mmoserver.auth.SessionRepository;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import static com.mmo.mmoserver.websockets.Events.CHAT;
import static com.mmo.mmoserver.websockets.Events.INIT_CHAT;

@Slf4j
@Service
public class ChatSocketController {

    private final SessionRepository sessionRepository;
    private final NettyWebSocketServer nettyWebSocketServer;
    private final ChatService chatService;


    @Autowired
    public ChatSocketController(NettyWebSocketServer nettyWebSocketServer, SessionRepository sessionRepository, ChatService chatService) {
        this.nettyWebSocketServer = nettyWebSocketServer;
        this.sessionRepository = sessionRepository;
        this.chatService = chatService;

        registerChatEvent(nettyWebSocketServer, sessionRepository);
        registerInitChat(nettyWebSocketServer);
    }

    private void registerChatEvent(NettyWebSocketServer nettyWebSocketServer, SessionRepository sessionRepository) {
        nettyWebSocketServer.getServer().addEventListener(CHAT, ChatMessage.class, (client, data, ackSender) -> {
            String username = sessionRepository.clientIdToUsername.get(client.getSessionId().toString());
            chatService.addMessage(username, data.getMsg());

            nettyWebSocketServer.broadcastEvent(CHAT, chatService.getAllMessages());
        });
    }

    private void registerInitChat(NettyWebSocketServer nettyWebSocketServer) {
        nettyWebSocketServer.getServer().addEventListener(INIT_CHAT, ChatMessage.class, (client, data, ackSender) -> {
            client.sendEvent(CHAT, chatService.getAllMessages());
        });
    }
}
