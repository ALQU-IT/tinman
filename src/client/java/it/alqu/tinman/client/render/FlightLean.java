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
 * <p>Purely client side and derived from things every client already knows — worn equipment,
 * position and time in the air — so remote players lean too without a packet for it.
 */
public final class FlightLean {
	private FlightLean() {
	}

	/**
	 * Ticks in the air past which a jump cannot be the explanation. A sprint jump is back on the
	 * ground inside about thirteen, so twenty leaves room for a jump off a slab or under Jump
	 * Boost without ever letting a hop read as flight.
	 */
	private static final int SUSTAINED_FLIGHT_TICKS = 20;

	/**
	 * Downward blocks per tick past which this is a fall, not flight. Free fall passes this
	 * within seven ticks and keeps accelerating to nearly four; flight never approaches it.
	 */
	private static final double FALL_SPEED = 0.5;

	private static final Map<UUID, State> STATES = new HashMap<>();

	/** Per player, so rendering can interpolate and flight can be told apart from a jump. */
	private static final class State {
		private float previous;
		private float current;
		private int airborneTicks;
	}

	public static void tick(ClientLevel level) {
		TinManConfig.Suit config = TinManConfig.get().suit;

		if (!config.flightLeanEnabled) {
			STATES.clear();
			return;
		}

		STATES.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);

		for (Player player : level.players()) {
			State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
			state.airborneTicks = player.onGround() ? 0 : state.airborneTicks + 1;

			float target = target(player, state, config);
			float rate = (float) Math.max(0.001, config.flightLeanRate);

			state.previous = state.current;
			// Snap the last sliver so it settles instead of jittering around the target.
			state.current = Math.abs(target - state.current) < rate
				? target
				: Mth.clamp(state.current + Math.signum(target - state.current) * rate, 0.0F, 1.0F);
		}
	}

	/** How far this player should be leaning right now, 0 upright to 1 flat. */
	private static float target(Player player, State state, TinManConfig.Suit config) {
		// Never fight a real elytra: vanilla already owns that pose.
		if (player.isFallFlying() || !SuitEvents.isFullSet(player)) {
			return 0.0F;
		}

		Vec3 movement = movement(player);

		if (!isFlying(player, state, movement)) {
			return 0.0F;
		}

		double speed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

		return Mth.clamp((float) (speed / Math.max(0.01, config.flightLeanFullSpeed)), 0.0F, 1.0F);
	}

	/**
	 * Whether this player is actually flying, rather than merely off the ground.
	 *
	 * <p>Being airborne is not enough on its own: a sprint jump is airborne too, and leaning into
	 * one was exactly the bug this guards against.
	 */
	private static boolean isFlying(Player player, State state, Vec3 movement) {
		if (player.onGround()) {
			return false;
		}

		// Exact for the player at this client, since vanilla syncs their own abilities to them.
		if (player.getAbilities().flying) {
			return true;
		}

		// Nobody else's flight flag ever reaches this client, so for remote players it has to be
		// inferred from what does: a jump ends quickly and a fall only gets faster, while flight
		// sustains both time in the air and a controlled vertical speed.
		return state.airborneTicks >= SUSTAINED_FLIGHT_TICKS && movement.y > -FALL_SPEED;
	}

	/**
	 * Distance covered since the last tick.
	 *
	 * <p>Deliberately not {@code getDeltaMovement()}. A remote player's is only ever written by an
	 * explicit velocity packet, so for anyone but the local player it reads as zero — which is why
	 * nobody else was ever actually seen leaning. The position delta is the movement that really
	 * happened, and it is right for both.
	 */
	private static Vec3 movement(Player player) {
		return player.position().subtract(player.oldPosition());
	}

	/** Interpolated lean for rendering, 0 upright to 1 flat. */
	public static float lean(Player player, float partialTicks) {
		State state = STATES.get(player.getUUID());
		return state == null ? 0.0F : Mth.lerp(partialTicks, state.previous, state.current);
	}
}
