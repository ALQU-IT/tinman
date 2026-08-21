package dev.alqu.tinman.enchantment;

import dev.alqu.tinman.TinMan;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class ModEnchantments {
	private ModEnchantments() {
	}

	/**
	 * Conservation: each level shaves a slice off the energy every draw costs.
	 *
	 * <p>Defined in {@code data/tinman/enchantment/conservation.json}. It carries no vanilla
	 * effects — the mod reads the level directly when charging a cost — so the JSON is only
	 * metadata telling the game how it may be obtained and what it goes on.
	 */
	public static final ResourceKey<Enchantment> CONSERVATION =
		ResourceKey.create(Registries.ENCHANTMENT, TinMan.id("conservation"));

	/**
	 * Level of Conservation on a stack, or 0.
	 *
	 * <p>Enchantments live in a datapack registry rather than a static one, so this needs a level
	 * to resolve the holder; without one there is nothing to look the key up in.
	 */
	public static int conservationLevel(Level level, ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}

		return level.registryAccess()
			.lookup(Registries.ENCHANTMENT)
			.flatMap(registry -> registry.get(CONSERVATION))
			.map(holder -> EnchantmentHelper.getItemEnchantmentLevel((Holder<Enchantment>) holder, stack))
			.orElse(0);
	}
}
