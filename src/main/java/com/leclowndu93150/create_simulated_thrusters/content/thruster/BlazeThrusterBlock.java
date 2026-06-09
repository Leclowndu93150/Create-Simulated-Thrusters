package com.leclowndu93150.create_simulated_thrusters.content.thruster;

import com.leclowndu93150.create_simulated_thrusters.CreateSimulatedThrusters;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class BlazeThrusterBlock extends DirectionalBlock implements IBE<BlazeThrusterBlockEntity>, IWrenchable {

    public static final EnumProperty<HeatLevel> HEAT_LEVEL = EnumProperty.create("heat", HeatLevel.class);
    public static final MapCodec<BlazeThrusterBlock> CODEC = simpleCodec(BlazeThrusterBlock::new);

    public BlazeThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(HEAT_LEVEL, HeatLevel.NONE));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HEAT_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            facing = facing.getOpposite();
        return defaultBlockState().setValue(FACING, facing).setValue(HEAT_LEVEL, HeatLevel.NONE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> be.tryInsertFuel(stack, player, hand));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BlazeThrusterBlockEntity be)
            be.updateRedstonePaused();
    }

    @Override
    public Class<BlazeThrusterBlockEntity> getBlockEntityClass() {
        return BlazeThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BlazeThrusterBlockEntity> getBlockEntityType() {
        return CreateSimulatedThrusters.BLAZE_THRUSTER_BE.get();
    }

    public enum HeatLevel implements StringRepresentable {
        NONE, LOW, HIGH, BLUE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }

        public boolean isActive() {
            return this != NONE;
        }
    }
}
