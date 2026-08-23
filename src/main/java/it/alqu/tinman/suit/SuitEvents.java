package it.alqu.tinman.suit;

import it.alqu.tinman.advancement.ModTriggers;
import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.item.Energy;
import it.alqu.tinman.item.Power;
import it.alqu.tinman.registry.ModArmor;
import it.alqu.tinman.registry.ModParticles;
import it.alqu.tinman.registry.ModSounds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side behaviour for the Tin Man suit.
 *
 * <p>This is the only place that decides whether flight is permitted or energy is spent. The
 * client never makes those calls; it reads the energy component (which vanilla already syncs with
 * the equipped stacks) purely to draw the HUD, and flight permission reaches the client through
 * the vanilla abilities packet.
 */
public final class SuitEvents {
	private SuitEvents() {
	}

	private static final EquipmentSlot[] ARMOR_SLOTS = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	/** Players whose flight this mod granted, so we only ever revoke our own grants. */
	private static final Set<UUID> granted = new HashSet<>();
	/** Fractional energy carried between ticks, so drain rates need not divide into 20. */
	private static final Map<UUID, Double> drainCarry = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !isSuitActive(player)) {
				return true;
			}

			// Full set with charge left shrugs off fall and fire damage.
			return !source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypeTags.IS_FIRE);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			granted.remove(id);
			drainCarry.remove(id);
			Unibeam.forget(id);
		});
	}

	public static List<ItemStack> suitPieces(LivingEntity entity) {
		return List.of(
			entity.getItemBySlot(EquipmentSlot.HEAD),
			entity.getItemBySlot(EquipmentSlot.CHEST),
			entity.getItemBySlot(EquipmentSlot.LEGS),
			entity.getItemBySlot(EquipmentSlot.FEET)
		);
	}

	/** True when all four pieces are worn, regardless of charge. */
	public static boolean isFullSet(LivingEntity entity) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (!ModArmor.isSuitPiece(entity.getItemBySlot(slot))) {
				return false;
			}
		}

		return true;
	}

	/** True when the full set is worn and it still has charge: the condition for every ability. */
	public static boolean isSuitActive(LivingEntity entity) {
		return isFullSet(entity) && Power.available(entity) > 0;
	}

	/**
	 * Spends energy on behalf of a powered weapon, from the carried batteries.
	 *
	 * @return true if the full amount was paid
	 */
	public static boolean drawPower(LivingEntity entity, ItemStack held, int amount) {
		return Power.pay(entity, amount);
	}

	/** How much energy is on hand for a weapon right now. */
	public static int availablePower(LivingEntity entity, ItemStack held) {
		return Power.available(entity);
	}

	private static void tickPlayer(ServerPlayer player) {
		TinManConfig config = TinManConfig.get();
		boolean active = isFullSet(player) && Power.available(player) > 0;

		handleFlight(player, config, active);

		if (active) {
			handleVision(player);
		}
	}

	private static void handleFlight(ServerPlayer player, TinManConfig config, boolean active) {
		UUID id = player.getUUID();
		Abilities abilities = player.getAbilities();

		// Creative and spectator manage their own flight; never interfere.
		if (player.isCreative() || player.isSpectator()) {
			granted.remove(id);
			return;
		}

		boolean shouldFly = active && config.suit.flightEnabled;

		if (shouldFly) {
			if (!abilities.mayfly) {
				abilities.mayfly = true;
				player.onUpdateAbilities();
				granted.add(id);
				player.level().playSound(null, player.blockPosition(),
					ModSounds.SUIT_POWER_UP, SoundSource.PLAYERS, 0.7F, 1.0F);
			}

			if (abilities.flying) {
				drainForFlight(player, config);
				applyClimb(player, config);
				trackFlightDistance(player);
			}

			return;
		}

		if (granted.remove(id)) {
			// Out of battery (or flight switched off): cut out immediately.
			abilities.mayfly = false;
			abilities.flying = false;
			player.onUpdateAbilities();
			drainCarry.remove(id);
			player.level().playSound(null, player.blockPosition(),
				ModSounds.SUIT_POWER_DOWN, SoundSource.PLAYERS, 0.8F, 1.0F);
		}
	}

	/** Accumulates distance flown under suit power and offers it to the advancement trigger. */
	private static void trackFlightDistance(ServerPlayer player) {
		double moved = Mth.length(
			player.getX() - player.xOld,
			player.getY() - player.yOld,
			player.getZ() - player.zOld);

		if (moved <= 0.0) {
			return;
		}

		double total = player.getAttachedOrCreate(ModTriggers.FLIGHT_DISTANCE) + moved;
		player.setAttached(ModTriggers.FLIGHT_DISTANCE, total);
		ModTriggers.SUIT_FLIGHT.trigger(player, total);
	}

	private static void drainForFlight(ServerPlayer player, TinManConfig config) {
		boolean boosting = player.isSprinting();
		double perSecond = Math.max(0, config.suit.flightDrainPerSecond);

		if (boosting) {
			perSecond *= Math.max(1.0, config.suit.boostDrainMultiplier);
			applyBoost(player, config);
		}

		UUID id = player.getUUID();
		double carry = drainCarry.getOrDefault(id, 0.0) + perSecond / 20.0;
		int whole = (int) carry;
		drainCarry.put(id, carry - whole);

		if (whole > 0) {
			Power.pay(player, whole);
		}
	}

	/**
	 * Adds the suit's own vertical thrust on top of vanilla's while jump or sneak is held.
	 *
	 * <p>Vanilla pushes a climb by {@code flyingSpeed * 3}, or 0.15 a tick, and then
	 * {@code Player.travel} throws away the tick's vertical result and keeps 0.6 of the speed it
	 * started with. A steady push against that damping settles at 1.5x itself, so creative flight
	 * climbs at 0.225 blocks a tick — 4.5 a second, against something like 37 for a boosted
	 * cruise. That eightfold gap is what makes going up feel like wading.
	 *
	 * <p>Raising {@code flyingSpeed} would not close it: horizontal speed scales with the same
	 * number, so both grow and the gap stays. This is deliberately vertical only.
	 *
	 * <p>Done here rather than on the client because it is a suit ability, and gameplay numbers
	 * are the server's to decide — a client-side version would let a player pick their own climb
	 * rate on someone else's server. The same damping applies to the server's own simulation of
	 * the player, so what the client is handed settles at 1.5x {@code climbSpeed}.
	 *
	 * <p>Jump and sneak reach the server as part of the ordinary player input packet, so reading
	 * them here needs nothing of our own.
	 */
	private static void applyClimb(ServerPlayer player, TinManConfig config) {
		double speed = config.suit.climbSpeed;

		if (speed <= 0.0) {
			return;
		}

		Input input = player.getLastClientInput();
		int direction = (input.jump() ? 1 : 0) - (input.shift() ? 1 : 0);

		if (direction == 0) {
			return;
		}

		player.setDeltaMovement(player.getDeltaMovement().add(0.0, direction * speed, 0.0));
		// Marks the velocity as changed so the server actually sends it back to the client, which
		// is otherwise the authority on where a flying player is.
		player.hurtMarked = true;
	}

	private static void applyBoost(ServerPlayer player, TinManConfig config) {
		Vec3 look = player.getLookAngle();
		double speed = config.suit.boostSpeed;
		player.setDeltaMovement(player.getDeltaMovement().add(look.scale(speed)));
		player.hurtMarked = true;

		if (player.level() instanceof ServerLevel level) {
			// Thruster plume under the boots, broadcast so other players see it too.
			Vec3 pos = player.position();
			level.sendParticles(ModParticles.THRUSTER_FLAME,
				pos.x, pos.y + 0.1, pos.z, 5, 0.12, 0.05, 0.12, 0.02);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				pos.x, pos.y + 0.1, pos.z, 2, 0.15, 0.05, 0.15, 0.02);

			// Thruster note, throttled so it reads as a loop rather than a machine-gun.
			if (player.tickCount % 8 == 0) {
				level.playSound(null, player.blockPosition(),
					ModSounds.THRUSTER_LOOP, SoundSource.PLAYERS, 0.35F, 1.0F);
			}
		}
	}

	private static void handleVision(ServerPlayer player) {
		boolean dark = player.isUnderWater()
			|| player.level().getMaxLocalRawBrightness(player.blockPosition()) < 6;

		if (!dark) {
			return;
		}

		MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);

		// Refresh before the vanilla end-of-effect flicker starts.
		if (current == null || current.getDuration() < 220) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
		}
	}
}
