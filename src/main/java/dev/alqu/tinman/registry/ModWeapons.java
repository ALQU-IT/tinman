package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;
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

	/**
	 * A netherite sword lands 8 damage (4.0 material bonus + 3.0 sword baseline + the player's 1).
	 * This was already 16; doubling again puts the Voltite Blade at 31.
	 */
	public static final ToolMaterial BLADE_MATERIAL = new ToolMaterial(
		BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
		6000,
		32.0F,
		27.0F,
		30,
		REPAIRS_VOLTITE_WEAPONS
	);

	public static final Item VOLTITE_BLADE = ModItems.register("voltite_blade", VoltiteBladeItem::new,
		new Item.Properties()
			.sword(BLADE_MATERIAL, 3.0F, -1.8F));

	public static final Item PULSE_GAUNTLET = ModItems.register("pulse_gauntlet", PulseGauntletItem::new,
		new Item.Properties()
			.stacksTo(1)
			.durability(3000)
			.repairable(REPAIRS_VOLTITE_WEAPONS)
			.enchantable(28));

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
