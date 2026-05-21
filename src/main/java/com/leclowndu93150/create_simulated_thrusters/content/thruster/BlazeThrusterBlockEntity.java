package com.leclowndu93150.create_simulated_thrusters.content.thruster;

import com.leclowndu93150.create_simulated_thrusters.Config;
import com.leclowndu93150.create_simulated_thrusters.content.thruster.BlazeThrusterBlock.HeatLevel;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import com.simibubi.create.api.registry.CreateDataMaps;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.List;

public class BlazeThrusterBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor, IHaveGoggleInformation {

    private static final int MAX_BURN_TICKS = 10000;
    private static final int INSERTION_THRESHOLD = 500;
    private static final double NOZZLE_OFFSET_FROM_CENTER = 0.55d;

    private double thrustPerTick;
    private int remainingBurnTicks;
    private HeatLevel heatLevel;
    private boolean creative;

    public BlazeThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        thrustPerTick = 0;
        remainingBurnTicks = 0;
        heatLevel = HeatLevel.NONE;
        creative = false;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this).setInsertionHandler(this::handleBeltInsertion));
    }

    private ItemStack handleBeltInsertion(TransportedItemStack transported, Direction side, boolean simulate) {
        ItemStack stack = transported.stack;
        if (simulate) {
            return canAcceptFuel(stack) ? ItemStack.EMPTY : stack;
        }
        ItemStack copy = stack.copy();
        tryInsertFuelAutomated(copy);
        return copy.isEmpty() ? ItemStack.EMPTY : copy;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!level.isClientSide && !creative && remainingBurnTicks > 0)
            notifyUpdate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide)
            return;
        if (getCurrentThrust() > 0)
            spawnParticles();

        if (creative || remainingBurnTicks <= 0)
            return;
        remainingBurnTicks--;
        if (remainingBurnTicks == 0) {
            thrustPerTick = 0;
            heatLevel = HeatLevel.NONE;
            creative = false;
            updateBlockState();
            notifyUpdate();
        }
    }

    private void spawnParticles() {
        ThrusterParticleEmitter.EmitterConfig config = switch (heatLevel) {
            case LOW  -> ThrusterParticleEmitter.LOW;
            case HIGH -> ThrusterParticleEmitter.HIGH;
            case BLUE -> ThrusterParticleEmitter.BLAZE;
            default   -> null;
        };
        if (config == null)
            return;

        Direction exhaust = getFacing().getOpposite();
        double plumeEndOffset = switch (heatLevel) {
            case LOW -> 1.25d;
            case HIGH, BLUE -> 2.25d;
            default -> NOZZLE_OFFSET_FROM_CENTER;
        };
        Vec3 localOrigin = new Vec3(
            worldPosition.getX() + 0.5 + exhaust.getStepX() * plumeEndOffset,
            worldPosition.getY() + 0.5 + exhaust.getStepY() * plumeEndOffset,
            worldPosition.getZ() + 0.5 + exhaust.getStepZ() * plumeEndOffset
        );

        if (!(level instanceof ServerLevel serverLevel))
            return;
        ThrusterParticleEmitter.emit(serverLevel, localOrigin, exhaust, config);
    }

    private boolean canAcceptFuel(ItemStack stack) {
        return resolveFuel(stack) != null;
    }

    private record FuelResult(int burnTime, HeatLevel level) {}

    private FuelResult resolveFuel(ItemStack stack) {
        if (AllItems.CREATIVE_BLAZE_CAKE.isIn(stack))
            return new FuelResult(-1, HeatLevel.BLUE);
        Holder<Item> holder = stack.getItem().builtInRegistryHolder();
        BlazeBurnerFuel superheated = holder.getData(CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS);
        BlazeBurnerFuel normal = holder.getData(CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS);
        if (superheated != null)
            return new FuelResult(superheated.burnTime(), HeatLevel.BLUE);
        if (AllItemTags.BLAZE_BURNER_FUEL_SPECIAL.matches(stack))
            return new FuelResult(3200, HeatLevel.BLUE);
        if (normal != null) {
            int t = normal.burnTime();
            return new FuelResult(t, t < 200 ? HeatLevel.LOW : HeatLevel.HIGH);
        }
        int burnTime = stack.getBurnTime(null);
        if (burnTime > 0)
            return new FuelResult(burnTime, burnTime < 200 ? HeatLevel.LOW : HeatLevel.HIGH);
        if (AllItemTags.BLAZE_BURNER_FUEL_REGULAR.matches(stack))
            return new FuelResult(1600, HeatLevel.HIGH);
        return null;
    }

    private void tryInsertFuelAutomated(ItemStack stack) {
        FuelResult fuel = resolveFuel(stack);
        if (fuel == null)
            return;
        if (fuel.burnTime() == -1) {
            creative = true;
            heatLevel = HeatLevel.BLUE;
            thrustPerTick = computeThrust(HeatLevel.BLUE);
            remainingBurnTicks = MAX_BURN_TICKS;
            updateBlockState();
            notifyUpdate();
            stack.shrink(1);
            return;
        }
        int newBurnTime = fuel.burnTime();
        HeatLevel newLevel = fuel.level();
        if (newLevel.ordinal() < heatLevel.ordinal() && remainingBurnTicks > 0)
            return;
        if (newLevel == heatLevel && remainingBurnTicks > 0) {
            if (remainingBurnTicks <= INSERTION_THRESHOLD)
                newBurnTime += remainingBurnTicks;
            else
                return;
        }
        heatLevel = newLevel;
        thrustPerTick = computeThrust(heatLevel);
        remainingBurnTicks = Math.min(newBurnTime, MAX_BURN_TICKS);
        updateBlockState();
        notifyUpdate();
        stack.shrink(1);
    }

    public ItemInteractionResult tryInsertFuel(ItemStack stack, Player player, InteractionHand hand) {
        if (stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (resolveFuel(stack) == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        ItemStack copy = player.isCreative() ? stack.copy() : stack;
        tryInsertFuelAutomated(copy);
        return ItemInteractionResult.SUCCESS;
    }

    private void updateBlockState() {
        BlockState state = getBlockState();
        if (state.getValue(BlazeThrusterBlock.HEAT_LEVEL) != heatLevel)
            level.setBlockAndUpdate(worldPosition, state.setValue(BlazeThrusterBlock.HEAT_LEVEL, heatLevel));
    }

    private static double computeThrust(HeatLevel level) {
        return switch (level) {
            case LOW  -> Config.blazeThrusterLowThrust;
            case HIGH -> Config.blazeThrusterHighThrust;
            case BLUE -> Config.blazeThrusterBlueThrust;
            default   -> 0;
        };
    }

    public double getCurrentThrust() {
        return (creative || remainingBurnTicks > 0) ? thrustPerTick : 0;
    }

    public HeatLevel getHeatLevel() {
        return heatLevel;
    }

    public Direction getFacing() {
        return getBlockState().getValue(BlazeThrusterBlock.FACING);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        double thrust = getCurrentThrust();
        if (thrust <= 0)
            return;

        Direction nozzleDir = getFacing().getOpposite();
        Direction thrustDir = getFacing();
        Vector3d nozzleDirV = new Vector3d(nozzleDir.getStepX(), nozzleDir.getStepY(), nozzleDir.getStepZ());
        Vector3d thrustDirV = new Vector3d(thrustDir.getStepX(), thrustDir.getStepY(), thrustDir.getStepZ());
        Vector3d point = new Vector3d(
                worldPosition.getX() + 0.5d,
                worldPosition.getY() + 0.5d,
                worldPosition.getZ() + 0.5d
        ).fma(NOZZLE_OFFSET_FROM_CENTER, nozzleDirV);
        Vector3d impulse = thrustDirV.mul(thrust * timeStep);

        QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
        forceGroup.applyAndRecordPointForce(point, impulse);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.blaze_thruster")
                        .withStyle(ChatFormatting.GOLD))
                .forGoggles(tooltip);

        boolean active = creative || remainingBurnTicks > 0;
        ChatFormatting statusColor = active ? ChatFormatting.GREEN : ChatFormatting.RED;
        String statusKey = active ? "working" : "no_fuel";

        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.status")
                        .withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(Component.translatable("create_simulated_thrusters.gui.goggles.status." + statusKey)
                        .withStyle(statusColor))
                .forGoggles(tooltip, 1);

        if (active) {
            ChatFormatting fuelColor = switch (heatLevel) {
                case LOW  -> ChatFormatting.YELLOW;
                case HIGH -> ChatFormatting.GOLD;
                case BLUE -> ChatFormatting.AQUA;
                default   -> ChatFormatting.GRAY;
            };
            CreateLang.builder()
                    .add(Component.translatable("create_simulated_thrusters.gui.goggles.fuel_type")
                            .withStyle(ChatFormatting.GRAY))
                    .text(ChatFormatting.GRAY, ": ")
                    .add(Component.translatable("create_simulated_thrusters.gui.goggles.fuel_type." + heatLevel.getSerializedName())
                            .withStyle(fuelColor))
                    .forGoggles(tooltip, 1);
        }

        if (active) {
            CreateLang.builder()
                    .add(Component.translatable("create_simulated_thrusters.gui.goggles.fuel_remaining")
                            .withStyle(ChatFormatting.GRAY))
                    .text(ChatFormatting.GRAY, ": ")
                    .add(creative
                            ? Component.literal("∞").withStyle(ChatFormatting.AQUA)
                            : CreateLang.number(remainingBurnTicks / 20.0).text(" s").style(ChatFormatting.GREEN).component())
                    .forGoggles(tooltip, 1);

            CreateLang.builder()
                    .add(Component.translatable("create_simulated_thrusters.gui.goggles.thrust")
                            .withStyle(ChatFormatting.GRAY))
                    .text(ChatFormatting.GRAY, ": ")
                    .add(CreateLang.number(getCurrentThrust())
                            .text(" N")
                            .style(ChatFormatting.AQUA))
                    .forGoggles(tooltip, 1);
        }

        return true;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putDouble("thrustPerTick", thrustPerTick);
        tag.putInt("remainingBurnTicks", remainingBurnTicks);
        tag.putInt("heatLevel", heatLevel.ordinal());
        tag.putBoolean("creative", creative);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        thrustPerTick = tag.getDouble("thrustPerTick");
        remainingBurnTicks = tag.getInt("remainingBurnTicks");
        int ord = tag.getInt("heatLevel");
        heatLevel = HeatLevel.values()[Math.min(ord, HeatLevel.values().length - 1)];
        creative = tag.getBoolean("creative");
        super.read(tag, registries, clientPacket);
    }
}
