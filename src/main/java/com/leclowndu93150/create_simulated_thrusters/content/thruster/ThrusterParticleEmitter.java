package com.leclowndu93150.create_simulated_thrusters.content.thruster;

import com.leclowndu93150.create_simulated_thrusters.particle.ThrusterPlumeParticleData;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ThrusterParticleEmitter {

    public record ColorPalette(int[] colors) {
        public Vector3f pick(RandomSource rand) {
            int hex = colors[rand.nextInt(colors.length)];
            return new Vector3f(
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >>  8) & 0xFF) / 255f,
                ( hex        & 0xFF) / 255f
            );
        }
    }

    public record EmitterConfig(
        ColorPalette palette,
        boolean isBlue,
        int particlesPerTick,
        float coneHalfAngleDeg,
        float minSpeed,
        float maxSpeed,
        float sizeScale
    ) {}

    private static final ColorPalette ORANGE = new ColorPalette(new int[]{ 0xf2a530, 0xe96a0c, 0xd14b06 });
    private static final ColorPalette BLUE   = new ColorPalette(new int[]{ 0x33d8ff, 0x168bff });

    public static final EmitterConfig LOW   = new EmitterConfig(ORANGE, false, 2, 14f, 0.22f, 0.32f, 2.5f);
    public static final EmitterConfig HIGH  = new EmitterConfig(ORANGE, false, 3, 11f, 0.34f, 0.48f, 3.0f);
    public static final EmitterConfig BLAZE = new EmitterConfig(BLUE,   true,  4,  2f, 0.58f, 0.76f, 1.0f);

    public static void emit(ServerLevel level, Vec3 origin, Direction exhaustDir, EmitterConfig config) {
        RandomSource rand = level.getRandom();
        Vec3 axis = new Vec3(exhaustDir.getStepX(), exhaustDir.getStepY(), exhaustDir.getStepZ());

        for (int i = 0; i < config.particlesPerTick(); i++) {
            Vec3 vel = coneVelocity(rand, axis, config.coneHalfAngleDeg(), config.minSpeed(), config.maxSpeed());
            ThrusterPlumeParticleData data = new ThrusterPlumeParticleData(config.palette().pick(rand), config.isBlue(), config.sizeScale());

            for (ServerPlayer player : level.players()) {
                level.sendParticles(player, data, true,
                    origin.x, origin.y, origin.z,
                    0, vel.x, vel.y, vel.z, 1.0);
            }
        }
    }

    private static Vec3 coneVelocity(RandomSource rand, Vec3 axis, float halfAngleDeg,
                                     float minSpeed, float maxSpeed) {
        float halfAngleRad = (float) Math.toRadians(halfAngleDeg);
        float cosAngle = (float) Math.cos(halfAngleRad);
        float z = cosAngle + rand.nextFloat() * (1f - cosAngle);
        float r = (float) Math.sqrt(1f - z * z);
        float phi = rand.nextFloat() * 2f * (float) Math.PI;
        float x = r * (float) Math.cos(phi);
        float y = r * (float) Math.sin(phi);

        Vec3 world = localToWorld(new Vec3(x, y, z), axis);
        float speed = minSpeed + rand.nextFloat() * (maxSpeed - minSpeed);
        return world.scale(speed);
    }

    private static Vec3 localToWorld(Vec3 local, Vec3 forward) {
        Vec3 up = Math.abs(forward.y) < 0.999 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = forward.cross(up).normalize();
        Vec3 realUp = forward.cross(right).normalize();
        return forward.scale(local.z).add(right.scale(local.x)).add(realUp.scale(local.y));
    }
}
