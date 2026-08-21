package dev.alqu.tinman.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A Tin Man suit piece.
 *
 * <p>The suit stores no energy itself: its abilities run off whatever Voltite Battery the
 * wearer is carrying.
 */
public class TinManArmorItem extends Item {
	private static final int BAR_COLOUR = 0x3FE0E8;

	public TinManArmorItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);


		tooltip.accept(Component.translatable("tooltip.tinman.set_bonus").withStyle(ChatFormatting.DARK_GRAY));
		tooltip.accept(Component.translatable("tooltip.tinman.needs_battery").withStyle(ChatFormatting.DARK_GRAY));
	}
}
