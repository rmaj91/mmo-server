package com.mmo.mmoserver.chat;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private static final int MAX_CHAT_SIZE = 10;

    private List<String> chatMessages = new ArrayList<>(MAX_CHAT_SIZE);

    public void addMessage(String username, String message) {
        String msg = username + ": " + message;
        chatMessages.add(msg);
        if (chatMessages.size() > MAX_CHAT_SIZE) {
            chatMessages.remove(0);
        }
    }

    public List<String> getAllMessages() {
        return new ArrayList<>(chatMessages);
    }
}
