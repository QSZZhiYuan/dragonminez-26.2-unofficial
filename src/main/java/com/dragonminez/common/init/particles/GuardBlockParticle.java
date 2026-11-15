package com.dragonminez.common.init.particles;

import net.minecraft.util.RandomSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class GuardBlockParticle extends SingleQuadParticle {

    private final SpriteSet spriteSet;

    protected GuardBlockParticle(ClientLevel level, double x, double y, double z, double r, double g, double b, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0, null);

        this.spriteSet = spriteSet;

        this.lifetime = 10;
        this.quadSize = 1.0F;
        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.rCol = (float) r;
        this.gCol = (float) g;
        this.bCol = (float) b;
        this.alpha = 1.0F;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.spriteSet);
            this.quadSize += 0.1F;
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float pPartialTick) {
        return 15728880;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public Provider(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double r, double g, double b, RandomSource random) {
            try {
                return new GuardBlockParticle(level, x, y, z, r, g, b, this.spriteSet);
            } catch (NullPointerException e) {
                return null;
            }
        }
    }
}
