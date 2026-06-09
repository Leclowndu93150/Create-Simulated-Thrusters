package com.leclowndu93150.create_simulated_thrusters.content.brassthruster;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BrassThrusterShapes {

    private BrassThrusterShapes() {
    }

    public static VoxelShape get() {
        return Shapes.block();
    }
}
