package com.leclowndu93150.create_simulated_thrusters.content.brassthruster;

import com.leclowndu93150.create_simulated_thrusters.Config;
import com.leclowndu93150.create_simulated_thrusters.content.thruster.ThrusterParticleEmitter;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.List;

public class BrassThrusterBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {

    private static final double NOZZLE_SURFACE_OFFSET = 2.5d;
    private static final double MIN_PLUME_LENGTH = 0.2d;

    private int gimbalLeftPower = 0;
    private int gimbalRightPower = 0;

    public BrassThrusterBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public Direction getFacing() {
        return this.getBlockState().getValue(BrassThrusterBlock.FACING);
    }

    public int getRoll() {
        return this.getBlockState().getValue(BrassThrusterBlock.ROLL);
    }

    public double getRpm() {
        return Math.abs(this.getSpeed());
    }

    public double getSpeedScale() {
        return Math.min(1.0d, this.getRpm() / Config.redstoneThrusterMaxThrustRpm);
    }

    public boolean isOperational() {
        return this.getRpm() > 1.0e-3 && !this.isOverStressed();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level != null && !this.level.isClientSide() && this.isOperational()) {
            this.spawnParticles();
        }
    }

    @Override
    public void onSpeedChanged(final float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        this.updatePoweredState();
    }

    private void updatePoweredState() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        final BlockState state = this.getBlockState();
        if (!state.hasProperty(BrassThrusterBlock.POWERED)) {
            return;
        }
        final boolean powered = this.isOperational();
        if (state.getValue(BrassThrusterBlock.POWERED) != powered) {
            this.level.setBlock(this.worldPosition, state.setValue(BrassThrusterBlock.POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    private void spawnParticles() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        final Direction exhaust = this.getFacing().getOpposite();
        final double plumeLength = MIN_PLUME_LENGTH + (1.0d - MIN_PLUME_LENGTH) * this.getSpeedScale();
        final Vec3 origin = new Vec3(
                this.worldPosition.getX() + 0.5d + exhaust.getStepX() * NOZZLE_SURFACE_OFFSET,
                this.worldPosition.getY() + 0.5d + exhaust.getStepY() * NOZZLE_SURFACE_OFFSET,
                this.worldPosition.getZ() + 0.5d + exhaust.getStepZ() * NOZZLE_SURFACE_OFFSET
        );
        ThrusterParticleEmitter.emit(serverLevel, origin, exhaust, ThrusterParticleEmitter.BRASS, (float) plumeLength);
    }

    public double getCurrentThrust() {
        if (!this.isOperational()) {
            return 0.0d;
        }
        return Config.brassThrusterThrust * this.getSpeedScale();
    }

    public double getCurrentAirflow() {
        if (!this.isOperational()) {
            return 0.0d;
        }
        return Config.brassThrusterAirflow * this.getSpeedScale();
    }

    public int getGimbalLeftPower() {
        return this.gimbalLeftPower;
    }

    public int getGimbalRightPower() {
        return this.gimbalRightPower;
    }

    public double getGimbalAngleDegrees() {
        return (this.gimbalRightPower - this.gimbalLeftPower) / 15.0d * Config.brassThrusterMaxGimbalDegrees;
    }

    public void setGimbalSignal(final BrassThrusterRole role, final int signal) {
        final int clamped = Math.max(0, Math.min(15, signal));
        if (role == BrassThrusterRole.GIMBAL_LEFT) {
            if (this.gimbalLeftPower == clamped) {
                return;
            }
            this.gimbalLeftPower = clamped;
        } else if (role == BrassThrusterRole.GIMBAL_RIGHT) {
            if (this.gimbalRightPower == clamped) {
                return;
            }
            this.gimbalRightPower = clamped;
        } else {
            return;
        }
        this.notifyUpdate();
    }

    public void scanGimbalSignals() {
        if (this.level == null) {
            return;
        }
        final Direction facing = this.getFacing();
        final int roll = this.getRoll();
        final BlockPos leftPos = this.worldPosition.offset(BrassThrusterStructure.offsetInPanel(facing, roll, -1, 0));
        final BlockPos rightPos = this.worldPosition.offset(BrassThrusterStructure.offsetInPanel(facing, roll, 1, 0));
        this.setGimbalSignal(BrassThrusterRole.GIMBAL_LEFT, this.level.getBestNeighborSignal(leftPos));
        this.setGimbalSignal(BrassThrusterRole.GIMBAL_RIGHT, this.level.getBestNeighborSignal(rightPos));
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        final double thrust = this.getCurrentThrust();
        if (thrust <= 0.0d) {
            return;
        }
        final Direction facing = this.getFacing();
        final Direction nozzleDirection = facing.getOpposite();
        final Vector3d thrustDirectionLocal = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());

        final double angleRad = Math.toRadians(this.getGimbalAngleDegrees());
        if (Math.abs(angleRad) > 1.0e-6) {
            final Direction upDir = BrassThrusterStructure.upFor(facing, this.getRoll());
            final Vector3d upAxis = new Vector3d(upDir.getStepX(), upDir.getStepY(), upDir.getStepZ());
            thrustDirectionLocal.rotateAxis(angleRad, upAxis.x, upAxis.y, upAxis.z);
        }

        final Vector3d pointLocal = new Vector3d(
                this.worldPosition.getX() + 0.5d,
                this.worldPosition.getY() + 0.5d,
                this.worldPosition.getZ() + 0.5d
        ).fma(0.55d, new Vector3d(nozzleDirection.getStepX(), nozzleDirection.getStepY(), nozzleDirection.getStepZ()));

        final Vector3d impulseLocal = thrustDirectionLocal.mul(thrust * timeStep);
        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
        forceGroup.applyAndRecordPointForce(pointLocal, impulseLocal);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("GimbalLeftPower", this.gimbalLeftPower);
        tag.putInt("GimbalRightPower", this.gimbalRightPower);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.gimbalLeftPower = tag.getInt("GimbalLeftPower");
        this.gimbalRightPower = tag.getInt("GimbalRightPower");
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        final boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (added) {
            tooltip.add(CommonComponents.EMPTY);
        }

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.brass_thruster")
                        .withStyle(ChatFormatting.GOLD))
                .forGoggles(tooltip);

        final ChatFormatting statusColor = this.isOperational() ? ChatFormatting.GREEN : ChatFormatting.RED;
        final String statusKey = this.isOverStressed()
                ? "overstressed"
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
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.rpm")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number(this.getRpm()).style(ChatFormatting.AQUA))
                .text(ChatFormatting.DARK_GRAY, " / " + (int) Config.redstoneThrusterMaxThrustRpm)
                .forGoggles(tooltip, 1);

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.thrust")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number(this.getCurrentThrust())
                        .text(" pN")
                        .style(ChatFormatting.AQUA))
                .text(ChatFormatting.DARK_GRAY, " / ")
                .add(CreateLang.number(Config.brassThrusterThrust)
                        .text(" pN")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        if (Config.brassThrusterAirflow > 0) {
            CreateLang.builder()
                    .add(Component.translatable("create_simulated_thrusters.gui.goggles.airflow")
                            .withStyle(ChatFormatting.GRAY))
                    .text(ChatFormatting.GRAY, ": ")
                    .add(CreateLang.number(this.getCurrentAirflow())
                            .text(" m/s")
                            .style(ChatFormatting.AQUA))
                    .forGoggles(tooltip, 1);
        }

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.gimbal_input")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number(this.gimbalLeftPower).style(ChatFormatting.RED))
                .text(ChatFormatting.DARK_GRAY, " / ")
                .add(CreateLang.number(this.gimbalRightPower).style(ChatFormatting.RED))
                .forGoggles(tooltip, 1);

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.gimbal")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number(this.getGimbalAngleDegrees())
                        .text("°")
                        .style(ChatFormatting.AQUA))
                .text(ChatFormatting.DARK_GRAY, " / ±" + (int) Config.brassThrusterMaxGimbalDegrees + "°")
                .forGoggles(tooltip, 1);

        return true;
    }
}
