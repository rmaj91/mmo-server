package com.mmo.mmoserver.engine;

import com.corundumstudio.socketio.SocketIOClient;
import com.mmo.mmoserver.auth.SessionService;
import com.mmo.mmoserver.mobs.Mob;
import com.mmo.mmoserver.player.PlayerState;
import com.mmo.mmoserver.websockets.NettyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.mmo.mmoserver.GameConfig.MAP_X;
import static com.mmo.mmoserver.GameConfig.MAP_Z;
import static com.mmo.mmoserver.commons.Events.STATE;

@Slf4j
@Component
public class GameEngine {

    private final Map<String, PlayerState> userToPosition = new HashMap<>();
    private final Map<String, String> userToState = new HashMap<>();
    private final Map<String, Double> userToDirection = new HashMap<>();

    private final Map<String, String> inCommingMeleeAttacks = new HashMap<>(); //username to mob
    private final Map<String, String> inCommingRangedAttacks = new HashMap<>();

    private final NettyWebSocketServer nettyWebSocketServer;
    private final SessionService sessionService;

    private final List<Mob> monsters = new ArrayList<>();

    public GameEngine(NettyWebSocketServer nettyWebSocketServer,
                      SessionService sessionService) {
        this.nettyWebSocketServer = nettyWebSocketServer;
        this.sessionService = sessionService;

        addMob(Mob.create("mob_1", 7));
        addMob(Mob.create("mob_2", 7));
        addMob(Mob.create("mob_3", 7));
        addMob(Mob.create("mob_4", 7));
        addMob(Mob.create("mob_5", 7));
        addMob(Mob.create("mob_6", 7));
        start();
    }

    private synchronized void addMob(Mob mob) {
        monsters.add(mob);
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
        List<PlayerState> monsters = new ArrayList<>();
        for (Mob monster : this.monsters) {
            PlayerState playerState = new PlayerState();
            playerState.setUsername(monster.getName());
            playerState.setPx(monster.getPx());
            playerState.setPy(monster.getPy());
            playerState.setPz(monster.getPz());
            playerState.setCombat(monster.isCombat());
            monsters.add(playerState);
        }

        userToPosition.forEach((username, position) -> {
            PlayerState toReturn = new PlayerState(position.getPx(), position.getPy(), position.getPz());
            userToPosition.forEach((u,p) -> {
                if (!u.equals(username)) {
                    PlayerState toAdd = new PlayerState(p.getPx(), p.getPy(), p.getPz());
                    toAdd.setUsername(u);
                    toAdd.setRotationY(userToDirection.get(u));
                    toReturn.getAnotherPlayers().add(toAdd);
                }
            });
            toReturn.setMonsters(monsters);

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
                    PlayerState position = userToPosition.getOrDefault(k, new PlayerState(Math.random() * MAP_X, 0 , Math.random() * MAP_Z));
                    Double direction = userToDirection.getOrDefault(k, 0d);

                    //apply magic logic
                    // Move 1 unit in the direction of rotation
                    double deltaX = 0.3 * Math.cos(direction);
                    double deltaZ = 0.3 * Math.sin(direction);
                    // Calculate the new position
                    double newX = position.getPx() + deltaX;
                    if (newX > MAP_X) {
                        newX = MAP_X;
                    }
                    if (newX < 0 ) {
                        newX = 0;
                    }
                    double newZ = position.getPz() - deltaZ;
                    if (newZ > MAP_Z) {
                        newZ = MAP_Z;
                    }
                    if (newZ < 0 ) {
                        newZ = 0;
                    }

                    PlayerState newState = new PlayerState(newX, position.getPy(), newZ);
                    log.info("new state: {}, {}, {}", newX, position.getPy(), newZ);
                    userToPosition.put(k, newState);
                }
                else {
                    userToPosition.putIfAbsent(k, new PlayerState(Math.random() * MAP_X, 0 , Math.random() * MAP_Z));
                    usersToAddIdle.add(k);
                }
            });
            for (String toAdd : usersToAddIdle) {
                userToState.put(toAdd, "idle");
            }

            //mobs behaviour
            List<Mob> killed = new ArrayList<>();
            for (Mob monster : monsters) {
                for (Map.Entry<String, PlayerState> entry : userToPosition.entrySet()) {
                    PlayerState player = entry.getValue();
                    double mobPlayerDistance = Math.sqrt(Math.pow(monster.getPx() - player.getPx(), 2) + Math.pow(monster.getPz() - player.getPz(), 2));
                    if (mobPlayerDistance < monster.getAggroRange() && mobPlayerDistance > 1) {
                        log.info("Monster {} is comming to {}", monster.getName(), player.getUsername());
                        moveMonsterInPlayersDirection(monster, player);
                    }
                    if (mobPlayerDistance < monster.getAggroRange() ) {
                        monster.setCombat(true);
                    } else {
                        monster.setCombat(false);
                    }

                    //handling melee attacks
                    if (inCommingMeleeAttacks.containsKey(entry.getKey()) && inCommingMeleeAttacks.get(entry.getKey()).equals(monster.getName())) {
                        if (mobPlayerDistance < 3) {
                            //attack
                            killed.add(monster);
                        }
                        inCommingMeleeAttacks.remove(entry.getKey());
                    }

                    //handling ranged attacks
                    if (inCommingRangedAttacks.containsKey(entry.getKey()) && inCommingRangedAttacks.get(entry.getKey()).equals(monster.getName())) {
                        if (mobPlayerDistance > 4 && mobPlayerDistance < 11) {
                            //attack
                            killed.add(monster);
                        }
                        inCommingRangedAttacks.remove(entry.getKey());
                    }
                }
            }
            monsters.removeAll(killed);
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

    private void moveMonsterInPlayersDirection(Mob monster, PlayerState player) {
        // Example points
        var x1 = monster.getPx();
        var z1 = monster.getPz();
        var x2 = player.getPx();
        var z2 = player.getPz();

        // Calculate vector from point 1 to point 2
        var dx = x2 - x1;
        var dz = z2 - z1;

        // Calculate magnitude of the vector
        var magnitude = Math.sqrt(dx * dx + dz * dz);

        // Normalize the vector
        var normalizedDx = dx / magnitude;
        var normalizedDz = dz / magnitude;

        // Define the distance to move (1 unit in this case)
        var distanceToMove = 0.1;

        // Calculate the new coordinates for point 1
        var newX1 = x1 + normalizedDx * distanceToMove;
        var newZ1 = z1 + normalizedDz * distanceToMove;

        monster.setPx(newX1);
        monster.setPz(newZ1);
    }

    public synchronized void setMobMeleeAttack(String username, String mob) {
        inCommingMeleeAttacks.put(username, mob);
    }

    public synchronized void setMobRangedAttack(String username, String mob) {
        inCommingRangedAttacks.put(username, mob);
    }
}
