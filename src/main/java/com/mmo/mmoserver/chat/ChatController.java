package com.mmo.mmoserver.chat;

import com.mmo.mmoserver.auth.SessionRepository;
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

    private int maxChatSize = 15;
    private List<String> chatMsgs = new ArrayList<>(15);

    @Autowired
    private SessionRepository sessionRepository;

    @PostMapping("/msgs")
    public void sendMsg(@RequestBody ChatMessage chatMessage, HttpServletRequest request) {
        String session = getSessionFromRequestCookie(request);
        String username = sessionRepository.getUsername(session);
        String msg = username + ": " + chatMessage.getMsg();
        chatMsgs.add(msg);
        log.info("added msg: {}", msg);
        if (chatMsgs.size() > maxChatSize) {
            chatMsgs.remove(0);
        }
    }

    @GetMapping("/msgs")
    public ResponseEntity<List<String>> getMsgs(HttpServletRequest request) {
        boolean loggedIn = sessionRepository.isSessionExist(getSessionFromRequestCookie(request));
        if (!loggedIn) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("returning chat: {}", chatMsgs);
        return ResponseEntity.ok(chatMsgs);
    }

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
