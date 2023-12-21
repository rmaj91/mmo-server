package com.mmo.mmoserver.chat;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.listener.EventInterceptor;
import com.corundumstudio.socketio.transport.NamespaceClient;
import com.mmo.mmoserver.auth.SessionRepository;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mmo.mmoserver.auth.AuthController.SESSION_COOKIE_NAME;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private int maxChatSize = 10;
    private List<String> chatMsgs = new ArrayList<>(15);

//    @Autowired
//    NettyWebSocketServer nettyWebSocketServer;

    public ChatController(NettyWebSocketServer nettyWebSocketServer) {
        nettyWebSocketServer.getServer().addEventListener("chat", ChatMessage.class, (client, data, ackSender) -> {
            String username = sessionRepository.clientIdToUsername.get(client.getSessionId().toString());
            String msg = username + ": " + data.getMsg();
            chatMsgs.add(msg);
            if (chatMsgs.size() > maxChatSize) {
                chatMsgs.remove(0);
            }

            nettyWebSocketServer.getServer().getBroadcastOperations().sendEvent("chat", chatMsgs);//todo implement to also send after connection
        });

        nettyWebSocketServer.getServer().addEventListener("init-chat", ChatMessage.class, (client, data, ackSender) -> {
            client.sendEvent("chat", chatMsgs);
        });
    }

    @Autowired
    private SessionRepository sessionRepository;

//    @PostMapping("/msgs")
//    public void sendMsg(@RequestBody ChatMessage chatMessage, HttpServletRequest request) {
//        String session = getSessionFromRequestCookie(request);
//        String username = sessionRepository.getUsername(session);
//        String msg = username + ": " + chatMessage.getMsg();
//        chatMsgs.add(msg);
//        log.info("added msg: {}", msg);
//        if (chatMsgs.size() > maxChatSize) {
//            chatMsgs.remove(0);
//        }
//    }

//    @GetMapping("/msgs")
//    public ResponseEntity<List<String>> getMsgs(HttpServletRequest request) {
//        boolean loggedIn = sessionRepository.isSessionExist(getSessionFromRequestCookie(request));
//        if (!loggedIn) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//        log.info("returning chat: {}", chatMsgs);
//        return ResponseEntity.ok(chatMsgs);
//    }

    private static String getSessionFromRequestCookie(HttpServletRequest request) {
        String session = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            session =  Arrays.stream(cookies)
                    .filter(cookie -> SESSION_COOKIE_NAME.equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return session;
    }
}
