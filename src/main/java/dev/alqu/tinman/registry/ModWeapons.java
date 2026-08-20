package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;
import dev.alqu.tinman.component.ModComponents;
import dev.alqu.tinman.item.PulseGauntletItem;
import dev.alqu.tinman.item.VoltiteBladeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class ModWeapons {
	private ModWeapons() {
	}

	public static final TagKey<Item> REPAIRS_VOLTITE_WEAPONS =
		TagKey.create(Registries.ITEM, TinMan.id("repairs_voltite_weapons"));

	/** Sword tier between diamond (3.0 bonus) and netherite (4.0). */
	public static final ToolMaterial BLADE_MATERIAL = new ToolMaterial(
		BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
		1900,
		9.0F,
		3.5F,
		15,
		REPAIRS_VOLTITE_WEAPONS
	);

	public static final Item VOLTITE_BLADE = ModItems.register("voltite_blade", VoltiteBladeItem::new,
		new Item.Properties()
			.sword(BLADE_MATERIAL, 3.0F, -2.4F)
			.component(ModComponents.ENERGY, 0));

	public static final Item PULSE_GAUNTLET = ModItems.register("pulse_gauntlet", PulseGauntletItem::new,
		new Item.Properties()
			.stacksTo(1)
			.durability(512)
			.repairable(REPAIRS_VOLTITE_WEAPONS)
			.enchantable(10)
			.component(ModComponents.ENERGY, 0));

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
