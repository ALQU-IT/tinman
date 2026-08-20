package dev.alqu.tinman.suit;

import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.item.Energy;
import dev.alqu.tinman.registry.ModArmor;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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
		return isFullSet(entity) && Energy.total(suitPieces(entity)) > 0;
	}

	private static void tickPlayer(ServerPlayer player) {
		TinManConfig config = TinManConfig.get();
		List<ItemStack> pieces = suitPieces(player);
		boolean active = isFullSet(player) && Energy.total(pieces) > 0;

		handleFlight(player, config, pieces, active);

		if (active) {
			handleVision(player);
		}
	}

	private static void handleFlight(ServerPlayer player, TinManConfig config, List<ItemStack> pieces, boolean active) {
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
					SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
			}

			if (abilities.flying) {
				drainForFlight(player, config, pieces);
			}

			return;
		}

		if (granted.remove(id)) {
			// Out of charge (or flight switched off): cut out immediately.
			abilities.mayfly = false;
			abilities.flying = false;
			player.onUpdateAbilities();
			drainCarry.remove(id);
			player.level().playSound(null, player.blockPosition(),
				SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, 0.7F);
		}
	}

	private static void drainForFlight(ServerPlayer player, TinManConfig config, List<ItemStack> pieces) {
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
			Energy.drainSpread(pieces, whole);
		}
	}

	private static void applyBoost(ServerPlayer player, TinManConfig config) {
		Vec3 look = player.getLookAngle();
		double speed = config.suit.boostSpeed;
		player.setDeltaMovement(player.getDeltaMovement().add(look.scale(speed)));
		player.hurtMarked = true;

		if (player.level() instanceof ServerLevel level) {
			// Thruster plume under the boots, broadcast so other players see it too.
			Vec3 pos = player.position();
			level.sendParticles(ParticleTypes.FLAME,
				pos.x, pos.y + 0.1, pos.z, 4, 0.12, 0.05, 0.12, 0.01);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				pos.x, pos.y + 0.1, pos.z, 2, 0.15, 0.05, 0.15, 0.02);
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
