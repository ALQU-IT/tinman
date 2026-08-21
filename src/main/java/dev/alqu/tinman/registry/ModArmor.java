package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;
import dev.alqu.tinman.item.TinManArmorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

public final class ModArmor {
	private ModArmor() {
	}

	/** Items that repair a suit piece in an anvil. */
	public static final TagKey<Item> REPAIRS_TIN_MAN_ARMOR =
		TagKey.create(Registries.ITEM, TinMan.id("repairs_tin_man_armor"));

	/** Points at {@code assets/tinman/equipment/tin_man.json}. */
	public static final ResourceKey<EquipmentAsset> TIN_MAN_ASSET =
		ResourceKey.create(EquipmentAssets.ROOT_ID, TinMan.id("tin_man"));

	/**
	 * Armour points come out one above diamond (21 vs 20) while toughness and knockback resistance
	 * sit between diamond and netherite, so the suit protects a little better than diamond without
	 * reaching netherite.
	 */
	public static final ArmorMaterial TIN_MAN_MATERIAL = new ArmorMaterial(
		40,
		Map.of(
			ArmorType.BOOTS, 3,
			ArmorType.LEGGINGS, 6,
			ArmorType.CHESTPLATE, 8,
			ArmorType.HELMET, 4,
			ArmorType.BODY, 11
		),
		16,
		SoundEvents.ARMOR_EQUIP_NETHERITE,
		2.5F,
		0.05F,
		REPAIRS_TIN_MAN_ARMOR,
		TIN_MAN_ASSET
	);

	public static final Item TIN_MAN_HELMET = register("tin_man_helmet", ArmorType.HELMET);
	public static final Item TIN_MAN_CHESTPLATE = register("tin_man_chestplate", ArmorType.CHESTPLATE);
	public static final Item TIN_MAN_LEGGINGS = register("tin_man_leggings", ArmorType.LEGGINGS);
	public static final Item TIN_MAN_BOOTS = register("tin_man_boots", ArmorType.BOOTS);

	private static Item register(String name, ArmorType type) {
		return ModItems.register(name, TinManArmorItem::new,
			new Item.Properties().humanoidArmor(TIN_MAN_MATERIAL, type));
	}

	public static boolean isSuitPiece(ItemStack stack) {
		return stack.getItem() instanceof TinManArmorItem;
	}

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
