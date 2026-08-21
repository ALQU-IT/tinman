package dev.alqu.tinman.suit;

import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.item.Power;
import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.network.ConfigSyncPayload;
import dev.alqu.tinman.network.FireUnibeamPayload;
import dev.alqu.tinman.registry.ModParticles;
import dev.alqu.tinman.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The full set's chest beam.
 *
 * <p>Everything here runs on the server: the client only reports that its key was pressed, and the
 * server decides whether the suit is worn and charged, where the beam points, what it hits and
 * what it costs. The beam itself is drawn with particles broadcast from the server, so other
 * players see the same shot rather than nothing at all.
 */
public final class Unibeam {
	private Unibeam() {
	}

	/** How far off the beam's centre line an entity can be and still be caught by it. */
	private static final double BEAM_THICKNESS = 0.85;

	/** Game time each player last fired, for the cooldown. */
	private static final Map<UUID, Long> lastFired = new HashMap<>();

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(FireUnibeamPayload.TYPE, FireUnibeamPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(FireUnibeamPayload.TYPE,
			(payload, context) -> context.server().execute(() -> fire(context.player())));

		// Hand each joining player the numbers this server actually runs, so their bars,
		// HUD and tooltips agree with it rather than with their own config file.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			sender.sendPacket(ConfigSyncPayload.of(TinManConfig.get())));
	}

	public static void forget(UUID player) {
		lastFired.remove(player);
	}

	private static void fire(ServerPlayer player) {
		TinManConfig.Suit config = TinManConfig.get().suit;

		if (!config.unibeamEnabled || !SuitEvents.isFullSet(player)) {
			return;
		}

		ServerLevel level = player.level() instanceof ServerLevel serverLevel ? serverLevel : null;

		if (level == null) {
			return;
		}

		long now = level.getGameTime();
		Long previous = lastFired.get(player.getUUID());

		if (previous != null && now - previous < Math.max(1, config.unibeamCooldownTicks)) {
			return;
		}

		if (!Power.pay(player, config.unibeamEnergyCost)) {
			level.playSound(null, player.blockPosition(), ModSounds.SUIT_POWER_DOWN, SoundSource.PLAYERS, 0.5F, 1.6F);
			return;
		}

		lastFired.put(player.getUUID(), now);

		// Fires from the chest, not the eyes, so it reads as coming out of the suit.
		Vec3 start = player.getEyePosition().subtract(0.0, 0.45, 0.0);

		fireBeam(level, player, start, player.getLookAngle(), config.unibeamRange, (float) config.unibeamDamage);
		level.playSound(null, player.blockPosition(), ModSounds.UNIBEAM_FIRE, SoundSource.PLAYERS, 1.2F, 1.0F);
	}

	/**
	 * The beam itself: trace it, stop it at terrain, hurt what it crosses and draw it.
	 *
	 * <p>Kept separate from the gating above so the geometry can be exercised without a wearer.
	 */
	public static void fireBeam(ServerLevel level, @Nullable ServerPlayer shooter, Vec3 start, Vec3 direction,
			double range, float damage) {
		Vec3 end = start.add(direction.normalize().scale(range));

		// Stop the beam at the first solid block it meets.
		// Fall back to an empty collision context when there is no shooter, so the trace still works.
		ClipContext clip = shooter != null
			? new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)
			: new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());

		BlockHitResult blocked = level.clip(clip);

		if (blocked.getType() != HitResult.Type.MISS) {
			end = blocked.getLocation();
		}

		hurtAlong(level, shooter, start, end, damage);
		draw(level, start, end);
	}

	/** Damages every living thing whose hitbox the beam passes through, not just the first. */
	private static void hurtAlong(ServerLevel level, @Nullable ServerPlayer shooter, Vec3 start, Vec3 end, float damage) {
		AABB sweep = new AABB(start, end).inflate(BEAM_THICKNESS);

		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep,
				entity -> entity != shooter && entity.isAlive() && entity.isPickable())) {

			// Everything the line crosses is hit, not just the first thing in the way.
			if (target.getBoundingBox().inflate(BEAM_THICKNESS).clip(start, end).isPresent()) {
				target.hurtServer(level, level.damageSources().indirectMagic(shooter, shooter), damage);
			}
		}
	}

	private static void draw(ServerLevel level, Vec3 start, Vec3 end) {
		Vec3 span = end.subtract(start);
		double length = span.length();

		if (length < 0.01) {
			return;
		}

		Vec3 step = span.scale(1.0 / length);

		// Two particles per block keeps the beam looking solid without flooding the network.
		for (double travelled = 0.0; travelled < length; travelled += 0.5) {
			Vec3 point = start.add(step.scale(travelled));
			level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ModParticles.THRUSTER_FLAME, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
		}

		// A burst where it lands.
		level.sendParticles(ModParticles.ASSEMBLER_SPARK, end.x, end.y, end.z, 20, 0.3, 0.3, 0.3, 0.25);
		level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 12, 0.2, 0.2, 0.2, 0.08);
	}
}
