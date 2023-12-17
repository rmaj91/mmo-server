package com.mmo.mmoserver.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerState {
    private double px; // p for position
    private double py;
    private double pz;
}
