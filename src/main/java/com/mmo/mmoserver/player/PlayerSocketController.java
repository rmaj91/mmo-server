package com.mmo.mmoserver.player;

import com.mmo.mmoserver.auth.SessionRepository;
import com.mmo.mmoserver.engine.GameEngine;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.mmo.mmoserver.commons.Events.DIRECTION;
import static com.mmo.mmoserver.commons.Events.STATE;

@Slf4j
@Component
public class PlayerSocketController {

    private final NettyWebSocketServer nettyWebSocketServer;
    private final SessionRepository sessionRepository;
    private final GameEngine gameEngine;

    public PlayerSocketController(NettyWebSocketServer nettyWebSocketServer, SessionRepository sessionRepository, GameEngine gameEngine) {
        this.nettyWebSocketServer = nettyWebSocketServer;
        this.sessionRepository = sessionRepository;
        this.gameEngine = gameEngine;

        initStateListener();
        initDirectionListener();
    }

    private void initStateListener() {
        nettyWebSocketServer.getServer().addEventListener(STATE, StateUpdateRequest.class, (client, data, ackSender) -> {
            String username = sessionRepository.clientIdToUsername.get(client.getSessionId().toString());
            gameEngine.setPlayerState(username, data.getState().toString());
            log.debug("SOCKET.IO: Updating player state to: {}", data.getState());
        });
    }

    private void initDirectionListener() {
        nettyWebSocketServer.getServer().addEventListener(DIRECTION, RotationUpdateRequest.class, (client, data, ackSender) -> {
            String username = sessionRepository.clientIdToUsername.get(client.getSessionId().toString());
            gameEngine.setPlayerDirection(username, data.getRotationY());
            log.debug("SOCKET.IO: Updating player direction to: {}", data.getRotationY());
        });
    }
}
