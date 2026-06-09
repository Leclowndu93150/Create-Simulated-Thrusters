package com.leclowndu93150.create_simulated_thrusters.client;

import com.leclowndu93150.create_simulated_thrusters.CreateSimulatedThrusters;
import com.leclowndu93150.create_simulated_thrusters.particle.ThrusterParticleTypes;
import com.leclowndu93150.create_simulated_thrusters.particle.ThrusterPlumeParticle;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = CreateSimulatedThrusters.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final ResourceLocation THRUST_SPRITE = ResourceLocation.fromNamespaceAndPath(
            CreateSimulatedThrusters.MODID, "block/redstone_thruster_thrust");
    public static final ResourceLocation BLAZE_THRUST_1_SPRITE = ResourceLocation.fromNamespaceAndPath(
            CreateSimulatedThrusters.MODID, "block/blaze_thrust_1");
    public static final ResourceLocation BLAZE_THRUST_2_SPRITE = ResourceLocation.fromNamespaceAndPath(
            CreateSimulatedThrusters.MODID, "block/blaze_thrust_2");
    public static final ResourceLocation BRASS_THRUST_MID_SPRITE = ResourceLocation.fromNamespaceAndPath(
            CreateSimulatedThrusters.MODID, "block/brass_thruster/brass_thruster_thrust_mid");
    public static final ResourceLocation BRASS_THRUST_HIGH_SPRITE = ResourceLocation.fromNamespaceAndPath(
            CreateSimulatedThrusters.MODID, "block/brass_thruster/brass_thruster_thrust_high");

    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ThrusterParticleTypes.THRUSTER_PLUME.get(), ThrusterPlumeParticle.Factory::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateSimulatedThrusters.REDSTONE_THRUSTER_BE.get(), RedstoneThrusterRenderer::new);
        event.registerBlockEntityRenderer(CreateSimulatedThrusters.BLAZE_THRUSTER_BE.get(), BlazeThrusterRenderer::new);
        event.registerBlockEntityRenderer(CreateSimulatedThrusters.BRASS_THRUSTER_BE.get(), BrassThrusterRenderer::new);
    }
}
