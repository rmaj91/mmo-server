package com.mmo.mmoserver.engine;

import com.mmo.mmoserver.player.PlayerState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GameEngine {

    private Map<String, PlayerState> userToPosition = new HashMap<>();
    private Map<String, String> userToState = new HashMap<>();
    private Map<String, Double> userToDirection = new HashMap<>();

    public GameEngine() {
        start();
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

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        new Thread(runnable).start();
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
