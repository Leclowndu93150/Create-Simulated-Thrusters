package com.leclowndu93150.create_simulated_thrusters.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import org.joml.Vector3f;

public class ThrusterPlumeParticleData implements ParticleOptions {

    public static final StreamCodec<RegistryFriendlyByteBuf, ThrusterPlumeParticleData> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VECTOR3F, d -> d.color,
            ByteBufCodecs.BOOL,     d -> d.isBlue,
            ByteBufCodecs.FLOAT,    d -> d.sizeScale,
            ThrusterPlumeParticleData::new
        );

    private final Vector3f color;
    private final boolean isBlue;
    private final float sizeScale;

    public ThrusterPlumeParticleData(Vector3f color, boolean isBlue) {
        this(color, isBlue, 1.0f);
    }

    public ThrusterPlumeParticleData(Vector3f color, boolean isBlue, float sizeScale) {
        this.color = color;
        this.isBlue = isBlue;
        this.sizeScale = sizeScale;
    }

    public Vector3f getColor() { return color; }
    public boolean isBlue() { return isBlue; }
    public float getSizeScale() { return sizeScale; }

    @Override
    public ParticleType<?> getType() {
        return ThrusterParticleTypes.THRUSTER_PLUME.get();
    }

    public static ParticleType<ThrusterPlumeParticleData> createType() {
        return new ParticleType<>(false) {
            @Override
            public MapCodec<ThrusterPlumeParticleData> codec() {
                return MapCodec.unit(new ThrusterPlumeParticleData(new Vector3f(1, 1, 1), false, 1.0f));
            }
            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ThrusterPlumeParticleData> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
