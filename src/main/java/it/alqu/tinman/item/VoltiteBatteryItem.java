package it.alqu.tinman.item;

import it.alqu.tinman.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * The one thing in the mod that stores energy.
 *
 * <p>Carrying a charged battery is what powers the suit and the weapons; they hold no charge of
 * their own. Charge it in the Assembler or the Charging Station.
 */
public class VoltiteBatteryItem extends Item implements Energy.EnergyStoring {
	private static final int BAR_COLOUR = 0x3FE0E8;

	public VoltiteBatteryItem(Properties properties) {
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

		tooltip.accept(Component.translatable("tooltip.tinman.energy", energy, Energy.max())
			.withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
		tooltip.accept(Component.translatable("tooltip.tinman.battery_hint")
			.withStyle(ChatFormatting.DARK_GRAY));
	}
}
