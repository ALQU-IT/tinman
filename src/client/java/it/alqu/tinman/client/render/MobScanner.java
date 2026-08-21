package it.alqu.tinman.client.render;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.registry.ModArmor;
import it.alqu.tinman.suit.SuitEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The helmet's threat scanner: outlines every living thing within range and floats a name and
 * health bar over it, optionally straight through terrain.
 *
 * <p>Drawn as one-frame gizmos rather than a bespoke render type. Vanilla already sorts out the
 * two hard parts for us: the {@code alwaysOnTop} gizmo pass clears the depth buffer before it
 * runs, which is what lets a mark show through a wall, and text gizmos are billboarded against
 * the camera for free. Health bars are billboarded by hand, since gizmo quads take explicit world
 * corners — the camera's orientation quaternion turns local right/up into world vectors for that.
 *
 * <p>Everything read here is already on the client: tracked entities carry their own health, so
 * like the rest of the HUD this needs no packet of its own.
 */
public final class MobScanner {
	/** Colours picked to read against terrain rather than to match anything in particular. */
	private static final int COLOUR_HOSTILE = 0xFFFF5347;
	private static final int COLOUR_PASSIVE = 0xFF3FE0E8;
	private static final int COLOUR_PLAYER = 0xFFFFD86B;

	private static final int COLOUR_BAR_BACK = 0xFF0E1A20;
	private static final int COLOUR_HEALTH_FULL = 0xFF4CE05C;
	private static final int COLOUR_HEALTH_HALF = 0xFFF0D24A;
	private static final int COLOUR_HEALTH_LOW = 0xFFE8452F;

	private static final float BOX_STROKE_WIDTH = 2.0F;
	/** Keeps the outline off the model's own surface so the two do not shimmer against each other. */
	private static final double BOX_PADDING = 0.04;

	/** Health bar size in blocks, at the reference distance. */
	private static final double BAR_WIDTH = 1.0;
	private static final double BAR_HEIGHT = 0.13;
	/** Gap between the top of the outline and the bottom of the bar. */
	private static final double BAR_GAP = 0.28;
	/** Gap between the top of the bar and the bottom of the label. */
	private static final double LABEL_GAP = 0.1;
	private static final float LABEL_SCALE = 0.3F;
	/**
	 * Height of a line of text in blocks, per unit of gizmo scale. Text gizmos hang below their
	 * anchor — the pose is flipped vertically so the font's own downward axis comes out right —
	 * so the label has to be anchored a full line above where it should sit.
	 */
	private static final double LABEL_LINE_HEIGHT = 9.0 / 16.0;

	/**
	 * Distance at which a mark is drawn at its nominal size. Further out it is scaled up in step
	 * with the distance so that it keeps roughly the same size on screen — a bar an arm's length
	 * wide is unreadable at thirty blocks otherwise.
	 */
	private static final double REFERENCE_DISTANCE = 10.0;

	private static int lastContacts;
	private static int lastHostiles;

	private MobScanner() {
	}

