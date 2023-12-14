package com.mmo.mmoserver.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SessionRepository {

    Map<String, String> sessionToUsername = new HashMap<>();
    Map<String, String> usernameToSession = new HashMap<>();

    public void setSession(String username, String session) {
        sessionToUsername.put(session, username);
        usernameToSession.put(username, session);
    }

    public void removeSession(String session) {
        String existingUser = sessionToUsername.get(session);
        usernameToSession.remove(existingUser);
        sessionToUsername.remove(session);
    }

    public String getUsername(String session) {
        return sessionToUsername.get(session);
    }

    public boolean isAlreadyAuthenticated(String username) {
        return usernameToSession.containsKey(username);
    }

    public boolean isSessionExist(String session) {
        return sessionToUsername.containsKey(session);
    }
}
