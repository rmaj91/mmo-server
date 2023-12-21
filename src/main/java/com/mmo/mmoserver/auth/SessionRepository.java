package com.mmo.mmoserver.auth;

import com.mmo.mmoserver.auth.repo.LoginHistoryEntity;
import com.mmo.mmoserver.auth.repo.LoginHistoryRepo;
import com.mmo.mmoserver.engine.GameEngine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SessionRepository {



    public Map<String, String> sessionToUsername = new HashMap<>();
    public Map<String, String> usernameToSession = new HashMap<>();
    public Set<String> activeSessions = new HashSet<>();
    public Map<String, String> clientIdToUsername = new HashMap<>();
    public Map<String, String> usernameToClientId = new HashMap<>();

    @Autowired
    private LoginHistoryRepo loginHistoryRepo;

//    private final GameEngine gameEngine;

//    public SessionRepository(GameEngine gameEngine) {
    public SessionRepository() {
//        this.gameEngine = gameEngine;
        log.info("starting - cleanUpOldSessions process.");
//        cleanUpOldSessions();
    }



    public void setSession(String username, String session) {
        sessionToUsername.put(session, username);
        usernameToSession.put(username, session);
        activeSessions.add(session);
        LoginHistoryEntity loginHistoryEntity = new LoginHistoryEntity();
        loginHistoryEntity.setId(UUID.randomUUID());
        loginHistoryEntity.setLogin(username);
        loginHistoryEntity.setDate(new Date());
        loginHistoryRepo.save(loginHistoryEntity);
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

    public void keepAlive(String session) {
        activeSessions.add(session);
    }

    public Map<String, String> getClientIdToUsername() {
        return clientIdToUsername;
    }

    public Map<String, String> getUsernameToClientId() {
        return usernameToClientId;
    }
}
