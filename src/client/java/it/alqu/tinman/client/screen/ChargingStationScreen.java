package it.alqu.tinman.client.screen;

import it.alqu.tinman.TinMan;
import it.alqu.tinman.menu.ChargingStationMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ChargingStationScreen extends AbstractContainerScreen<ChargingStationMenu> {
	private static final Identifier TEXTURE = TinMan.id("textures/gui/container/charging_station.png");

	/** Vertical charge gauge, filled bottom-up, stored beside the panel in the texture. */
	private static final int GAUGE_U = 176;
	private static final int GAUGE_V = 0;
	private static final int GAUGE_W = 10;
	private static final int GAUGE_H = 40;
	private static final int GAUGE_X = 52;
	private static final int GAUGE_Y = 24;

	public ChargingStationScreen(ChargingStationMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);

		int x = this.leftPos;
		int y = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

		int filled = Mth.ceil(this.menu.bufferFill() * GAUGE_H);

		if (filled > 0) {
			int offset = GAUGE_H - filled;
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
				x + GAUGE_X, y + GAUGE_Y + offset,
				GAUGE_U, GAUGE_V + offset,
				GAUGE_W, filled, 256, 256);
		}
	}
}
