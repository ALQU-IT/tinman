package dev.alqu.tinman.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A Tin Man suit piece.
 *
 * <p>The bar under the icon shows stored energy rather than durability, since energy is the
 * resource the player actually manages; remaining durability is spelled out in the tooltip so
 * nothing is hidden.
 */
public class TinManArmorItem extends Item implements Energy.EnergyStoring {
	private static final int BAR_COLOUR = 0x3FE0E8;

	public TinManArmorItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(Energy.get(stack) * 13.0F / Energy.max());
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOUR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);

		int energy = Energy.get(stack);
		int max = Energy.max();

		tooltip.accept(Component.translatable("tooltip.tinman.energy", energy, max)
			.withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));

		int maxDamage = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);

		if (maxDamage > 0) {
			int damage = stack.getOrDefault(DataComponents.DAMAGE, 0);
			tooltip.accept(Component.translatable("tooltip.tinman.durability", maxDamage - damage, maxDamage)
				.withStyle(ChatFormatting.DARK_GRAY));
		}

		tooltip.accept(Component.translatable("tooltip.tinman.set_bonus").withStyle(ChatFormatting.DARK_GRAY));
	}
}
