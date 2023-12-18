package com.mmo.mmoserver.auth;

import com.mmo.mmoserver.engine.GameEngine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SessionRepository {

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    Map<String, String> sessionToUsername = new HashMap<>();
    Map<String, String> usernameToSession = new HashMap<>();
    Set<String> activeSessions = new HashSet<>();

    private final GameEngine gameEngine;

    public SessionRepository(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
        log.info("starting - cleanUpOldSessions process.");
        cleanUpOldSessions();
    }

    private void cleanUpOldSessions() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sessionToUsername.forEach((key, value) -> {
                    if (!activeSessions.contains(key)) {
                        String removedUser = sessionToUsername.remove(key);
                        usernameToSession.remove(removedUser);
                        gameEngine.clearUsername(removedUser);

                        log.info("cleanUpOldSessions \"{}\" has been logout! because of inactivity.", removedUser);
                    }
                });
                activeSessions.clear();
                log.info("cleanUpOldSessions - finished");
            } catch (Exception e) {
                log.error("cleanUpOldSessions.", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void setSession(String username, String session) {
        sessionToUsername.put(session, username);
        usernameToSession.put(username, session);
        activeSessions.add(session);
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
}