	public static void register() {
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> draw(context.levelState().cameraRenderState));
	}

	/**
	 * Creatures the last scan found. Read by the HUD overlay, which extracts its state before the
	 * level is submitted and so is always one frame behind — which no one can see in a count.
	 */
	public static int contacts() {
		return lastContacts;
	}

	/** How many of {@link #contacts()} were hostile. */
	public static int hostiles() {
		return lastHostiles;
	}

	private static void draw(final CameraRenderState camera) {
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		Player player = client.player;

		if (level == null || player == null || !camera.initialized) {
			forget();
			return;
		}

		TinManConfig.Hud config = TinManConfig.get().hud;

		// Same gate as the rest of the helmet overlay: the scanner is a feature of the full suit.
		if (!config.mobScannerEnabled
			|| !ModArmor.isSuitPiece(player.getItemBySlot(EquipmentSlot.HEAD))
			|| !SuitEvents.isFullSet(player)) {
			forget();
			return;
		}

		List<LivingEntity> targets = scan(level, player, config);
		lastContacts = targets.size();
		lastHostiles = (int) targets.stream().filter(entity -> entity instanceof Enemy).count();

		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Quaternionf orientation = camera.orientation;
		Vec3 right = toVec3(orientation.transform(new Vector3f(1.0F, 0.0F, 0.0F)));
		Vec3 up = toVec3(orientation.transform(new Vector3f(0.0F, 1.0F, 0.0F)));

		for (LivingEntity target : targets) {
			mark(target, camera.pos, right, up, partialTick, config.mobScannerThroughWalls);
		}
	}

	private static void forget() {
		lastContacts = 0;
		lastHostiles = 0;
	}

	private static List<LivingEntity> scan(final ClientLevel level, final Player player, final TinManConfig.Hud config) {
		double radius = Math.max(0.0, config.mobScannerRadius);
		Vec3 eye = player.getEyePosition();
		double radiusSq = radius * radius;

		List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
			player.getBoundingBox().inflate(radius),
			entity -> isTarget(player, entity, config) && entity.distanceToSqr(eye) <= radiusSq));

		// The nearest contacts are the ones worth knowing about, so they are the ones that survive
		// the cap — and sorting front to back also keeps the marks stable as the player turns.
		found.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(eye)));

		int cap = Math.max(0, config.mobScannerMaxTargets);
		return found.size() > cap ? found.subList(0, cap) : found;
	}

	private static boolean isTarget(final Player self, final LivingEntity entity, final TinManConfig.Hud config) {
		if (entity == self || !entity.isAlive() || entity instanceof ArmorStand) {
			return false;
		}

		if (entity instanceof Player other && other.isSpectator()) {
			return false;
		}

		return config.mobScannerShowPassive || entity instanceof Enemy || entity instanceof Player;
	}

	private static void mark(
		final LivingEntity target,
		final Vec3 cameraPos,
		final Vec3 right,
		final Vec3 up,
		final float partialTick,
		final boolean throughWalls
	) {
		// The entity's own bounding box is the right shape — it already accounts for pose and for
		// scale, so babies come out small — but it sits at the last tick's position, which visibly
		// lags a running mob. Shift it to the interpolated position the model is drawn at.
		Vec3 position = target.getPosition(partialTick);
		AABB box = target.getBoundingBox().move(position.subtract(target.position())).inflate(BOX_PADDING);

		int accent = accent(target);
		apply(Gizmos.cuboid(box, GizmoStyle.stroke(accent, BOX_STROKE_WIDTH)), throughWalls);

		double scale = Math.max(1.0, position.distanceTo(cameraPos) / REFERENCE_DISTANCE);
		double barWidth = BAR_WIDTH * scale;
		double barHeight = BAR_HEIGHT * scale;
		double barBottom = box.maxY + BAR_GAP * scale;
		Vec3 barCentre = new Vec3(position.x, barBottom + barHeight / 2.0, position.z);

		float health = Mth.clamp(target.getHealth(), 0.0F, target.getMaxHealth());
		float fraction = target.getMaxHealth() <= 0.0F ? 0.0F : health / target.getMaxHealth();

		// Backdrop first, fill second: the always-on-top pass writes no depth, so within a single
		// alpha class the later quad is simply the one that ends up visible.
		quad(barCentre, right, up, -barWidth / 2.0, barWidth / 2.0, barHeight, COLOUR_BAR_BACK, throughWalls);

		if (fraction > 0.0F) {
			double inset = barHeight * 0.18;
			double innerWidth = barWidth - inset * 2.0;
			quad(barCentre, right, up,
				-innerWidth / 2.0, -innerWidth / 2.0 + innerWidth * fraction,
				barHeight - inset * 2.0, healthColour(fraction), throughWalls);
		}

		double labelBottom = barBottom + barHeight + LABEL_GAP * scale;
		Vec3 labelPos = new Vec3(position.x, labelBottom + LABEL_SCALE * scale * LABEL_LINE_HEIGHT, position.z);
		String label = "%s  %d/%d".formatted(
			target.getName().getString(),
			Mth.ceil(health),
			Mth.ceil(target.getMaxHealth()));

		apply(Gizmos.billboardText(label, labelPos,
			TextGizmo.Style.forColorAndCentered(accent).withScale((float) (LABEL_SCALE * scale))), throughWalls);
	}

	/** Emits one billboarded rectangle, given its horizontal span relative to the bar's centre. */
	private static void quad(
		final Vec3 centre,
		final Vec3 right,
		final Vec3 up,
		final double from,
		final double to,
		final double height,
		final int colour,
		final boolean throughWalls
	) {
		double half = height / 2.0;
		apply(Gizmos.rect(
			corner(centre, right, up, from, -half),
			corner(centre, right, up, from, half),
			corner(centre, right, up, to, half),
			corner(centre, right, up, to, -half),
			GizmoStyle.fill(colour)), throughWalls);
	}

	private static Vec3 corner(final Vec3 centre, final Vec3 right, final Vec3 up, final double x, final double y) {
		return centre.add(right.scale(x)).add(up.scale(y));
	}

	private static void apply(final GizmoProperties gizmo, final boolean throughWalls) {
		if (throughWalls) {
			gizmo.setAlwaysOnTop();
		}
	}

	private static int accent(final LivingEntity target) {
		if (target instanceof Player) {
			return COLOUR_PLAYER;
		}

		return target instanceof Enemy ? COLOUR_HOSTILE : COLOUR_PASSIVE;
	}

	private static int healthColour(final float fraction) {
		return fraction > 0.5F
			? blend(COLOUR_HEALTH_HALF, COLOUR_HEALTH_FULL, (fraction - 0.5F) * 2.0F)
			: blend(COLOUR_HEALTH_LOW, COLOUR_HEALTH_HALF, fraction * 2.0F);
	}

	private static int blend(final int from, final int to, final float progress) {
		float t = Mth.clamp(progress, 0.0F, 1.0F);
		int red = Mth.lerpInt(t, from >> 16 & 0xFF, to >> 16 & 0xFF);
		int green = Mth.lerpInt(t, from >> 8 & 0xFF, to >> 8 & 0xFF);
		int blue = Mth.lerpInt(t, from & 0xFF, to & 0xFF);
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	private static Vec3 toVec3(final Vector3f vector) {
		return new Vec3(vector.x(), vector.y(), vector.z());
	}
}
