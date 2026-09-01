package it.alqu.tinman.client.screen;

import it.alqu.tinman.menu.TerminalMenu;
import it.alqu.tinman.network.TerminalViewPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Storage Terminal's screen: a chest-shaped grid with a search box over it.
 *
 * <p>Neither the search text nor the scroll position is applied here. Both are sent to the server,
 * which owns the pool and repaints the grid — the client has never seen what is in those chests
 * and cannot filter what it does not have.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

	private static final int SCROLLER_X = 175;
	private static final int SCROLLER_Y = 18;
	private static final int SCROLLER_HEIGHT = 108;

	private EditBox search;

	public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 222);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();

		this.search = new EditBox(this.font, this.leftPos + 82, this.topPos + 5, 86, 12,
			Component.translatable("gui.tinman.storage_terminal.search"));
		this.search.setMaxLength(64);
		this.search.setBordered(false);
		this.search.setTextColor(0xE0F8FF);
		this.search.setHint(Component.translatable("gui.tinman.storage_terminal.search"));
		this.search.setResponder(text -> this.send(text, 0));
		this.addWidget(this.search);
	}

	private void send(String filter, int row) {
		ClientPlayNetworking.send(new TerminalViewPayload(filter, row));
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// The search box swallows keys while it has focus, or typing "e" to search for emeralds
		// would close the screen instead.
		if (this.search.isFocused() && event.key() != InputConstants.KEY_ESCAPE) {
			return this.search.keyPressed(event) || this.search.canConsumeInput();
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return this.search.charTyped(event) || super.charTyped(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int maxScroll = Math.max(0, this.menu.rowCount() - TerminalMenu.ROWS);

		if (maxScroll <= 0) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}

		int row = Math.clamp(this.menu.scrollRow() - (int) Math.signum(scrollY), 0, maxScroll);
		this.send(this.search.getValue(), row);
		return true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);

		int x = this.leftPos;
		int y = this.topPos;

		// Vanilla's own six-row chest panel: this grid is exactly that shape, and borrowing it
		// keeps the terminal looking like part of the game rather than beside it.
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE,
			x, y, 0.0F, 0.0F, this.imageWidth, 126, 256, 256);
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE,
			x, y + 126, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);

		// A recessed strip behind the search box, drawn rather than textured so the panel
		// underneath can stay vanilla's.
		graphics.fill(x + 79, y + 3, x + 170, y + 16, 0xFF373737);
		graphics.fill(x + 80, y + 4, x + 169, y + 15, 0xFF101418);

		drawScrollbar(graphics, x, y);
	}

	private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int y) {
		int maxScroll = Math.max(0, this.menu.rowCount() - TerminalMenu.ROWS);
		graphics.fill(x + SCROLLER_X, y + SCROLLER_Y, x + SCROLLER_X + 4, y + SCROLLER_Y + SCROLLER_HEIGHT, 0xFF373737);

		int height = maxScroll == 0 ? SCROLLER_HEIGHT
			: Math.max(12, SCROLLER_HEIGHT * TerminalMenu.ROWS / Math.max(1, this.menu.rowCount()));
		int travel = SCROLLER_HEIGHT - height;
		int offset = maxScroll == 0 ? 0 : travel * this.menu.scrollRow() / maxScroll;

		graphics.fill(x + SCROLLER_X, y + SCROLLER_Y + offset,
			x + SCROLLER_X + 4, y + SCROLLER_Y + offset + height, 0xFF3FE0E8);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		this.search.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// The title would run straight under the search box, so only the inventory label is drawn.
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
	}
}
