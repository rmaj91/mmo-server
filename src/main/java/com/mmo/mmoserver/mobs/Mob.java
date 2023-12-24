package com.mmo.mmoserver.mobs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import static com.mmo.mmoserver.GameConfig.MAP_X;
import static com.mmo.mmoserver.GameConfig.MAP_Z;

@Getter
@Setter
@AllArgsConstructor
public class Mob {

    private String name;
    private double px;
    private double py;
    private double pz;
    private double aggroRange;

    public static Mob create(String name, double aggroRange) {
        return new Mob(name, Math.random() * MAP_X, 0, Math.random() * MAP_Z, aggroRange);
    }
}
