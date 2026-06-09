package com.leclowndu93150.create_simulated_thrusters.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ThrusterPlumeParticle extends SimpleAnimatedParticle {

    private static final float PLUME_SPREAD       = 0.01f;
    private static final float PLUME_QUAD_SIZE     = 2.0f;
    private static final float PLUME_FRICTION      = 0.99f;
    private static final float PLUME_SPEED_MUL     = 1.0f;
    private static final int   PLUME_BASE_LIFETIME = 40;
    private static final int   BLUE_PLUME_SPRITES  = 6;
    private static final float WHITE_TINT_TARGET    = 1.0f;
    private static final float HOT_TINT_R           = 1.0f;
    private static final float HOT_TINT_G           = 0.68f;
    private static final float HOT_TINT_B           = 0.28f;
    private static final float SMOKE_SPREAD_MAG    = 0.0f;
    private static final float SMOKE_FRICTION      = 0.96f;
    private static final float SMOKE_BASE_LIFT     = 0.0f;
    private static final int   PLUME_SPRITES       = 6;
    private static final int   SMOKE_SPRITES       = 6;
    private static final int   BASE_SMOKE_TRANS    = 20;

    private enum State { PLUME, SMOKE }

    private final SpriteSet spriteSet;
    private final boolean isBlue;
    private State state;
    private float baseSize;
    private final int smokeTransitionAge;
    private final float smokeLift;
    private final Vec3 spreadDirection;
    private final float spreadMagnitude;
    private final float startR;
    private final float startG;
    private final float startB;
    private boolean hasCollided = false;

    double dx, dy, dz;

    protected ThrusterPlumeParticle(ClientLevel level, double x, double y, double z,
                                    double dxIn, double dyIn, double dzIn,
                                    SpriteSet sprites, Vector3f color, boolean isBlue, float sizeScale, float lengthScale) {
        super(level, x, y, z, sprites, 0);
        this.spriteSet = sprites;
        this.isBlue = isBlue;
        this.quadSize *= PLUME_QUAD_SIZE * sizeScale;
        this.baseSize = this.quadSize;
        int baseLife = isBlue ? 24 + random.nextInt(4) : PLUME_BASE_LIFETIME + random.nextInt(5);
        this.lifetime = Math.max(2, Math.round(baseLife * Mth.clamp(lengthScale, 0.05f, 1.0f)));
        this.friction = PLUME_FRICTION;
        this.dx = dxIn + spread();
        this.dy = dyIn + spread();
        this.dz = dzIn + spread();
        this.hasPhysics = true;
        this.state = State.PLUME;

        Vec3 vel = new Vec3(this.dx, this.dy, this.dz).normalize();
        Vec3 nonParallel = Math.abs(vel.dot(new Vec3(1, 0, 0))) > 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = vel.cross(nonParallel).normalize();
        Vec3 v = vel.cross(u).normalize();
        double angle = random.nextDouble() * 2.0 * Math.PI;
        this.spreadDirection = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
        this.spreadMagnitude = 0.1f + random.nextFloat() * 0.7f;
        int baseTransition = BASE_SMOKE_TRANS + random.nextIntBetweenInclusive(-2, 2);
        this.smokeTransitionAge = Math.max(1, Math.round(baseTransition * Mth.clamp(lengthScale, 0.05f, 1.0f)));
        this.smokeLift = isBlue ? 0.0f : SMOKE_BASE_LIFT;

        this.startR = color.x();
        this.startG = color.y();
        this.startB = color.z();

        setColor(this.startR, this.startG, this.startB);
        setAlpha(1f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        double imx = this.dx * PLUME_SPEED_MUL;
        double imy = this.dy * PLUME_SPEED_MUL;
        double imz = this.dz * PLUME_SPEED_MUL;
        double px = this.x, py = this.y, pz = this.z;
        this.move(imx, imy, imz);
        double amx = this.x - px, amy = this.y - py, amz = this.z - pz;

        Vec3 normal = null;
        if (this.onGround) {
            normal = new Vec3(0, 1, 0);
        } else {
            float f = 0.95f;
            if (Math.abs(imx) > 0.001 && Math.abs(amx) < Math.abs(imx) * f)
                normal = new Vec3(imx < 0 ? 1 : -1, 0, 0);
            else if (Math.abs(imz) > 0.001 && Math.abs(amz) < Math.abs(imz) * f)
                normal = new Vec3(0, 0, imz < 0 ? 1 : -1);
            else if (Math.abs(imy) > 0.001 && imy > 0 && Math.abs(amy) < Math.abs(imy) * f)
                normal = new Vec3(0, -1, 0);
        }

        if (normal != null) {
            Vec3 in = new Vec3(this.dx, this.dy, this.dz);
            if (in.normalize().dot(normal) < -1e-5 && in.lengthSqr() > 1e-7) {
                this.hasCollided = true;
                Vec3 nComp = normal.scale(in.dot(normal));
                Vec3 tComp = in.subtract(nComp);
                Vec3 refl = nComp.scale(-0.1).add(tComp);
                double len = refl.length();
                if (len > 1e-5) {
                    double sp = in.length() * 0.9;
                    this.dx = refl.x / len * sp;
                    this.dy = refl.y / len * sp;
                    this.dz = refl.z / len * sp;
                }
            }
        }

        if (state == State.PLUME && this.age >= this.smokeTransitionAge) {
            state = State.SMOKE;
            this.baseSize *= 1.2f;
            this.friction = SMOKE_FRICTION;
            if (isBlue) this.remove();
        }
        if (state == State.SMOKE) {
            this.dy += this.smokeLift;
            // fade color toward gray for smoke phase
            float smokeProgress = (float)(this.age - this.smokeTransitionAge) / (this.lifetime - this.smokeTransitionAge);
            float gray = Mth.lerp(smokeProgress, (this.rCol + this.gCol + this.bCol) / 3f, 0.22f);
            setColor(Mth.lerp(smokeProgress, this.rCol, gray),
                     Mth.lerp(smokeProgress, this.gCol, gray),
                     Mth.lerp(smokeProgress, this.bCol, gray));
        } else if (isBlue) {
            float whiteProgress = Mth.clamp((float) this.age / this.smokeTransitionAge, 0.0f, 1.0f);
            whiteProgress *= whiteProgress;
            setColor(Mth.lerp(whiteProgress, this.startR, WHITE_TINT_TARGET),
                     Mth.lerp(whiteProgress, this.startG, WHITE_TINT_TARGET),
                     Mth.lerp(whiteProgress, this.startB, WHITE_TINT_TARGET));
        } else {
            float hotProgress = Mth.clamp((float) this.age / this.smokeTransitionAge, 0.0f, 1.0f);
            setColor(Mth.lerp(hotProgress, this.startR, HOT_TINT_R),
                     Mth.lerp(hotProgress, this.startG, HOT_TINT_G),
                     Mth.lerp(hotProgress, this.startB, HOT_TINT_B));
        }

        float pct = (float) this.age / this.lifetime;
        if (state == State.PLUME)
            this.quadSize = this.baseSize + (float) Math.pow(pct, 0.8f) * 2f;
        else
            this.quadSize = this.baseSize - pct * 2f + 2.5f;

        int presmoke = this.smokeTransitionAge - 5;
        if (this.age >= presmoke && !this.hasCollided) {
            float sp = (this.age - presmoke) / (float)(this.lifetime - presmoke);
            float mag = (0.8f - sp) * this.spreadMagnitude;
            this.dx += this.spreadDirection.x * SMOKE_SPREAD_MAG * mag;
            this.dy += this.spreadDirection.y * SMOKE_SPREAD_MAG * mag;
            this.dz += this.spreadDirection.z * SMOKE_SPREAD_MAG * mag;
        }

        this.dx *= this.friction;
        this.dy *= this.friction;
        this.dz *= this.friction;

        pickSprite();
    }

    private void pickSprite() {
        int idx;
        if (state == State.PLUME && isBlue) {
            idx = Mth.clamp((int)((float)this.age / this.smokeTransitionAge * BLUE_PLUME_SPRITES), 0, BLUE_PLUME_SPRITES - 1);
        } else if (state == State.PLUME) {
            idx = Mth.clamp((int)((float)this.age / this.smokeTransitionAge * PLUME_SPRITES), 0, PLUME_SPRITES - 1);
        } else {
            int dur = this.lifetime - this.smokeTransitionAge;
            int smoke = dur <= 0 ? 0 : (this.age - this.smokeTransitionAge) * SMOKE_SPRITES / dur;
            idx = PLUME_SPRITES + Mth.clamp(smoke, 0, SMOKE_SPRITES - 1);
        }
        this.setSprite(this.spriteSet.get(idx, PLUME_SPRITES + SMOKE_SPRITES));
    }

    private float spread() {
        if (isBlue)
            return 0.0f;
        return (random.nextFloat() * 2f - 1f) * PLUME_SPREAD;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static class Factory implements ParticleProvider<ThrusterPlumeParticleData> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ThrusterPlumeParticleData data, ClientLevel level,
                double x, double y, double z, double dx, double dy, double dz) {
            return new ThrusterPlumeParticle(level, x, y, z, dx, dy, dz, sprites, data.getColor(), data.isBlue(), data.getSizeScale(), data.getLengthScale());
        }
    }
}
