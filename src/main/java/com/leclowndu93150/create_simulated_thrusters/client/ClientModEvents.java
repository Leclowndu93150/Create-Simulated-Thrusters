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

    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ThrusterParticleTypes.THRUSTER_PLUME.get(), ThrusterPlumeParticle.Factory::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateSimulatedThrusters.REDSTONE_THRUSTER_BE.get(), RedstoneThrusterRenderer::new);
        event.registerBlockEntityRenderer(CreateSimulatedThrusters.BLAZE_THRUSTER_BE.get(), BlazeThrusterRenderer::new);
    }
}
