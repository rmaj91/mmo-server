package com.mmo.mmoserver.engine;

import com.corundumstudio.socketio.SocketIOClient;
import com.mmo.mmoserver.auth.SessionService;
import com.mmo.mmoserver.player.PlayerState;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.mmo.mmoserver.commons.Events.STATE;

@Slf4j
@Component
public class GameEngine {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final Map<String, PlayerState> userToPosition = new HashMap<>();
    private final Map<String, String> userToState = new HashMap<>();
    private final Map<String, Double> userToDirection = new HashMap<>();

    private final NettyWebSocketServer nettyWebSocketServer;
    private final SessionService sessionService;

    public GameEngine(NettyWebSocketServer nettyWebSocketServer,
                      SessionService sessionService) {
        this.nettyWebSocketServer = nettyWebSocketServer;
        this.sessionService = sessionService;

        start();
        scheduleCleanUpOldSessions();
    }

    private void scheduleCleanUpOldSessions() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                synchronized (sessionService) {
                    List<String> loggedInSessions = sessionService.getAllLoggedInSessions();
                    Set<String> allActiveSessions = sessionService.getAllActiveSessions();
                    for (String loggedInSession : loggedInSessions) {
                        if (!allActiveSessions.contains(loggedInSession)) {
                            String username = sessionService.getUsername(loggedInSession);
                            sessionService.removeSession(loggedInSession);
                            clearUsername(username);

                            log.info("cleanUpOldSessions \"{}\" has been logout! because of inactivity.", username);
                        }
                    }
                    sessionService.clearActiveSessions();
                }
                log.info("cleanUpOldSessions - finished");
            } catch (Exception e) {
                log.error("cleanUpOldSessions.", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public synchronized void clearUsername(String username) {
        userToPosition.remove(username);
        userToState.remove(username);
        userToDirection.remove(username);
    }

    public synchronized void setPlayerDirection(String username, double rotationY) {
        userToDirection.put(username, rotationY);
    }

    public synchronized void setPlayerState(String username, String state) {
        userToState.put(username, state);
    }

    public synchronized PlayerState getPlayerPosition(String username) {
        return userToPosition.get(username);
    }

    public synchronized PlayerState setPlayerPosition(String username, PlayerState position) {
        return userToPosition.put(username, position);
    }

    private void start() {
        Runnable runnable = () -> {
            while (true) {
                gameLoopIteration();
            }
        };
        new Thread(runnable).start();
    }

    private void sendGameStateToPlayers() {
        userToPosition.forEach((username, position) -> {
            PlayerState toReturn = new PlayerState(position.getPx(), position.getPy(), position.getPz());
            userToPosition.forEach((u,p) -> {
                if (!u.equals(username)) {
                    PlayerState toAdd = new PlayerState(p.getPx(), p.getPy(), p.getPz());
                    toAdd.setUsername(u);
                    toReturn.getAnotherPlayers().add(toAdd);
                }
            });

            String clientId = sessionService.getClientId(username);
            if (clientId != null) {
                SocketIOClient client = nettyWebSocketServer.getServer().getClient(UUID.fromString(clientId));
                client.sendEvent(STATE, toReturn);
//                log.info("Sending 'state' to the {}. data: {}", username, position);
            } else {
                log.info("ClientId not found for: {}", username);
            }
        });
    }

    private void gameLoopIteration() {
        try {
            List<String> usersToAddIdle = new ArrayList<>();
            userToState.forEach((k,v)-> {
                if (v.equals("walk")) {
                    PlayerState position = userToPosition.getOrDefault(k, new PlayerState(Math.random() * 10, 0 , Math.random() * 10));
                    Double direction = userToDirection.getOrDefault(k, 0d);

                    //apply magic logic
                    // Move 1 unit in the direction of rotation
                    double deltaX = 0.3 * Math.cos(direction);
                    double deltaZ = 0.3 * Math.sin(direction);
                    // Calculate the new position
                    double newX = position.getPx() + deltaX;
                    double newZ = position.getPz() - deltaZ;

                    PlayerState newState = new PlayerState(newX, position.getPy(), newZ);
                    log.info("new state: {}, {}, {}", newX, position.getPy(), newZ);
                    userToPosition.put(k, newState);
                }
                else {
                    userToPosition.putIfAbsent(k, new PlayerState(Math.random() * 10, 0 , Math.random() * 10));
                    usersToAddIdle.add(k);
                }
            });
            for (String toAdd : usersToAddIdle) {
                userToState.put(toAdd, "idle");
            }

            sendGameStateToPlayers();//todo: fix, because sending affects game loop time

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            log.info("Exception in game loop. {}, {}", e.getCause(), e.getMessage(), e);
        }
    }
}
