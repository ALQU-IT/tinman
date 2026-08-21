package it.alqu.tinman.registry;

import it.alqu.tinman.TinMan;
import it.alqu.tinman.item.TinManArmorItem;
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
	 * Exactly double netherite on every defensive axis.
	 *
	 * <p>Netherite is 3/6/8/3 defence (20 points), 3.0 toughness and 0.1 knockback resistance per
	 * piece; this is 6/12/16/6 (40 points), 6.0 and 0.2, on double the durability multiplier.
	 *
	 * <p>Vanilla clamps the armour attribute at 30 and toughness at 20, so a full set reads 30 and
	 * 20 in game rather than 40 and 24. The excess is not wasted: the damage formula subtracts
	 * {@code damage / (2 + toughness / 4)} from the armour value before capping the result at 20,
	 * so the extra toughness is what keeps very large hits pinned at the 80% reduction ceiling
	 * instead of falling away from it.
	 */
	public static final ArmorMaterial TIN_MAN_MATERIAL = new ArmorMaterial(
		74,
		Map.of(
			ArmorType.BOOTS, 6,
			ArmorType.LEGGINGS, 12,
			ArmorType.CHESTPLATE, 16,
			ArmorType.HELMET, 6,
			ArmorType.BODY, 38
		),
		30,
		SoundEvents.ARMOR_EQUIP_NETHERITE,
		6.0F,
		0.2F,
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
