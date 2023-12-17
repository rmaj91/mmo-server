package com.mmo.mmoserver.player;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StateUpdateRequest {
    // walk or idle
    private String state;
}
