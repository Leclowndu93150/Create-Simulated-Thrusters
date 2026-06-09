package com.leclowndu93150.create_simulated_thrusters.client;

import com.leclowndu93150.create_simulated_thrusters.content.brassthruster.BrassThrusterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BrassThrusterRenderer extends KineticBlockEntityRenderer<BrassThrusterBlockEntity> {

    private static final float MAX_PLUME_HALF_WIDTH = 1.5f;
    private static final float MAX_PLUME_LENGTH = 6.0f;

    public BrassThrusterRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(final BrassThrusterBlockEntity be, final float partialTicks, final PoseStack poseStack, final MultiBufferSource buffer, final int light, final int overlay) {
        final BlockState state = this.getRenderedBlockState(be);
        final Direction socketSide = be.getFacing();
        final int shaftLight = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(socketSide));
        renderRotatingBuffer(be, this.getRotatedModel(be, state), poseStack, buffer.getBuffer(RenderType.cutoutMipped()), shaftLight);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final BrassThrusterBlockEntity be, final BlockState state) {
        final Direction socketSide = be.getFacing();
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, socketSide);
    }

    @Override
    public AABB getRenderBoundingBox(final BrassThrusterBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(MAX_PLUME_LENGTH + MAX_PLUME_HALF_WIDTH);
    }
}
