package dev.alqu.tinman.client.hud;

import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.item.Energy;
import dev.alqu.tinman.item.Power;
import dev.alqu.tinman.registry.ModArmor;
import dev.alqu.tinman.suit.SuitEvents;
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

import java.util.List;

/**
 * Helmet HUD: an energy bar, current altitude and a readout of whatever the player is looking at.
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

		int x = PANEL_X;
		int y = PANEL_Y;

		graphics.fill(x - 3, y - 3, x + BAR_WIDTH + 3, y + 34, COLOUR_FRAME);

		graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOUR_TRACK);
		int filled = Math.round(BAR_WIDTH * fill);

		if (filled > 0) {
			graphics.fill(x, y, x + filled, y + BAR_HEIGHT, fill <= LOW_POWER ? COLOUR_FILL_LOW : COLOUR_FILL);
		}

		graphics.text(client.font,
			Component.translatable("hud.tinman.energy", energy, max),
			x, y + BAR_HEIGHT + 2, COLOUR_TEXT);

		graphics.text(client.font,
			Component.translatable("hud.tinman.altitude", (int) Math.floor(player.getY())),
			x, y + BAR_HEIGHT + 12, COLOUR_TEXT);

		Component target = targetLine(client);

		if (target != null) {
			graphics.text(client.font, target, x, y + BAR_HEIGHT + 22, COLOUR_TARGET);
		}

		if (!TinManConfig.get().suit.flightEnabled) {
			graphics.text(client.font,
				Component.translatable("hud.tinman.flight_disabled"),
				x, y + BAR_HEIGHT + 32, COLOUR_FILL_LOW);
		}
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
