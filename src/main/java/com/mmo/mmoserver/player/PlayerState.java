package com.mmo.mmoserver.player;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PlayerState {
    private String username;
    private double px; // p for position
    private double py;
    private double pz;

    private List<PlayerState> anotherPlayers = new ArrayList<>();

    public PlayerState(double px, double py, double pz) {
        this.px = px;
        this.py = py;
        this.pz = pz;
    }
}
