package it.alqu.tinman.client.render;

import it.alqu.tinman.network.UnibeamShotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws each unibeam shot as a solid beam, using vanilla's beacon beam.
 *
 * <p>A beam is a hitscan the server re-resolves every tick it is held, so there is nothing
 * persistent to hang a renderer off — no entity, no block entity. Instead updates arrive as a pair
 * of points, once a tick per firing player, and are kept here under that player's entity id. A
 * held beam therefore refreshes in place rather than piling twenty overlapping copies a second on
 * top of each other, and one that stops simply runs out of updates and fades.
 *
 * <p>{@link BeaconRenderer#submitBeaconBeam} does the geometry, so this is the real beacon beam
 * rather than a lookalike: same texture, same scrolling, same inner core inside an outer glow. It
 * only ever builds a beam straight up, so the pose is rotated to put that axis along the shot.
 */
public final class UnibeamRenderer {
	private UnibeamRenderer() {
	}

	/**
	 * Ticks a beam survives without an update before it is gone.
	 *
	 * <p>Short, because a held beam is refreshed every tick: this is only the tail after the key
	 * is released, plus enough slack that a dropped or late packet does not make it flicker.
	 */
	private static final int LIFETIME = 4;

	/** Bright cyan-white core, matching the suit's palette rather than a beacon's white. */
	private static final int COLOUR_CORE = 0xFF9FF4FF;

	/** Thinner than a beacon's full block, which would swallow the player who fired it. */
	private static final float CORE_RADIUS = 0.11F;
	private static final float GLOW_RADIUS = 0.2F;

	/** At most one live beam per shooter, so a held one refreshes instead of accumulating. */
	private static final Map<Integer, Shot> BEAMS = new HashMap<>();

	/** One beam, from a muzzle to wherever the server's trace stopped. */
	private static final class Shot {
		private Vec3 start;
		private Vec3 end;
		private int age;

		private Shot(Vec3 start, Vec3 end) {
			this.start = start;
			this.end = end;
		}
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(UnibeamShotPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				Shot beam = BEAMS.computeIfAbsent(payload.shooterId(), id -> new Shot(payload.start(), payload.end()));
				beam.start = payload.start();
				beam.end = payload.end();
				beam.age = 0;
			}));

		LevelRenderEvents.COLLECT_SUBMITS.register(UnibeamRenderer::draw);
	}

	/** Ages the beams a tick and drops any that have stopped being refreshed. */
	public static void tick() {
		BEAMS.values().removeIf(beam -> ++beam.age >= LIFETIME);
	}

	/** Everything goes when the world does, so a beam cannot outlive the level it was fired in. */
	public static void clear() {
		BEAMS.clear();
	}

	private static void draw(LevelRenderContext context) {
		if (BEAMS.isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Vec3 camera = context.levelState().cameraRenderState.pos;
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		// Scrolls the texture along the beam, the same way a beacon's climbs.
		float animation = client.level == null ? 0.0F : Math.floorMod(client.level.getGameTime(), 40) + partialTick;

		for (Shot shot : BEAMS.values()) {
			// A beam being held is refreshed to age 0 every tick, so this only bites on the tail
			// after it stops: it dies away rather than blinking out.
			float remaining = 1.0F - Mth.clamp((shot.age + partialTick) / LIFETIME, 0.0F, 1.0F);
			int length = Mth.ceil(shot.end.distanceTo(shot.start));

			if (length <= 0 || remaining <= 0.0F) {
				continue;
			}

			submit(context, camera, shot, length, animation, remaining);
		}
	}

	private static void submit(LevelRenderContext context, Vec3 camera, Shot shot, int length,
			float animation, float remaining) {
		Vec3 direction = shot.end.subtract(shot.start).normalize();

		context.poseStack().pushPose();
		context.poseStack().translate(shot.start.x - camera.x, shot.start.y - camera.y, shot.start.z - camera.z);

		// The beam is built along +Y, so swing that axis onto the shot.
		context.poseStack().mulPose(new Quaternionf().rotateTo(
			0.0F, 1.0F, 0.0F,
			(float) direction.x, (float) direction.y, (float) direction.z));

		// submitBeaconBeam offsets by half a block to centre itself in a block; there is no block
		// here, so take that back out and let the beam run down the line it was actually fired on.
		context.poseStack().translate(-0.5, 0.0, -0.5);

		BeaconRenderer.submitBeaconBeam(
			context.poseStack(),
			context.submitNodeCollector(),
			BeaconRenderer.BEAM_LOCATION,
			1.0F,
			animation,
			0,
			length,
			ARGB.multiplyAlpha(COLOUR_CORE, remaining),
			CORE_RADIUS,
			GLOW_RADIUS);

		context.poseStack().popPose();
	}
}
