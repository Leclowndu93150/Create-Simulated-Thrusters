package com.leclowndu93150.create_simulated_thrusters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.leclowndu93150.create_simulated_thrusters.content.thruster.RedstoneThrusterBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RedstoneThrusterRenderer extends KineticBlockEntityRenderer<RedstoneThrusterBlockEntity> {

    public RedstoneThrusterRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(final RedstoneThrusterBlockEntity be, final float partialTicks, final PoseStack poseStack, final MultiBufferSource buffer, final int light, final int overlay) {
        final BlockState state = this.getRenderedBlockState(be);
        final Direction socketSide = be.getFacing();
        final int shaftLight = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(socketSide));
        renderRotatingBuffer(be, this.getRotatedModel(be, state), poseStack, buffer.getBuffer(RenderType.cutoutMipped()), shaftLight);

        if (be.isOperational()) {
            final Direction exhaustSide = socketSide.getOpposite();
            final TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(ClientModEvents.THRUST_SPRITE);
            this.renderThrustPlume(poseStack, buffer.getBuffer(Sheets.cutoutBlockSheet()), sprite, exhaustSide, LightTexture.FULL_BRIGHT, overlay);
        }
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final RedstoneThrusterBlockEntity be, final BlockState state) {
        final Direction socketSide = be.getFacing();
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, socketSide);
    }

    private void renderThrustPlume(final PoseStack poseStack, final VertexConsumer consumer, final TextureAtlasSprite sprite, final Direction exhaustSide, final int light, final int overlay) {
        final Vector3f direction = step(exhaustSide);
        final Vector3f up = exhaustSide.getAxis() == Direction.Axis.Y ? step(Direction.SOUTH) : step(Direction.UP);
        final Vector3f right = exhaustSide.getAxis() == Direction.Axis.X ? step(Direction.SOUTH) : step(Direction.EAST);
        final Vector3f origin = new Vector3f(0.5f, 0.5f, 0.5f).add(new Vector3f(direction).mul(0.5f));
        final PoseStack.Pose pose = poseStack.last();

        this.renderDoubleSidedPlane(consumer, pose, sprite, origin, direction, right, up, 0.0f, -0.4375f, 0.4375f, light, overlay);
        this.renderDoubleSidedPlane(consumer, pose, sprite, origin, direction, up, right, 0.0f, -0.4375f, 0.4375f, light, overlay);
    }

    private void renderDoubleSidedPlane(final VertexConsumer consumer, final PoseStack.Pose pose, final TextureAtlasSprite sprite, final Vector3f origin, final Vector3f direction,
                                        final Vector3f right, final Vector3f up, final float rightOffset, final float upMin, final float upMax,
                                        final int light, final int overlay) {
        final Vector3f a = point(origin, direction, right, up, 0.0f, rightOffset, upMin);
        final Vector3f b = point(origin, direction, right, up, 0.0f, rightOffset, upMax);
        final Vector3f c = point(origin, direction, right, up, 1.0f, rightOffset, upMax);
        final Vector3f d = point(origin, direction, right, up, 1.0f, rightOffset, upMin);

        this.vertex(consumer, pose, sprite, a, 1.0f, 0.0f, light, overlay);
        this.vertex(consumer, pose, sprite, b, 1.0f, 1.0f, light, overlay);
        this.vertex(consumer, pose, sprite, c, 0.0f, 1.0f, light, overlay);
        this.vertex(consumer, pose, sprite, d, 0.0f, 0.0f, light, overlay);

        this.vertex(consumer, pose, sprite, d, 0.0f, 0.0f, light, overlay);
        this.vertex(consumer, pose, sprite, c, 0.0f, 1.0f, light, overlay);
        this.vertex(consumer, pose, sprite, b, 1.0f, 1.0f, light, overlay);
        this.vertex(consumer, pose, sprite, a, 1.0f, 0.0f, light, overlay);
    }

    private void vertex(final VertexConsumer consumer, final PoseStack.Pose pose, final TextureAtlasSprite sprite, final Vector3f point, final float u, final float v, final int light, final int overlay) {
        final Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, point.x(), point.y(), point.z())
                .setColor(255, 255, 255, 255)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    private static Vector3f point(final Vector3f origin, final Vector3f direction, final Vector3f right, final Vector3f up,
                                  final float along, final float rightOffset, final float upOffset) {
        return new Vector3f(origin)
                .add(new Vector3f(direction).mul(along))
                .add(new Vector3f(right).mul(rightOffset))
                .add(new Vector3f(up).mul(upOffset));
    }

    private static Vector3f step(final Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
