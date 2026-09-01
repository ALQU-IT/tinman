package it.alqu.tinman;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.advancement.ModTriggers;
import it.alqu.tinman.component.ModComponents;
import it.alqu.tinman.recipe.ModRecipes;
import it.alqu.tinman.registry.ModArmor;
import it.alqu.tinman.suit.SuitEvents;
import it.alqu.tinman.storage.TerminalNetworking;
import it.alqu.tinman.suit.Unibeam;
import it.alqu.tinman.registry.ModBlockEntities;
import it.alqu.tinman.registry.ModBlocks;
import it.alqu.tinman.registry.ModEntities;
import it.alqu.tinman.registry.ModParticles;
import it.alqu.tinman.registry.ModSounds;
import it.alqu.tinman.registry.ModWeapons;
import it.alqu.tinman.registry.ModItemGroup;
import it.alqu.tinman.registry.ModItems;
import it.alqu.tinman.registry.ModMenus;
import it.alqu.tinman.worldgen.ModWorldGen;
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
		ModWeapons.init();
		ModEntities.init();
		ModSounds.init();
		ModParticles.init();
		ModTriggers.init();
		ModRecipes.init();
		ModMenus.init();
		SuitEvents.register();
		Unibeam.register();
		TerminalNetworking.register();
		ModItemGroup.init();
		ModWorldGen.init();

		LOGGER.info("Tin Man online.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
