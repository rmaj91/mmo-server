package com.mmo.mmoserver.player;

import com.mmo.mmoserver.auth.SessionService;
import com.mmo.mmoserver.engine.GameEngine;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.mmo.mmoserver.commons.Events.DIRECTION;
import static com.mmo.mmoserver.commons.Events.STATE;

@Slf4j
@Component
public class PlayerSocketController {

    private final NettyWebSocketServer nettyWebSocketServer;
    private final SessionService sessionService;
    private final GameEngine gameEngine;

    public PlayerSocketController(NettyWebSocketServer nettyWebSocketServer, SessionService sessionService, GameEngine gameEngine) {
        this.nettyWebSocketServer = nettyWebSocketServer;
        this.sessionService = sessionService;
        this.gameEngine = gameEngine;

        initStateListener();
        initDirectionListener();
    }

    private void initStateListener() {
        nettyWebSocketServer.getServer().addEventListener(STATE, StateUpdateRequest.class, (client, data, ackSender) -> {
            String username = sessionService.getUsernameByClientId(client.getSessionId().toString());
            gameEngine.setPlayerState(username, data.getState().toString());
            log.debug("SOCKET.IO: Updating player state to: {}", data.getState());
        });
    }

    private void initDirectionListener() {
        nettyWebSocketServer.getServer().addEventListener(DIRECTION, RotationUpdateRequest.class, (client, data, ackSender) -> {
            String username = sessionService.getUsernameByClientId(client.getSessionId().toString());
            gameEngine.setPlayerDirection(username, data.getRotationY());
            log.debug("SOCKET.IO: Updating player direction to: {}", data.getRotationY());
        });
    }
}
