package it.alqu.tinman.suit;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.item.Power;
import it.alqu.tinman.network.ConfigSyncPayload;
import it.alqu.tinman.network.FireUnibeamPayload;
import it.alqu.tinman.network.UnibeamShotPayload;
import it.alqu.tinman.registry.ModParticles;
import it.alqu.tinman.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
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
 * <p>Everything here runs on the server: the client only reports that its key is <em>held</em>,
 * and the server decides whether the suit is worn and charged, where the beam points, what it hits
 * and what it costs. Once resolved, the traced line is sent to every client that can see it, so
 * other players watch the same beam rather than nothing at all.
 *
 * <p>The beam is continuous. The client asks once a tick for as long as the key is down, and each
 * request buys exactly one tick of beam — so it stops the moment the key is released, or the
 * player disconnects, or the packets stop for any other reason. No held state to get stuck on.
 */
public final class Unibeam {
	private Unibeam() {
	}

	/** How far off the beam's centre line an entity can be and still be caught by it. */
	private static final double BEAM_THICKNESS = 0.85;

	/** How far from a shot a player has to be before it is not worth telling them about it. */
	private static final double VIEW_RANGE = 192.0;

	/** Game time each player last fired, so a held beam can be told from a fresh one. */
	private static final Map<UUID, Long> lastFired = new HashMap<>();

	/** Fractional energy carried between ticks, so a per-second rate need not divide into 20. */
	private static final Map<UUID, Double> drainCarry = new HashMap<>();

	/** A gap longer than this means the beam stopped and is starting again, not continuing. */
	private static final long SUSTAIN_GAP = 2;

	/** How often the firing sound repeats while the beam is held. */
	private static final long LOOP_TICKS = 20;

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(FireUnibeamPayload.TYPE, FireUnibeamPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(UnibeamShotPayload.TYPE, UnibeamShotPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(FireUnibeamPayload.TYPE,
			(payload, context) -> context.server().execute(() -> fire(context.player())));

		// Hand each joining player the numbers this server actually runs, so their bars,
		// HUD and tooltips agree with it rather than with their own config file.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			sender.sendPacket(ConfigSyncPayload.of(TinManConfig.get())));
	}

	public static void forget(UUID player) {
		lastFired.remove(player);
		drainCarry.remove(player);
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

		UUID id = player.getUUID();
		long now = level.getGameTime();
		Long previous = lastFired.get(id);

		// The client asks once a tick, but packets do not have to land one per tick. Never let a
		// burst of them buy more than one tick of beam.
		if (previous != null && previous >= now) {
			return;
		}

		boolean starting = previous == null || now - previous > SUSTAIN_GAP;

		if (starting) {
			drainCarry.remove(id);
		}

		if (!spend(player, config)) {
			drainCarry.remove(id);
			lastFired.remove(id);
			level.playSound(null, player.blockPosition(), ModSounds.SUIT_POWER_DOWN, SoundSource.PLAYERS, 0.5F, 1.6F);
			return;
		}

		lastFired.put(id, now);

		// Fires from the chest, not the eyes, so it reads as coming out of the suit.
		Vec3 start = player.getEyePosition().subtract(0.0, 0.45, 0.0);

		fireBeam(level, player, start, player.getLookAngle(), config.unibeamRange, (float) config.unibeamDamage);

		// Once on the way up, then a quieter loop, so holding it reads as one sustained note
		// rather than the same crack twenty times a second.
		if (starting) {
			level.playSound(null, player.blockPosition(), ModSounds.UNIBEAM_FIRE, SoundSource.PLAYERS, 1.2F, 1.0F);
		} else if (now % LOOP_TICKS == 0) {
			level.playSound(null, player.blockPosition(), ModSounds.UNIBEAM_FIRE, SoundSource.PLAYERS, 0.5F, 1.1F);
		}
	}

	/**
	 * Takes this tick's share of the per-second cost.
	 *
	 * <p>Carried as a fraction between ticks, so a rate that does not divide into 20 is not
	 * silently rounded down to nothing.
	 */
	private static boolean spend(ServerPlayer player, TinManConfig.Suit config) {
		UUID id = player.getUUID();
		double carry = drainCarry.getOrDefault(id, 0.0) + Math.max(0, config.unibeamEnergyPerSecond) / 20.0;
		int whole = (int) carry;

		if (whole > 0 && !Power.pay(player, whole)) {
			return false;
		}

		drainCarry.put(id, carry - whole);
		return true;
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
		broadcast(level, shooter == null ? -1 : shooter.getId(), start, end);
	}

	/**
	 * Tells every client that could see the shot to draw it.
	 *
	 * <p>Sent to whoever is tracking the midpoint rather than the muzzle: a beam thirty blocks
	 * long can easily start outside a viewer's range and end well inside it.
	 */
	private static void broadcast(ServerLevel level, int shooterId, Vec3 start, Vec3 end) {
		UnibeamShotPayload shot = new UnibeamShotPayload(shooterId, start, end);

		for (ServerPlayer viewer : PlayerLookup.around(level, start.add(end).scale(0.5), VIEW_RANGE)) {
			ServerPlayNetworking.send(viewer, shot);
		}
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

	/**
	 * The two ends of the shot. The line between them is drawn client side as a solid beam, so
	 * all that is wanted here is a flare at the muzzle and a burst where it lands.
	 *
	 * <p>Counts are deliberately small: this runs every tick a held beam is alive, so what looks
	 * modest per call is still twenty times a second.
	 */
	private static void draw(ServerLevel level, Vec3 start, Vec3 end) {
		if (end.distanceToSqr(start) < 0.0001) {
			return;
		}

		level.sendParticles(ModParticles.THRUSTER_FLAME, start.x, start.y, start.z, 1, 0.08, 0.08, 0.08, 0.01);

		level.sendParticles(ModParticles.ASSEMBLER_SPARK, end.x, end.y, end.z, 3, 0.25, 0.25, 0.25, 0.2);
		level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 2, 0.2, 0.2, 0.2, 0.06);
	}
}
