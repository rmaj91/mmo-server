package com.mmo.mmoserver.chat;

import com.mmo.mmoserver.auth.SessionService;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import static com.mmo.mmoserver.commons.Events.CHAT;
import static com.mmo.mmoserver.commons.Events.INIT_CHAT;

@Slf4j
@Service
public class ChatSocketController {

    private final SessionService sessionService;
    private final NettyWebSocketServer nettyWebSocketServer;
    private final ChatService chatService;


    @Autowired
    public ChatSocketController(NettyWebSocketServer nettyWebSocketServer, SessionService sessionService, ChatService chatService) {
        this.nettyWebSocketServer = nettyWebSocketServer;
        this.sessionService = sessionService;
        this.chatService = chatService;

        registerChatEvent(nettyWebSocketServer, sessionService);
        registerInitChat(nettyWebSocketServer);
    }

    private void registerChatEvent(NettyWebSocketServer nettyWebSocketServer, SessionService sessionService) {
        nettyWebSocketServer.getServer().addEventListener(CHAT, ChatMessage.class, (client, data, ackSender) -> {
            String username = sessionService.getUsernameByClientId(client.getSessionId().toString());
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
