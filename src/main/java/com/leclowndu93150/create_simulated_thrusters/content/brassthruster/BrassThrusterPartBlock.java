package com.leclowndu93150.create_simulated_thrusters.content.brassthruster;

import com.leclowndu93150.create_simulated_thrusters.CreateSimulatedThrusters;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class BrassThrusterPartBlock extends Block implements IBE<BrassThrusterPartBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public BrassThrusterPartBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public PushReaction getPistonPushReaction(final BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof BrassThrusterPartBlockEntity partBE)) {
            return InteractionResult.PASS;
        }
        final BlockPos controllerPos = partBE.getControllerPos();
        final BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof BrassThrusterBlock controllerBlock)) {
            return InteractionResult.PASS;
        }
        return controllerBlock.onWrenched(controllerState, redirectedContext(context, controllerPos));
    }

    @Override
    public InteractionResult onSneakWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof BrassThrusterPartBlockEntity partBE)) {
            return InteractionResult.PASS;
        }
        final BlockPos controllerPos = partBE.getControllerPos();
        final BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof BrassThrusterBlock controllerBlock)) {
            return InteractionResult.PASS;
        }
        return controllerBlock.onSneakWrenched(controllerState, redirectedContext(context, controllerPos));
    }

    @Override
    public BlockState rotate(final BlockState state, final net.minecraft.world.level.block.Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final net.minecraft.world.level.block.Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }
        if (!level.isClientSide()) {
            this.getBlockEntityOptional(level, pos).ifPresent(be -> {
                final BlockPos controllerPos = be.getControllerPos();
                BrassThrusterDismantle.dismantle(level, controllerPos, pos);
            });
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        this.getBlockEntityOptional(level, pos).ifPresent(be -> {
            final BrassThrusterRole role = be.getRole();
            if (role != BrassThrusterRole.GIMBAL_LEFT && role != BrassThrusterRole.GIMBAL_RIGHT) {
                return;
            }
            final BrassThrusterBlockEntity controller = be.findController();
            if (controller == null) {
                return;
            }
            final int signal = level.getBestNeighborSignal(pos);
            controller.setGimbalSignal(role, signal);
        });
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BrassThrusterPartBlockEntity partBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        final BlockPos controllerPos = partBE.getControllerPos();
        final BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof BrassThrusterBlock)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return controllerState.useItemOn(stack, level, player, hand, redirectedHit(hitResult, controllerPos));
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BrassThrusterPartBlockEntity partBE)) {
            return InteractionResult.PASS;
        }
        final BlockPos controllerPos = partBE.getControllerPos();
        final BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof BrassThrusterBlock)) {
            return InteractionResult.PASS;
        }
        return controllerState.useWithoutItem(level, player, redirectedHit(hitResult, controllerPos));
    }

    @Override
    protected void attack(final BlockState state, final Level level, final BlockPos pos, final Player player) {
        if (!(level.getBlockEntity(pos) instanceof BrassThrusterPartBlockEntity partBE)) {
            return;
        }
        final BlockPos controllerPos = partBE.getControllerPos();
        final BlockState controllerState = level.getBlockState(controllerPos);
        if (controllerState.getBlock() instanceof BrassThrusterBlock) {
            controllerState.attack(level, controllerPos, player);
        }
    }

    @Override
    public Class<BrassThrusterPartBlockEntity> getBlockEntityClass() {
        return BrassThrusterPartBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BrassThrusterPartBlockEntity> getBlockEntityType() {
        return CreateSimulatedThrusters.BRASS_THRUSTER_PART_BE.get();
    }

    private static BlockHitResult redirectedHit(final BlockHitResult hitResult, final BlockPos controllerPos) {
        return new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), controllerPos, hitResult.isInside());
    }

    private static UseOnContext redirectedContext(final UseOnContext context, final BlockPos controllerPos) {
        final BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), controllerPos, context.isInside());
        return new UseOnContext(context.getPlayer(), context.getHand(), hit);
    }
}
