package it.alqu.tinman.client.hud;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.item.Energy;
import it.alqu.tinman.item.Power;
import it.alqu.tinman.client.render.MobScanner;
import it.alqu.tinman.registry.ModArmor;
import it.alqu.tinman.suit.SuitEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Helmet HUD: an energy bar, current altitude and a readout of whatever the player is looking at.
 *
 * <p>The scanner's own marks are drawn in the world by {@link MobScanner}; the count shown
 * here is just its tally for the frame.
 *
 * <p>Everything drawn here comes from state the client already has — the energy component travels
 * with the equipped stacks, and tracked entities carry their own health — so the overlay needs no
 * dedicated packet and cannot drift out of step with the server.
 */
public class SuitHudElement implements HudElement {
	private static final int PANEL_X = 8;
	private static final int PANEL_Y = 8;
	private static final int BAR_WIDTH = 80;
	private static final int BAR_HEIGHT = 6;

	private static final int COLOUR_TEXT = 0xFFB8F8FF;
	private static final int COLOUR_FRAME = 0xC0102026;
	private static final int COLOUR_TRACK = 0xFF14323A;
	private static final int COLOUR_FILL = 0xFF3FE0E8;
	private static final int COLOUR_FILL_LOW = 0xFFE85C3F;
	private static final int COLOUR_TARGET = 0xFFFFD86B;

	private static final int LINE_HEIGHT = 10;

	/** Below this fraction the bar turns red as a low-power warning. */
	private static final float LOW_POWER = 0.15F;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		Player player = client.player;

		if (player == null) {
			return;
		}

		// The overlay is a helmet feature, so it needs the helmet on and the set complete.
		if (!ModArmor.isSuitPiece(player.getItemBySlot(EquipmentSlot.HEAD)) || !SuitEvents.isFullSet(player)) {
			return;
		}

		List<ItemStack> batteries = Power.batteries(player);
		int energy = Energy.total(batteries);
		int max = Math.max(1, Energy.max() * batteries.size());
		float fill = max == 0 ? 0.0F : Math.clamp(energy / (float) max, 0.0F, 1.0F);

		List<Line> lines = new ArrayList<>();
		lines.add(new Line(Component.translatable("hud.tinman.energy", energy, max), COLOUR_TEXT));
		lines.add(new Line(Component.translatable("hud.tinman.altitude", (int) Math.floor(player.getY())), COLOUR_TEXT));

		if (TinManConfig.get().hud.mobScannerEnabled) {
			int hostiles = MobScanner.hostiles();
			lines.add(new Line(
				Component.translatable("hud.tinman.scan", MobScanner.contacts(), hostiles),
				hostiles > 0 ? COLOUR_FILL_LOW : COLOUR_TEXT));
		}

		Component target = targetLine(client);

		if (target != null) {
			lines.add(new Line(target, COLOUR_TARGET));
		}

		if (!TinManConfig.get().suit.flightEnabled) {
			lines.add(new Line(Component.translatable("hud.tinman.flight_disabled"), COLOUR_FILL_LOW));
		}

		int x = PANEL_X;
		int y = PANEL_Y;

		// The panel grows to whatever the lines need: mob names in particular are far wider than
		// the energy bar, and a frame cropped mid-word looks like a bug rather than a readout.
		int width = BAR_WIDTH;

		for (Line line : lines) {
			width = Math.max(width, client.font.width(line.text()));
		}

		int firstLineY = y + BAR_HEIGHT + 2;
		int bottom = firstLineY + lines.size() * LINE_HEIGHT;

		graphics.fill(x - 3, y - 3, x + width + 3, bottom, COLOUR_FRAME);

		graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOUR_TRACK);
		int filled = Math.round(BAR_WIDTH * fill);

		if (filled > 0) {
			graphics.fill(x, y, x + filled, y + BAR_HEIGHT, fill <= LOW_POWER ? COLOUR_FILL_LOW : COLOUR_FILL);
		}

		for (int i = 0; i < lines.size(); i++) {
			graphics.text(client.font, lines.get(i).text(), x, firstLineY + i * LINE_HEIGHT, lines.get(i).colour());
		}
	}

	/** One row of the readout, held until the panel knows how wide and tall it has to be. */
	private record Line(Component text, int colour) {
	}

	private static @Nullable Component targetLine(Minecraft client) {
		HitResult hit = client.crosshairPickEntity != null
			? new EntityHitResult(client.crosshairPickEntity)
			: client.hitResult;

		if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity living)) {
			return null;
		}

		return Component.translatable("hud.tinman.target",
			living.getName(),
			(int) Math.ceil(living.getHealth()),
			(int) Math.ceil(living.getMaxHealth()));
	}
}
