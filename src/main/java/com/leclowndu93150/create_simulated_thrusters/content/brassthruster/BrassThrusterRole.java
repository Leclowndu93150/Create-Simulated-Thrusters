package com.leclowndu93150.create_simulated_thrusters.content.brassthruster;

import net.minecraft.util.StringRepresentable;

public enum BrassThrusterRole implements StringRepresentable {
    CONTROLLER("controller"),
    GIMBAL_LEFT("gimbal_left"),
    GIMBAL_RIGHT("gimbal_right"),
    FRAME("frame");

    private final String name;

    BrassThrusterRole(final String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
