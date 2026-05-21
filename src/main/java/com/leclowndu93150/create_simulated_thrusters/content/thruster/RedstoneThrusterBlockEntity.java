package com.leclowndu93150.create_simulated_thrusters.content.thruster;

import com.leclowndu93150.create_simulated_thrusters.Config;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.List;

public class RedstoneThrusterBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {
    private static final double NOZZLE_OFFSET_FROM_CENTER = 0.55d;

    public RedstoneThrusterBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public Direction getFacing() {
        return this.getBlockState().getValue(RedstoneThrusterBlock.FACING);
    }

    public int getRedstonePower() {
        return this.getBlockState().getValue(RedstoneThrusterBlock.POWER);
    }

    public double getThrottle() {
        return this.getRedstonePower() / 15.0d;
    }

    public double getSpeedScale() {
        return Math.min(1.0d, this.getRpm() / Config.redstoneThrusterMaxThrustRpm);
    }

    public double getRpm() {
        return Math.abs(this.getSpeed());
    }

    public double getCurrentAirflow() {
        if (!this.isOperational()) {
            return 0.0d;
        }
        return Config.redstoneThrusterAirflow * this.getThrottle() * this.getSpeedScale();
    }

    public boolean isOperational() {
        return this.getRpm() > 1.0e-3 && this.getRedstonePower() > 0 && !this.isOverStressed();
    }

    public double getCurrentThrust() {
        if (!this.isOperational()) {
            return 0.0d;
        }
        return Config.redstoneThrusterThrust * this.getThrottle() * this.getSpeedScale();
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        final double thrust = this.getCurrentThrust();
        if (thrust <= 0.0d) {
            return;
        }

        final Direction nozzleDirection = this.getFacing().getOpposite();
        final Direction thrustDirection = this.getFacing();
        final Vector3d nozzleDirectionLocal = new Vector3d(nozzleDirection.getStepX(), nozzleDirection.getStepY(), nozzleDirection.getStepZ());
        final Vector3d thrustDirectionLocal = new Vector3d(thrustDirection.getStepX(), thrustDirection.getStepY(), thrustDirection.getStepZ());
        final Vector3d pointLocal = new Vector3d(
                this.worldPosition.getX() + 0.5d,
                this.worldPosition.getY() + 0.5d,
                this.worldPosition.getZ() + 0.5d
        ).fma(NOZZLE_OFFSET_FROM_CENTER, nozzleDirectionLocal);
        final Vector3d impulseLocal = thrustDirectionLocal.mul(thrust * timeStep);

        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
        forceGroup.applyAndRecordPointForce(pointLocal, impulseLocal);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        final boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (added) {
            tooltip.add(CommonComponents.EMPTY);
        }

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.redstone_thruster")
                        .withStyle(ChatFormatting.GOLD))
                .forGoggles(tooltip);

        final ChatFormatting statusColor = this.isOperational() ? ChatFormatting.GREEN : ChatFormatting.RED;
        final String statusKey = this.isOverStressed()
                ? "overstressed"
                : this.getRedstonePower() <= 0
                ? "not_powered"
                : this.getRpm() <= 1.0e-3
                ? "not_rotating"
                : "working";

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.status")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.status." + statusKey)
                        .withStyle(statusColor))
                .forGoggles(tooltip, 1);

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.redstone_power")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number(this.getRedstonePower())
                        .style(ChatFormatting.RED))
                .text(ChatFormatting.DARK_GRAY, " / 15")
                .forGoggles(tooltip, 1);

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.thrust")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number(this.getCurrentThrust())
                        .text(" N")
                        .style(ChatFormatting.AQUA))
                .forGoggles(tooltip, 1);

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.airflow")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number(this.getCurrentAirflow())
                        .text(" m/s")
                        .style(ChatFormatting.AQUA))
                .forGoggles(tooltip, 1);

        return true;
    }
}
