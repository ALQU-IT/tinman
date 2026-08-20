package dev.alqu.tinman.client.recipebook;

import dev.alqu.tinman.menu.AssemblerMenu;
import dev.alqu.tinman.recipe.ModRecipes;
import dev.alqu.tinman.registry.ModBlocks;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.List;

/**
 * The Assembler's recipe book: the same widget the crafting table uses, listing only
 * {@code tinman:assembling} recipes.
 *
 * <p>Reuses vanilla's recipe book sprites so it matches the rest of the game's UI rather than
 * shipping a near-identical copy of them.
 */
public class AssemblerRecipeBookComponent extends RecipeBookComponent<AssemblerMenu> {
	private static final WidgetSprites FILTER_BUTTON_SPRITES = new WidgetSprites(
		Identifier.withDefaultNamespace("recipe_book/filter_enabled"),
		Identifier.withDefaultNamespace("recipe_book/filter_disabled"),
		Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
		Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted")
	);

	/** One tab: every Assembler recipe lives in the same category. */
	private static List<RecipeBookComponent.TabInfo> tabs() {
		return List.of(new RecipeBookComponent.TabInfo(ModBlocks.ASSEMBLER.asItem(), ModRecipes.ASSEMBLING_CATEGORY));
	}

	public AssemblerRecipeBookComponent(AssemblerMenu menu) {
		super(menu, tabs());
	}

	@Override
	protected WidgetSprites getFilterButtonTextures() {
		return FILTER_BUTTON_SPRITES;
	}

	@Override
	protected Component getRecipeFilterName() {
		return Component.translatable("gui.tinman.assembler.toggleRecipes");
	}

	@Override
	protected boolean isCraftingSlot(Slot slot) {
		return this.menu.isRecipeBookSlot(slot);
	}

	/** The Assembler's grid is a fixed 3x3, so any shaped recipe that fits a table fits here. */
	private boolean canDisplay(RecipeDisplay display) {
		return display instanceof ShapedCraftingRecipeDisplay shaped
			&& shaped.width() <= 3
			&& shaped.height() <= 3;
	}

	@Override
	protected void selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents) {
		collection.selectRecipes(stackedContents, this::canDisplay);
	}

	@Override
	protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipe, ContextMap context) {
		ghostSlots.setResult(this.menu.getResultSlot(), context, recipe.result());

		if (!(recipe instanceof ShapedCraftingRecipeDisplay shaped)) {
			return;
		}

		List<Slot> grid = this.menu.getInputGridSlots();
		List<SlotDisplay> ingredients = shaped.ingredients();

		// Recipes narrower than the grid are drawn in the top-left, matching the crafting table.
		for (int y = 0; y < shaped.height(); y++) {
			for (int x = 0; x < shaped.width(); x++) {
				SlotDisplay ingredient = ingredients.get(x + y * shaped.width());

				if (!(ingredient instanceof SlotDisplay.Empty)) {
					ghostSlots.setInput(grid.get(x + y * 3), context, ingredient);
				}
			}
		}
	}
}
