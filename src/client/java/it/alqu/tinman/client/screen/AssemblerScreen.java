package it.alqu.tinman.client.screen;

import it.alqu.tinman.TinMan;
import it.alqu.tinman.client.recipebook.AssemblerRecipeBookComponent;
import it.alqu.tinman.menu.AssemblerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class AssemblerScreen extends AbstractRecipeBookScreen<AssemblerMenu> {
	private static final Identifier TEXTURE = TinMan.id("textures/gui/container/assembler.png");

	/** Position and size of the filled progress arrow inside the GUI texture. */
	private static final int ARROW_U = 176;
	private static final int ARROW_V = 0;
	private static final int ARROW_W = 24;
	private static final int ARROW_H = 17;
	private static final int ARROW_X = 90;
	private static final int ARROW_Y = 35;

	public AssemblerScreen(AssemblerMenu menu, Inventory inventory, Component title) {
		// AbstractRecipeBookScreen only offers the default 176x166, which the layout now fits.
		super(menu, new AssemblerRecipeBookComponent(menu), inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		// Leave room for the recipe book button, as the crafting table does.
		this.titleLabelX = 29;
	}

	@Override
	protected ScreenPosition getRecipeBookButtonPosition() {
		return new ScreenPosition(this.leftPos + 5, this.topPos + 51);
	}

	@Override
	protected boolean isBiggerResultSlot() {
		// The Assembler's output is an ordinary 16x16 slot, not the crafting table's larger one.
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);

		int x = this.leftPos;
		int y = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

		// Progress arrow fills left-to-right as the assembly runs.
		int filled = Mth.ceil(this.menu.assemblyProgress() * ARROW_W);

		if (filled > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
				x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V, filled, ARROW_H, 256, 256);
		}
	}
}
