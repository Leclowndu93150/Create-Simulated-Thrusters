package com.leclowndu93150.create_simulated_thrusters.particle;

import com.leclowndu93150.create_simulated_thrusters.CreateSimulatedThrusters;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ThrusterParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTER =
        DeferredRegister.create(Registries.PARTICLE_TYPE, CreateSimulatedThrusters.MODID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<ThrusterPlumeParticleData>> THRUSTER_PLUME =
        REGISTER.register("thruster_plume", ThrusterPlumeParticleData::createType);
}
