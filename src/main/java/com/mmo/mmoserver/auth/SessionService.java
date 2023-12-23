package com.mmo.mmoserver.auth;

import com.mmo.mmoserver.auth.repo.LoginHistoryEntity;
import com.mmo.mmoserver.auth.repo.LoginHistoryRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class SessionService {

    private final Map<String, String> sessionToUsername = new HashMap<>();
    private final Map<String, String> usernameToSession = new HashMap<>();
    private final Map<String, String> clientIdToUsername = new HashMap<>();
    private final Map<String, String> usernameToClientId = new HashMap<>();

    private final Set<String> activeSessions = new HashSet<>();

    @Autowired
    private LoginHistoryRepo loginHistoryRepo;


    public synchronized void setSession(String username, String session) {
        sessionToUsername.put(session, username);
        usernameToSession.put(username, session);
        activeSessions.add(session);
        LoginHistoryEntity loginHistoryEntity = new LoginHistoryEntity();
        loginHistoryEntity.setId(UUID.randomUUID());
        loginHistoryEntity.setLogin(username);
        loginHistoryEntity.setDate(new Date());
        loginHistoryRepo.save(loginHistoryEntity);
    }

    public synchronized void removeSession(String session) {
        String username = sessionToUsername.remove(session);
        usernameToSession.remove(username);

        String clientId = usernameToClientId.remove(username);
        clientIdToUsername.remove(clientId);
    }

    public synchronized List<String> getAllLoggedInSessions() {
        return sessionToUsername.keySet().stream().toList();
    }

    public synchronized Set<String> getAllActiveSessions() {
        return Set.copyOf(activeSessions);
    }

    public synchronized void keepAlive(String session) {
        activeSessions.add(session);
    }

    public synchronized void clearActiveSessions() {
        activeSessions.clear();
    }

    public synchronized void putClientIdToUsername(String clientId, String username) {
        clientIdToUsername.put(clientId, username);
    }

    public void putUsernameToClientId(String username, String clientId) {
        usernameToClientId.put(username, clientId);
    }

    public synchronized String removeClientIdToUsername(String clientId) {
        return clientIdToUsername.remove(clientId);
    }

    public synchronized String removeUsernameToClientId(String username) {
        return usernameToClientId.remove(username);
    }

    public int getTotalOnline() {
        return usernameToSession.size();
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

    public String getClientId(String username) {
        return usernameToClientId.get(username);
    }

    public String getUsernameByClientId(String clientId) {
        return clientIdToUsername.get(clientId);
    }
}
