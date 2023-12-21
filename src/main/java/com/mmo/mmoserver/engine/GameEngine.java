package com.mmo.mmoserver.engine;

import com.corundumstudio.socketio.SocketIOClient;
import com.mmo.mmoserver.auth.SessionRepository;
import com.mmo.mmoserver.player.PlayerState;
import com.mmo.mmoserver.player.RotationUpdateRequest;
import com.mmo.mmoserver.player.StateUpdateRequest;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GameEngine {

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Map<String, PlayerState> userToPosition = new HashMap<>();
    private Map<String, String> userToState = new HashMap<>();
    private Map<String, Double> userToDirection = new HashMap<>();

//    @Autowired
    NettyWebSocketServer nettyWebSocketServer;
//    @Autowired
    SessionRepository sessionRepository;

    public GameEngine(NettyWebSocketServer nettyWebSocketServer,
                      SessionRepository sessionRepository
    ) {
        this.nettyWebSocketServer = nettyWebSocketServer;
        this.sessionRepository = sessionRepository;
        start();
        initListiners();
        scheduleCleanUpOldSessions();
    }

    private void scheduleCleanUpOldSessions() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sessionRepository.sessionToUsername.forEach((key, value) -> {
                    if (!sessionRepository.activeSessions.contains(key)) {
                        String removedUser = sessionRepository.sessionToUsername.remove(key);
                        sessionRepository.usernameToSession.remove(removedUser);
                        clearUsername(removedUser);

                        log.info("cleanUpOldSessions \"{}\" has been logout! because of inactivity.", removedUser);
                    }
                });
                sessionRepository.activeSessions.clear();
                log.info("cleanUpOldSessions - finished");
            } catch (Exception e) {
                log.error("cleanUpOldSessions.", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void initListiners() {
        nettyWebSocketServer.getServer().addEventListener("state", StateUpdateRequest.class, (client, data, ackSender) -> {
            System.out.println("Received state smg from client: " + data);

            String username = sessionRepository.clientIdToUsername.get(client.getSessionId().toString());
            setPlayerState(username, data.getState());
            log.info("SOCKET.IO: Updating player state to: {}", data.getState());
        });

        nettyWebSocketServer.getServer().addEventListener("direction", RotationUpdateRequest.class, (client, data, ackSender) -> {
            System.out.println("Received direction from client: " + data);

            String username = sessionRepository.clientIdToUsername.get(client.getSessionId().toString());
            setPlayerDirection(username, data.getRotationY());
            log.info("SOCKET.IO: Updating player direction to: {}", data.getRotationY());
        });
    }

    public void clearUsername(String username) {
        userToPosition.remove(username);
        userToState.remove(username);
        userToDirection.remove(username);
    }

    private void start() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {

                    userToState.forEach((k,v)-> {
                        if (v.equals("walk")) {
                            PlayerState position = userToPosition.getOrDefault(k, new PlayerState(10, 0, 10));
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
                    });
                    sendGameStateToPlayers();

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        new Thread(runnable).start();
    }

    private void sendGameStateToPlayers() {
        userToPosition.forEach((username, position) -> {
            PlayerState status = userToPosition.getOrDefault(username, new PlayerState(10, 0, 10));//default position 10,10
            PlayerState toReturn = new PlayerState(status.getPx(), status.getPy(), status.getPz()); //copy because keeping array of players
            userToPosition.forEach((u,p) -> {
                if (!u.equals(username)) {
                    PlayerState toAdd = new PlayerState(p.getPx(), p.getPy(), p.getPz());
                    toAdd.setUsername(u);
                    toReturn.getAnotherPlayers().add(toAdd);
                }
            });


            String clientId = sessionRepository.getUsernameToClientId().get(username);
            SocketIOClient client = nettyWebSocketServer.getServer().getClient(UUID.fromString(clientId));
            client.sendEvent("state", toReturn);
            log.info("Send 'state' to the {}. data: {}", username, position);
        });

    }

    public PlayerState getPlayerState(String username) {
        PlayerState status = userToPosition.getOrDefault(username, new PlayerState(10, 0, 10));//default position 10,10
        PlayerState toReturn = new PlayerState(status.getPx(), status.getPy(), status.getPz()); //copy because keeping array of players
        userToPosition.forEach((k,v) -> {
            if (!k.equals(username)) {
                PlayerState toAdd = new PlayerState(v.getPx(), v.getPy(), v.getPz());
                toAdd.setUsername(k);
                toReturn.getAnotherPlayers().add(toAdd);
            }
        });
        return toReturn;
    }

    public void setPlayerDirection(String username, double rotationY) {
        userToDirection.put(username, rotationY);
//        log.info("Updating rotationY for player \"{}\", to:\"{}\".", username, rotationY);
    }

    public void setPlayerState(String username, String state) {
        userToState.put(username, state);
//        log.info("Updating state for player \"{}\", to: \"{}\".", username, state);
    }
}
