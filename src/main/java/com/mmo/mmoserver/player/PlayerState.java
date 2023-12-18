package com.mmo.mmoserver.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
