package it.alqu.tinman.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** A short-lived, quickly shrinking flame that drifts with whatever emitted it. */
public class ThrusterFlameParticle extends SimpleAnimatedParticle {
	private ThrusterFlameParticle(ClientLevel level, double x, double y, double z,
			double xa, double ya, double za, SpriteSet sprites) {
		super(level, x, y, z, sprites, 0.0F);
		this.xd = xa * 0.4;
		this.yd = ya * 0.4 - 0.02;
		this.zd = za * 0.4;
		this.quadSize *= 0.9F;
		this.lifetime = 8 + this.random.nextInt(6);
		this.setFadeColor(0x3FE0E8);
		this.setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		// Shrink as it burns out so the plume tapers.
		this.quadSize *= 0.88F;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType options, ClientLevel level,
				double x, double y, double z, double xa, double ya, double za, RandomSource random) {
			return new ThrusterFlameParticle(level, x, y, z, xa, ya, za, this.sprites);
		}
	}
}
