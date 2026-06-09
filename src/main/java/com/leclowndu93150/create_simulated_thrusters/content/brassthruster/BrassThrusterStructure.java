package com.leclowndu93150.create_simulated_thrusters.content.brassthruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class BrassThrusterStructure {

    private BrassThrusterStructure() {
    }

    public static Direction baseRightFor(final Direction facing) {
        return switch (facing.getAxis()) {
            case X -> Direction.SOUTH;
            case Y -> Direction.EAST;
            case Z -> Direction.EAST;
        };
    }

    public static Direction baseUpFor(final Direction facing) {
        return switch (facing.getAxis()) {
            case X -> Direction.UP;
            case Y -> Direction.SOUTH;
            case Z -> Direction.UP;
        };
    }

    public static Direction rightFor(final Direction facing, final int roll) {
        final int normalized = Math.floorMod(roll, 4);
        Direction right = baseRightFor(facing);
        for (int i = 0; i < normalized; i++) {
            right = right.getCounterClockWise(facing.getAxis());
        }
        return right;
    }

    public static Direction upFor(final Direction facing, final int roll) {
        final int normalized = Math.floorMod(roll, 4);
        Direction up = baseUpFor(facing);
        for (int i = 0; i < normalized; i++) {
            up = up.getCounterClockWise(facing.getAxis());
        }
        return up;
    }

    public static Direction rightFor(final Direction facing) {
        return rightFor(facing, 0);
    }

    public static Direction upFor(final Direction facing) {
        return upFor(facing, 0);
    }

    public static BlockPos offsetInPanel(final Direction facing, final int roll, final int rightSteps, final int upSteps) {
        final Direction right = rightFor(facing, roll);
        final Direction up = upFor(facing, roll);
        return new BlockPos(
                right.getStepX() * rightSteps + up.getStepX() * upSteps,
                right.getStepY() * rightSteps + up.getStepY() * upSteps,
                right.getStepZ() * rightSteps + up.getStepZ() * upSteps
        );
    }

    public static BlockPos offsetInPanel(final Direction facing, final int rightSteps, final int upSteps) {
        return offsetInPanel(facing, 0, rightSteps, upSteps);
    }

    public static BlockPos offsetInVolume(final Direction facing, final int roll, final int rightSteps, final int upSteps, final int depthSteps) {
        final Direction nozzle = facing.getOpposite();
        final BlockPos panelOffset = offsetInPanel(facing, roll, rightSteps, upSteps);
        return panelOffset.relative(nozzle, depthSteps);
    }

    public static BrassThrusterRole roleFor(final int rightSteps, final int upSteps, final int depthSteps) {
        if (depthSteps != 0) {
            return BrassThrusterRole.FRAME;
        }
        if (rightSteps == 0 && upSteps == 0) {
            return BrassThrusterRole.CONTROLLER;
        }
        if (upSteps == 0 && rightSteps == -1) {
            return BrassThrusterRole.GIMBAL_LEFT;
        }
        if (upSteps == 0 && rightSteps == 1) {
            return BrassThrusterRole.GIMBAL_RIGHT;
        }
        return BrassThrusterRole.FRAME;
    }

    public static void forEachPart(final BlockPos controllerPos, final Direction facing, final int roll, final PartConsumer consumer) {
        for (int d = 0; d <= 2; d++) {
            for (int r = -1; r <= 1; r++) {
                for (int u = -1; u <= 1; u++) {
                    final BlockPos offset = offsetInVolume(facing, roll, r, u, d);
                    final BlockPos partPos = controllerPos.offset(offset);
                    consumer.accept(partPos, offset, roleFor(r, u, d));
                }
            }
        }
    }

    public static void forEachPart(final BlockPos controllerPos, final Direction facing, final PartConsumer consumer) {
        forEachPart(controllerPos, facing, 0, consumer);
    }

    @FunctionalInterface
    public interface PartConsumer {
        void accept(BlockPos partPos, BlockPos offsetFromController, BrassThrusterRole role);
    }
}
