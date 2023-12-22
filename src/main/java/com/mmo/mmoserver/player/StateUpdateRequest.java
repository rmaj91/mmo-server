package com.mmo.mmoserver.player;

import com.mmo.mmoserver.commons.PlayerMovementState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StateUpdateRequest {
    private PlayerMovementState state;
}
