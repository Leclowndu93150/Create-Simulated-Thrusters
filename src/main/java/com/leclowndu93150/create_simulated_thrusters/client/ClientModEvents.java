package com.leclowndu93150.create_simulated_thrusters.client;

import com.leclowndu93150.create_simulated_thrusters.CreateSimulatedThrusters;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CreateSimulatedThrusters.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final ResourceLocation THRUST_SPRITE = ResourceLocation.fromNamespaceAndPath(
            CreateSimulatedThrusters.MODID, "block/redstone_thruster_thrust");

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateSimulatedThrusters.REDSTONE_THRUSTER_BE.get(), RedstoneThrusterRenderer::new);
    }
}
