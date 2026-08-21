package it.alqu.tinman.client.render;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.suit.SuitEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks how far each suited flier has leaned into their dive.
 *
 * <p>Purely client side and derived from things every client already knows — worn equipment and
 * movement — so remote players lean too without a packet for it.
 */
public final class FlightLean {
	private FlightLean() {
	}

	/** Per player: {@code [previous tick, current tick]}, so rendering can interpolate. */
	private static final Map<UUID, float[]> LEAN = new HashMap<>();

	public static void tick(ClientLevel level) {
		TinManConfig.Suit config = TinManConfig.get().suit;

		if (!config.flightLeanEnabled) {
			LEAN.clear();
			return;
		}

		LEAN.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);

		for (Player player : level.players()) {
			float[] lean = LEAN.computeIfAbsent(player.getUUID(), id -> new float[2]);
			lean[0] = lean[1];
			lean[1] = Mth.clamp(lean[1] + Math.signum(target(player, config) - lean[1])
				* (float) Math.max(0.001, config.flightLeanRate), 0.0F, 1.0F);

			// Snap the last sliver so it settles instead of jittering around the target.
			if (Math.abs(target(player, config) - lean[1]) < config.flightLeanRate) {
				lean[1] = target(player, config);
			}
		}
	}

	/** How far this player should be leaning right now, 0 upright to 1 flat. */
	private static float target(Player player, TinManConfig.Suit config) {
		// Never fight a real elytra: vanilla already owns that pose.
		if (player.isFallFlying() || player.onGround() || !SuitEvents.isFullSet(player)) {
			return 0.0F;
		}

		Vec3 movement = player.getDeltaMovement();
		double speed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

		return Mth.clamp((float) (speed / Math.max(0.01, config.flightLeanFullSpeed)), 0.0F, 1.0F);
	}

	/** Interpolated lean for rendering, 0 upright to 1 flat. */
	public static float lean(Player player, float partialTicks) {
		float[] lean = LEAN.get(player.getUUID());
		return lean == null ? 0.0F : Mth.lerp(partialTicks, lean[0], lean[1]);
	}
}
