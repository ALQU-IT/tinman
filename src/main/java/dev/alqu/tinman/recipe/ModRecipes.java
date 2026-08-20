package dev.alqu.tinman.recipe;

import dev.alqu.tinman.TinMan;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
	private ModRecipes() {
	}

	/** {@code tinman:assembling} — the Assembler's own recipe type. */
	public static final RecipeType<AssemblingRecipe> ASSEMBLING = Registry.register(
		BuiltInRegistries.RECIPE_TYPE,
		TinMan.id("assembling"),
		new RecipeType<AssemblingRecipe>() {
			@Override
			public String toString() {
				return "tinman:assembling";
			}
		}
	);

	public static final RecipeSerializer<AssemblingRecipe> ASSEMBLING_SERIALIZER = Registry.register(
		BuiltInRegistries.RECIPE_SERIALIZER,
		TinMan.id("assembling"),
		new RecipeSerializer<>(AssemblingRecipe.MAP_CODEC, AssemblingRecipe.STREAM_CODEC)
	);

	public static final RecipeBookCategory ASSEMBLING_CATEGORY = Registry.register(
		BuiltInRegistries.RECIPE_BOOK_CATEGORY,
		TinMan.id("assembling"),
		new RecipeBookCategory()
	);

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
