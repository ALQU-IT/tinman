package it.alqu.tinman.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** A small spark that pops upward then falls, for machines that are mid-craft. */
public class AssemblerSparkParticle extends SimpleAnimatedParticle {
	private AssemblerSparkParticle(ClientLevel level, double x, double y, double z,
			double xa, double ya, double za, SpriteSet sprites) {
		super(level, x, y, z, sprites, 0.06F);
		this.xd = xa + (this.random.nextDouble() - 0.5) * 0.02;
		this.yd = ya + 0.04;
		this.zd = za + (this.random.nextDouble() - 0.5) * 0.02;
		this.quadSize *= 0.55F;
		this.lifetime = 12 + this.random.nextInt(10);
		this.setFadeColor(0x188C9E);
		this.setSpriteFromAge(sprites);
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType options, ClientLevel level,
				double x, double y, double z, double xa, double ya, double za, RandomSource random) {
			return new AssemblerSparkParticle(level, x, y, z, xa, ya, za, this.sprites);
		}
	}
}
