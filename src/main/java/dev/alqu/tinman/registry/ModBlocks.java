package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;
import dev.alqu.tinman.block.AssemblerBlock;
import dev.alqu.tinman.block.ChargingStationBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

public final class ModBlocks {
	private ModBlocks() {
	}

	/**
	 * Voltite ore drops 3-7 XP, matching diamond. The actual item drop is decided by the loot
	 * table so that Silk Touch and Fortune behave correctly.
	 */
	public static final Block VOLTITE_ORE = register(
		"voltite_ore",
		p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.requiresCorrectToolForDrops()
			.strength(3.0F, 3.0F)
	);

	public static final Block DEEPSLATE_VOLTITE_ORE = register(
		"deepslate_voltite_ore",
		p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
		BlockBehaviour.Properties.ofLegacyCopy(VOLTITE_ORE)
			.mapColor(MapColor.DEEPSLATE)
			.strength(4.5F, 3.0F)
			.sound(SoundType.DEEPSLATE)
	);

	/** Storage block. Faintly glowing, so it doubles as decoration. */
	public static final Block VOLTITE_BLOCK = register(
		"voltite_block",
		Block::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.WARPED_WART_BLOCK)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL)
			.lightLevel(state -> 4)
	);

	/** The mod's crafting station. Holds a block entity, a GUI and its own recipe type. */
	public static final Block ASSEMBLER = register(
		"assembler",
		AssemblerBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.requiresCorrectToolForDrops()
			.strength(3.5F, 6.0F)
			.sound(SoundType.METAL)
			.lightLevel(state -> state.getValue(AssemblerBlock.CRAFTING) ? 7 : 0)
	);

	/** Trickle-charges suit pieces and weapons, in its slots or worn nearby. */
	public static final Block CHARGING_STATION = register(
		"charging_station",
		ChargingStationBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.requiresCorrectToolForDrops()
			.strength(3.5F, 6.0F)
			.sound(SoundType.METAL)
			.lightLevel(state -> state.getValue(ChargingStationBlock.ACTIVE) ? 9 : 2)
	);

	public static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, TinMan.id(name));
		Block block = factory.apply(properties.setId(key));
		Block registered = Registry.register(BuiltInRegistries.BLOCK, key, block);

		// Every block in this mod wants a matching BlockItem, registered under the same path.
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, TinMan.id(name));
		BlockItem blockItem = new BlockItem(registered, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

		return registered;
	}

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
