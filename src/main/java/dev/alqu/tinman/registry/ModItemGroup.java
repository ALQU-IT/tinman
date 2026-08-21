package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** A single creative tab holding everything the mod adds. */
public final class ModItemGroup {
	private ModItemGroup() {
	}

	public static final ResourceKey<CreativeModeTab> TIN_MAN =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, TinMan.id("tin_man"));

	public static void init() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.tinman.tin_man"))
			.icon(() -> new ItemStack(ModItems.VOLTITE_INGOT))
			.displayItems((parameters, output) -> {
				output.accept(ModBlocks.VOLTITE_ORE);
				output.accept(ModBlocks.DEEPSLATE_VOLTITE_ORE);
				output.accept(ModBlocks.VOLTITE_BLOCK);
				output.accept(ModBlocks.ASSEMBLER);
				output.accept(ModBlocks.CHARGING_STATION);
				output.accept(ModItems.RAW_VOLTITE);
				output.accept(ModItems.VOLTITE_INGOT);
				output.accept(ModItems.VOLTITE_NUGGET);
				output.accept(ModItems.VOLTITE_BATTERY);
				output.accept(ModItems.VOLTITE_PICKAXE);
				output.accept(ModItems.VOLTITE_AXE);
				output.accept(ModItems.VOLTITE_SHOVEL);
				output.accept(ModItems.VOLTITE_HOE);
				output.accept(ModArmor.TIN_MAN_HELMET);
				output.accept(ModArmor.TIN_MAN_CHESTPLATE);
				output.accept(ModArmor.TIN_MAN_LEGGINGS);
				output.accept(ModArmor.TIN_MAN_BOOTS);
				output.accept(ModWeapons.VOLTITE_BLADE);
				output.accept(ModWeapons.PULSE_GAUNTLET);
			})
			.build();

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TIN_MAN, tab);
	}
}
