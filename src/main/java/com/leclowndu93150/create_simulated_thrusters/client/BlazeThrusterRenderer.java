package com.leclowndu93150.create_simulated_thrusters.client;

import com.leclowndu93150.create_simulated_thrusters.content.thruster.BlazeThrusterBlock;
import com.leclowndu93150.create_simulated_thrusters.content.thruster.BlazeThrusterBlock.HeatLevel;
import com.leclowndu93150.create_simulated_thrusters.content.thruster.BlazeThrusterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class BlazeThrusterRenderer implements BlockEntityRenderer<BlazeThrusterBlockEntity> {

    private static final int FRAME_TICKS = 2;

    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath("create_simulated_thrusters", path);
    }

    private static final ResourceLocation[][] SPRITES = {
        { loc("block/blaze_thrust_low_1"),  loc("block/blaze_thrust_low_2")  },
        { loc("block/blaze_thrust_1"),       loc("block/blaze_thrust_2")       },
        { loc("block/blaze_thrust_blue_1"),  loc("block/blaze_thrust_blue_2")  },
    };

    public BlazeThrusterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BlazeThrusterBlockEntity be, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        if (be.isRedstonePaused())
            return;

        BlockState state = be.getBlockState();
        HeatLevel heat = state.getValue(BlazeThrusterBlock.HEAT_LEVEL);
        if (!heat.isActive())
            return;

        int spriteSet = switch (heat) {
            case LOW  -> 0;
            case HIGH -> 1;
            case BLUE -> 2;
            default   -> 1;
        };

        long tick = Minecraft.getInstance().level.getGameTime();
        int frame = (int) ((tick / FRAME_TICKS) % 2);
        ResourceLocation spriteId = SPRITES[spriteSet][frame];

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(spriteId);

        float plumeLength = (heat == HeatLevel.LOW) ? 1.0f : 2.0f;

        Direction facing = be.getFacing();
        Direction exhaust = facing.getOpposite();

        renderPlume(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)), sprite, exhaust, plumeLength, LightTexture.FULL_BRIGHT, overlay);
    }

    private void renderPlume(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
            Direction exhaustDir, float length, int light, int overlay) {
        Vector3f dir = step(exhaustDir);
        Vector3f up = exhaustDir.getAxis() == Direction.Axis.Y ? step(Direction.SOUTH) : step(Direction.UP);
        Vector3f right = exhaustDir.getAxis() == Direction.Axis.X ? step(Direction.SOUTH) : step(Direction.EAST);

        Vector3f origin = new Vector3f(0.5f, 0.5f, 0.5f).add(new Vector3f(dir).mul(0.25f));
        PoseStack.Pose pose = poseStack.last();

        renderDoubleSidedPlane(consumer, pose, sprite, origin, dir, right, up, 0f, -0.5f, 0.5f, length, light, overlay);
        renderDoubleSidedPlane(consumer, pose, sprite, origin, dir, up, right, 0f, -0.5f, 0.5f, length, light, overlay);
    }

    private void renderDoubleSidedPlane(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            Vector3f origin, Vector3f dir, Vector3f right, Vector3f up,
            float rightOffset, float upMin, float upMax, float length, int light, int overlay) {
        Vector3f a = point(origin, dir, right, up, 0f,      rightOffset, upMin);
        Vector3f b = point(origin, dir, right, up, 0f,      rightOffset, upMax);
        Vector3f c = point(origin, dir, right, up, length,  rightOffset, upMax);
        Vector3f d = point(origin, dir, right, up, length,  rightOffset, upMin);

        vertex(consumer, pose, sprite, a, 1f, 1f, light, overlay);
        vertex(consumer, pose, sprite, b, 1f, 0f, light, overlay);
        vertex(consumer, pose, sprite, c, 0f, 0f, light, overlay);
        vertex(consumer, pose, sprite, d, 0f, 1f, light, overlay);

        vertex(consumer, pose, sprite, d, 0f, 1f, light, overlay);
        vertex(consumer, pose, sprite, c, 0f, 0f, light, overlay);
        vertex(consumer, pose, sprite, b, 1f, 0f, light, overlay);
        vertex(consumer, pose, sprite, a, 1f, 1f, light, overlay);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            Vector3f point, float u, float v, int light, int overlay) {
        consumer.addVertex(pose.pose(), point.x(), point.y(), point.z())
                .setColor(255, 255, 255, 255)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
    }

    private static Vector3f point(Vector3f origin, Vector3f dir, Vector3f right, Vector3f up,
            float along, float rightOffset, float upOffset) {
        return new Vector3f(origin)
                .add(new Vector3f(dir).mul(along))
                .add(new Vector3f(right).mul(rightOffset))
                .add(new Vector3f(up).mul(upOffset));
    }

    private static Vector3f step(Direction d) {
        return new Vector3f(d.getStepX(), d.getStepY(), d.getStepZ());
    }
}
