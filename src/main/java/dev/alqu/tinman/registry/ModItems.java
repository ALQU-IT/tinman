package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;

import java.util.function.Function;

public final class ModItems {
	private ModItems() {
	}

	/** Items that can repair a Voltite tool in an anvil. Populated by a datapack tag. */
	public static final TagKey<Item> REPAIRS_VOLTITE_TOOLS =
		TagKey.create(Registries.ITEM, TinMan.id("repairs_voltite_tools"));

	/**
	 * Sits between diamond and netherite on durability and damage, but mines faster than either.
	 * Mining tier matches diamond, so it can still handle obsidian and ancient debris.
	 */
	/** Deliberately overpowered: well past netherite on every axis. */
	public static final ToolMaterial VOLTITE_TOOL_MATERIAL = new ToolMaterial(
		BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
		4200,
		24.0F,
		6.0F,
		28,
		REPAIRS_VOLTITE_TOOLS
	);

	public static final Item RAW_VOLTITE = register("raw_voltite", new Item.Properties());
	public static final Item VOLTITE_INGOT = register("voltite_ingot", new Item.Properties());
	public static final Item VOLTITE_NUGGET = register("voltite_nugget", new Item.Properties());

	// Tools are deliberately NOT Assembler-locked; only armour and weapons are.
	public static final Item VOLTITE_PICKAXE = register("voltite_pickaxe",
		new Item.Properties().pickaxe(VOLTITE_TOOL_MATERIAL, 1.0F, -2.8F));
	public static final Item VOLTITE_SHOVEL = register("voltite_shovel",
		p -> new ShovelItem(VOLTITE_TOOL_MATERIAL, 1.5F, -3.0F, p), new Item.Properties());
	public static final Item VOLTITE_AXE = register("voltite_axe",
		p -> new AxeItem(VOLTITE_TOOL_MATERIAL, 5.0F, -3.0F, p), new Item.Properties());
	public static final Item VOLTITE_HOE = register("voltite_hoe",
		p -> new HoeItem(VOLTITE_TOOL_MATERIAL, -3.0F, 0.0F, p), new Item.Properties());

	public static Item register(String name, Item.Properties properties) {
		return register(name, Item::new, properties);
	}

	public static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TinMan.id(name));
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
