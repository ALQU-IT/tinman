package dev.alqu.tinman.worldgen;

import dev.alqu.tinman.TinMan;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModWorldGen {
	private ModWorldGen() {
	}

	/** Larger veins, defined in {@code data/tinman/worldgen/placed_feature/voltite_ore.json}. */
	public static final ResourceKey<PlacedFeature> VOLTITE_ORE =
		ResourceKey.create(Registries.PLACED_FEATURE, TinMan.id("voltite_ore"));

	/** Smaller veins, so the effective vein size lands in the 4-6 range. */
	public static final ResourceKey<PlacedFeature> VOLTITE_ORE_SMALL =
		ResourceKey.create(Registries.PLACED_FEATURE, TinMan.id("voltite_ore_small"));

	public static void init() {
		Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, TinMan.id("config_count"), ConfigCountPlacement.TYPE);

		// The features themselves live in JSON; we only decide which biomes they apply to.
		// ConfigCountPlacement returns 0 when the ore is disabled, so no extra gate is needed here
		// (and doing it here would bake the config into the world at load time instead).
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, VOLTITE_ORE);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, VOLTITE_ORE_SMALL);
	}
}
