package com.mmo.mmoserver.engine;

import com.mmo.mmoserver.player.PlayerState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameEngine {


    public PlayerState getPlayerState(String username) {
        PlayerState playerState = new PlayerState();
        playerState.setPx(0);
        playerState.setPy(0);
        playerState.setPz(0);
        return playerState;
    }

    public void setPlayerDirection(String username, double rotationY) {
        log.info("Updating rotationY for player \"{}\", to:\"{}\".", username, rotationY);
    }

    public void setPlayerState(String username, String state) {
        log.info("Updating state for player \"{}\", to: \"{}\".", username, state);
    }
}
