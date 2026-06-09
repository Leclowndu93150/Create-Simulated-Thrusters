package com.leclowndu93150.create_simulated_thrusters.content.brassthruster;

import com.leclowndu93150.create_simulated_thrusters.CreateSimulatedThrusters;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BrassThrusterBlock extends DirectionalKineticBlock implements IBE<BrassThrusterBlockEntity>, IWrenchable {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);

    public BrassThrusterBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(POWERED, false)
                .setValue(ROLL, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, ROLL);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        Direction preferred = this.getPreferredFacing(context);
        if (preferred == null) {
            preferred = context.getClickedFace().getOpposite();
        }
        final Direction facing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                ? preferred.getOpposite()
                : preferred;
        if (!this.canFitFootprint(context.getLevel(), context.getClickedPos(), facing, 0, context)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, false)
                .setValue(ROLL, 0);
    }

    private boolean canFitFootprint(final Level level, final BlockPos controllerPos, final Direction facing, final int roll, final BlockPlaceContext context) {
        final boolean[] ok = {true};
        BrassThrusterStructure.forEachPart(controllerPos, facing, roll, (partPos, offset, role) -> {
            if (role == BrassThrusterRole.CONTROLLER) {
                return;
            }
            if (!level.getWorldBorder().isWithinBounds(partPos)) {
                ok[0] = false;
                return;
            }
            final BlockState existing = level.getBlockState(partPos);
            final BlockPlaceContext partContext = BlockPlaceContext.at(context, partPos, context.getClickedFace());
            final BlockState partState = CreateSimulatedThrusters.BRASS_THRUSTER_PART.get().defaultBlockState()
                    .setValue(BrassThrusterPartBlock.FACING, facing);
            final CollisionContext collision = context.getPlayer() == null
                    ? CollisionContext.empty()
                    : CollisionContext.of(context.getPlayer());
            if (!existing.canBeReplaced(partContext) || !level.isUnobstructed(partState, partPos, collision)) {
                ok[0] = false;
            }
        });
        return ok[0];
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        final Direction facing = state.getValue(FACING);
        final int roll = state.getValue(ROLL);
        this.placeParts(level, pos, facing, roll);
        if (level.getBlockEntity(pos) instanceof BrassThrusterBlockEntity controller) {
            controller.scanGimbalSignals();
        }
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
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return BrassThrusterShapes.get();
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return BrassThrusterShapes.get();
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Direction facing = state.getValue(FACING);
        final int currentRoll = state.getValue(ROLL);
        final int newRoll = (currentRoll + 1) % 4;
        final BlockState rotated = state.setValue(ROLL, newRoll);

        if (!this.canRotateFootprint(level, pos, facing, currentRoll, facing, newRoll, context)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BrassThrusterDismantle.guarded(() -> {
            BrassThrusterStructure.forEachPart(pos, facing, currentRoll, (partPos, offset, role) -> {
                if (role == BrassThrusterRole.CONTROLLER) {
                    return;
                }
                final BlockState partState = level.getBlockState(partPos);
                if (partState.getBlock() instanceof BrassThrusterPartBlock) {
                    level.removeBlock(partPos, false);
                }
            });

            KineticBlockEntity.switchToBlockState(level, pos, this.updateAfterWrenched(rotated, context));
            this.placeParts(level, pos, facing, newRoll);
            if (level.getBlockEntity(pos) instanceof BrassThrusterBlockEntity controller) {
                controller.scanGimbalSignals();
            }
        });

        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    private boolean canRotateFootprint(final Level level, final BlockPos controllerPos, final Direction oldFacing, final int oldRoll, final Direction newFacing, final int newRoll, final UseOnContext context) {
        final BlockState partState = CreateSimulatedThrusters.BRASS_THRUSTER_PART.get().defaultBlockState()
                .setValue(BrassThrusterPartBlock.FACING, newFacing);
        final CollisionContext collision = context.getPlayer() == null
                ? CollisionContext.empty()
                : CollisionContext.of(context.getPlayer());
        final BlockPlaceContext basePlacementContext = new BlockPlaceContext(context);
        final boolean[] ok = {true};

        BrassThrusterStructure.forEachPart(controllerPos, newFacing, newRoll, (partPos, offset, role) -> {
            if (role == BrassThrusterRole.CONTROLLER) {
                return;
            }
            if (!level.getWorldBorder().isWithinBounds(partPos)) {
                ok[0] = false;
                return;
            }
            final BlockState existing = level.getBlockState(partPos);
            final boolean occupiedByCurrentStructure = this.isCurrentStructurePart(level, partPos, controllerPos, oldFacing, oldRoll);
            final BlockPlaceContext partContext = BlockPlaceContext.at(basePlacementContext, partPos, context.getClickedFace());
            if ((!occupiedByCurrentStructure && !existing.canBeReplaced(partContext)) || !level.isUnobstructed(partState, partPos, collision)) {
                ok[0] = false;
            }
        });

        return ok[0];
    }

    private boolean isCurrentStructurePart(final Level level, final BlockPos partPos, final BlockPos controllerPos, final Direction facing, final int roll) {
        final boolean[] current = {false};
        BrassThrusterStructure.forEachPart(controllerPos, facing, roll, (existingPos, offset, role) -> {
            if (current[0] || role == BrassThrusterRole.CONTROLLER || !existingPos.equals(partPos)) {
                return;
            }
            current[0] = level.getBlockState(existingPos).getBlock() instanceof BrassThrusterPartBlock;
        });
        return current[0];
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            BrassThrusterDismantle.dismantle(level, pos, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<BrassThrusterBlockEntity> getBlockEntityClass() {
        return BrassThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BrassThrusterBlockEntity> getBlockEntityType() {
        return CreateSimulatedThrusters.BRASS_THRUSTER_BE.get();
    }

    private void placeParts(final Level level, final BlockPos pos, final Direction facing, final int roll) {
        final BlockState partState = CreateSimulatedThrusters.BRASS_THRUSTER_PART.get().defaultBlockState()
                .setValue(BrassThrusterPartBlock.FACING, facing);
        BrassThrusterStructure.forEachPart(pos, facing, roll, (partPos, offset, role) -> {
            if (role == BrassThrusterRole.CONTROLLER) {
                return;
            }
            level.setBlock(partPos, partState, Block.UPDATE_ALL);
            if (level.getBlockEntity(partPos) instanceof BrassThrusterPartBlockEntity partBE) {
                partBE.configure(offset, role);
            }
        });
    }
}
