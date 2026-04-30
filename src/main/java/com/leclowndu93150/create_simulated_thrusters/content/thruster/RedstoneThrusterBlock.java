package com.leclowndu93150.create_simulated_thrusters.content.thruster;

import com.leclowndu93150.create_simulated_thrusters.CreateSimulatedThrusters;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class RedstoneThrusterBlock extends DirectionalKineticBlock implements IBE<RedstoneThrusterBlockEntity>, IWrenchable {

    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public RedstoneThrusterBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        Direction preferred = this.getPreferredFacing(context);
        if (preferred == null) {
            preferred = context.getNearestLookingDirection();
        }
        final Direction facing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                ? preferred.getOpposite()
                : preferred;
        final int signal = context.getLevel().getBestNeighborSignal(context.getClickedPos());
        return this.defaultBlockState().setValue(FACING, facing).setValue(POWER, signal);
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        final int signal = level.getBestNeighborSignal(pos);
        if (state.getValue(POWER) != signal) {
            level.setBlock(pos, state.setValue(POWER, signal), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(final BlockState state) {
        return false;
    }

    @Override
    public Class<RedstoneThrusterBlockEntity> getBlockEntityClass() {
        return RedstoneThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RedstoneThrusterBlockEntity> getBlockEntityType() {
        return CreateSimulatedThrusters.REDSTONE_THRUSTER_BE.get();
    }
}
