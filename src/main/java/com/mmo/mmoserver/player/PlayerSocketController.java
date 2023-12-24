package com.mmo.mmoserver.player;

import com.mmo.mmoserver.auth.SessionService;
import com.mmo.mmoserver.engine.GameEngine;
import com.mmo.mmoserver.mobs.MobAttack;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.mmo.mmoserver.GameConfig.MAP_X;
import static com.mmo.mmoserver.GameConfig.MAP_Z;
import static com.mmo.mmoserver.commons.Events.*;

@Slf4j
@Component
public class PlayerSocketController {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final NettyWebSocketServer nettyWebSocketServer;
    private final SessionService sessionService;
    private final GameEngine gameEngine;

    public PlayerSocketController(NettyWebSocketServer nettyWebSocketServer, SessionService sessionService, GameEngine gameEngine) {
        this.nettyWebSocketServer = nettyWebSocketServer;
        this.sessionService = sessionService;
        this.gameEngine = gameEngine;

        initStateListener();
        initDirectionListener();
        initAttackListener();

        scheduleCleanUpOldSessions();
    }

    private void initAttackListener() {
        nettyWebSocketServer.getServer().addEventListener(MELEE, MobAttack.class, (client, data, ackSender) -> {
            String username = sessionService.getUsernameByClientId(client.getSessionId().toString());
            gameEngine.setMobMeleeAttack(username, data.getMob());
            log.info("Player {} tries melee attack mob {}", username,data.getMob());
        });

        nettyWebSocketServer.getServer().addEventListener(RANGED, MobAttack.class, (client, data, ackSender) -> {
            String username = sessionService.getUsernameByClientId(client.getSessionId().toString());
            gameEngine.setMobRangedAttack(username, data.getMob());
            log.info("Player {} tries ranged attack mob {}", username,data.getMob());
        });
    }

    private void initStateListener() {
        nettyWebSocketServer.getServer().addEventListener(STATE, StateUpdateRequest.class, (client, data, ackSender) -> {
            String username = sessionService.getUsernameByClientId(client.getSessionId().toString());
            gameEngine.setPlayerState(username, data.getState().toString());
            log.info("SOCKET.IO: Updating player state to: {}", data.getState());
        });

        nettyWebSocketServer.getServer().addEventListener(INIT_STATE, StateUpdateRequest.class, (client, data, ackSender) -> {
            String username = sessionService.getUsernameByClientId(client.getSessionId().toString());
            PlayerState playerPosition = gameEngine.getPlayerPosition(username);
            if (playerPosition == null) {
                playerPosition = new PlayerState(Math.random() * MAP_X, 0, Math.random() * MAP_Z);
                gameEngine.setPlayerPosition(username, playerPosition);
            }
            client.sendEvent(STATE, playerPosition);
            log.info("SOCKET.IO: Updating player state to: {}", data.getState());
        });
    }

    private void initDirectionListener() {
        nettyWebSocketServer.getServer().addEventListener(DIRECTION, RotationUpdateRequest.class, (client, data, ackSender) -> {
            String username = sessionService.getUsernameByClientId(client.getSessionId().toString());
            gameEngine.setPlayerDirection(username, data.getRotationY());
            log.info("SOCKET.IO: Updating player direction to: {}", data.getRotationY());
        });
    }

    private void scheduleCleanUpOldSessions() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<String> loggedInSessions = sessionService.getAllLoggedInSessions();
                Set<String> allActiveSessions = sessionService.getAllActiveSessions();
                for (String loggedInSession : loggedInSessions) {
                    if (!allActiveSessions.contains(loggedInSession)) {
                        String username = sessionService.getUsername(loggedInSession);
                        sessionService.removeSession(loggedInSession);
                        gameEngine.clearUsername(username);

                        log.info("cleanUpOldSessions \"{}\" has been logout! because of inactivity.", username);
                    }
                }
                sessionService.clearActiveSessions();
                log.info("cleanUpOldSessions - finished");
            } catch (Exception e) {
                log.error("cleanUpOldSessions.", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}
