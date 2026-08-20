package dev.alqu.tinman;

import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.component.ModComponents;
import dev.alqu.tinman.recipe.ModRecipes;
import dev.alqu.tinman.registry.ModArmor;
import dev.alqu.tinman.suit.SuitEvents;
import dev.alqu.tinman.registry.ModBlockEntities;
import dev.alqu.tinman.registry.ModBlocks;
import dev.alqu.tinman.registry.ModItemGroup;
import dev.alqu.tinman.registry.ModItems;
import dev.alqu.tinman.registry.ModMenus;
import dev.alqu.tinman.worldgen.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TinMan implements ModInitializer {
	public static final String MOD_ID = "tinman";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		TinManConfig.get();

		ModBlocks.init();
		ModBlockEntities.init();
		ModComponents.init();
		ModItems.init();
		ModArmor.init();
		ModRecipes.init();
		ModMenus.init();
		SuitEvents.register();
		ModItemGroup.init();
		ModWorldGen.init();

		LOGGER.info("Tin Man online.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
