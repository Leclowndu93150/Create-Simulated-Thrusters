package com.leclowndu93150.create_simulated_thrusters.content.brassthruster;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BrassThrusterPartBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    private BlockPos controllerOffset = BlockPos.ZERO;
    private BrassThrusterRole role = BrassThrusterRole.FRAME;

    public BrassThrusterPartBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public BlockPos getControllerOffset() {
        return this.controllerOffset;
    }

    public BlockPos getControllerPos() {
        return this.worldPosition.subtract(this.controllerOffset);
    }

    public BrassThrusterRole getRole() {
        return this.role;
    }

    public void configure(final BlockPos offsetFromController, final BrassThrusterRole assignedRole) {
        this.controllerOffset = offsetFromController;
        this.role = assignedRole;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Nullable
    public BrassThrusterBlockEntity findController() {
        if (this.level == null) {
            return null;
        }
        final BlockEntity be = this.level.getBlockEntity(this.getControllerPos());
        return be instanceof BrassThrusterBlockEntity controller ? controller : null;
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        final BrassThrusterBlockEntity controller = this.findController();
        return controller != null && controller.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.writeThrusterData(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        this.writeThrusterData(tag);
        return tag;
    }

    private void writeThrusterData(final CompoundTag tag) {
        tag.putInt("ControllerOffsetX", this.controllerOffset.getX());
        tag.putInt("ControllerOffsetY", this.controllerOffset.getY());
        tag.putInt("ControllerOffsetZ", this.controllerOffset.getZ());
        tag.putString("Role", this.role.getSerializedName());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.controllerOffset = new BlockPos(
                tag.getInt("ControllerOffsetX"),
                tag.getInt("ControllerOffsetY"),
                tag.getInt("ControllerOffsetZ"));
        final String roleName = tag.getString("Role");
        this.role = BrassThrusterRole.FRAME;
        for (final BrassThrusterRole candidate : BrassThrusterRole.values()) {
            if (candidate.getSerializedName().equals(roleName)) {
                this.role = candidate;
                break;
            }
        }
    }
}
