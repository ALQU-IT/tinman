package dev.alqu.tinman.entity;

import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Pulse Gauntlet's projectile.
 *
 * <p>It has no model: the visible bolt is a particle trail emitted server side with
 * {@code sendParticles}, so every nearby player sees the same thing and the entity itself renders
 * as nothing. Damage and knockback are resolved here, on the server.
 */
public class PulseBolt extends ThrowableProjectile {
	/** Despawn after three seconds so stray shots never accumulate. */
	private static final int MAX_AGE = 60;

	private float damage = 6.0F;
	private boolean charged;
	private int age;

	public PulseBolt(EntityType<? extends PulseBolt> type, Level level) {
		super(type, level);
	}

	public PulseBolt(Level level, LivingEntity owner) {
		super(ModEntities.PULSE_BOLT, level);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
	}

	public void configure(float damage, boolean charged) {
		this.damage = damage;
		this.charged = charged;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		// Nothing to sync: the bolt renders as particles the server already broadcasts.
	}

	@Override
	protected double getDefaultGravity() {
		// An energy bolt should fly flat rather than arc like a thrown item.
		return 0.0;
	}

	@Override
	protected float getAirDrag() {
		return 1.0F;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level() instanceof ServerLevel level) {
			Vec3 pos = this.position();
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 3, 0.05, 0.05, 0.05, 0.0);
			level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);

			if (++this.age > MAX_AGE) {
				this.discard();
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);

		if (this.level().isClientSide()) {
			return;
		}

		Entity target = hitResult.getEntity();
		Entity owner = this.getOwner();
		target.hurt(this.damageSources().thrown(this, owner), this.damage);

		if (target instanceof LivingEntity living) {
			Vec3 push = this.getDeltaMovement().normalize().scale(this.charged ? 1.2 : 0.6);
			living.push(push.x, 0.25, push.z);
			living.hurtMarked = true;
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);

		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}

		Vec3 pos = this.position();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 18, 0.25, 0.25, 0.25, 0.15);
		level.playSound(null, this.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.6F, 1.8F);

		TinManConfig.Weapons config = TinManConfig.get().weapons;

		if (this.charged && config.chargedShotEnabled && config.chargedShotExplosionRadius > 0) {
			// ExplosionInteraction.NONE: hurts mobs, never touches terrain.
			level.explode(this.getOwner(), pos.x, pos.y, pos.z,
				(float) config.chargedShotExplosionRadius, Level.ExplosionInteraction.NONE);
		}

		this.discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("PulseDamage", this.damage);
		output.putBoolean("PulseCharged", this.charged);
		output.putInt("PulseAge", this.age);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.damage = input.getFloatOr("PulseDamage", 6.0F);
		this.charged = input.getBooleanOr("PulseCharged", false);
		this.age = input.getIntOr("PulseAge", 0);
	}
}
