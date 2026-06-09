package com.leclowndu93150.create_simulated_thrusters.content.brassthruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BrassThrusterDismantle {

    private static final ThreadLocal<Boolean> DISMANTLING = ThreadLocal.withInitial(() -> false);

    private BrassThrusterDismantle() {
    }

    public static boolean isDismantling() {
        return DISMANTLING.get();
    }

    public static void guarded(final Runnable action) {
        if (DISMANTLING.get()) {
            action.run();
            return;
        }
        DISMANTLING.set(true);
        try {
            action.run();
        } finally {
            DISMANTLING.set(false);
        }
    }

    public static void dismantle(final Level level, final BlockPos controllerPos, final BlockPos breakOrigin) {
        if (DISMANTLING.get()) {
            return;
        }
        guarded(() -> {
            final BlockEntity controllerBE = level.getBlockEntity(controllerPos);
            Direction facing = null;
            int roll = 0;
            if (controllerBE instanceof BrassThrusterBlockEntity controller) {
                facing = controller.getFacing();
                roll = controller.getRoll();
            } else {
                final BlockState controllerState = level.getBlockState(controllerPos);
                if (controllerState.getBlock() instanceof BrassThrusterBlock) {
                    if (controllerState.hasProperty(BrassThrusterBlock.FACING)) {
                        facing = controllerState.getValue(BrassThrusterBlock.FACING);
                    }
                    if (controllerState.hasProperty(BrassThrusterBlock.ROLL)) {
                        roll = controllerState.getValue(BrassThrusterBlock.ROLL);
                    }
                }
            }

            if (facing == null) {
                level.removeBlock(breakOrigin, false);
                return;
            }

            BrassThrusterStructure.forEachPart(controllerPos, facing, roll, (partPos, offset, role) -> {
                if (partPos.equals(breakOrigin)) {
                    return;
                }
                final BlockState state = level.getBlockState(partPos);
                if (state.getBlock() instanceof BrassThrusterBlock || state.getBlock() instanceof BrassThrusterPartBlock) {
                    level.destroyBlock(partPos, false);
                }
            });
        });
    }
}
