package dev.alqu.tinman.item;

import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.entity.PulseBolt;
import dev.alqu.tinman.registry.ModSounds;
import dev.alqu.tinman.suit.SuitEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Fires energy bolts. A tap shoots straight away; holding the button long enough charges a heavier
 * shot that also sets off a small blast which damages mobs but never breaks blocks.
 */
public class PulseGauntletItem extends net.minecraft.world.item.Item implements Energy.EnergyStoring {
	private static final int BAR_COLOUR = 0x3FE0E8;

	public PulseGauntletItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		// Always start "using": releaseUsing decides tap vs charged from how long it was held.
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) {
			return false;
		}

		TinManConfig.Weapons config = TinManConfig.get().weapons;
		int held = this.getUseDuration(stack, entity) - timeLeft;
		boolean charged = config.chargedShotEnabled && held >= Math.max(1, config.pulseChargeTicks);

		int cost = charged ? config.chargedPulseEnergyCost : config.pulseEnergyCost;

		if (!SuitEvents.drawPower(entity, stack, cost)) {
			if (level instanceof ServerLevel) {
				level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7F, 1.2F);
			}

			return false;
		}

		if (level instanceof ServerLevel serverLevel) {
			PulseBolt bolt = new PulseBolt(serverLevel, player);
			bolt.configure((float) (charged ? config.chargedPulseDamage : config.pulseDamage), charged);
			bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, charged ? 2.2F : 1.8F, 0.4F);
			serverLevel.addFreshEntity(bolt);

			level.playSound(null, player.blockPosition(), ModSounds.REPULSOR_FIRE,
				SoundSource.PLAYERS, charged ? 1.0F : 0.7F, charged ? 0.75F : 1.25F);
		}

		player.getCooldowns().addCooldown(stack, Math.max(1, charged ? config.pulseCooldownTicks * 3 : config.pulseCooldownTicks));
		return true;
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

		TinManConfig.Weapons config = TinManConfig.get().weapons;

		tooltip.accept(Component.translatable("tooltip.tinman.energy", Energy.get(stack), Energy.max())
			.withStyle(ChatFormatting.AQUA));
		tooltip.accept(Component.translatable("tooltip.tinman.gauntlet", (int) config.pulseDamage, config.pulseEnergyCost)
			.withStyle(ChatFormatting.DARK_GRAY));

		if (config.chargedShotEnabled) {
			tooltip.accept(Component.translatable("tooltip.tinman.gauntlet_charged",
					(int) config.chargedPulseDamage, config.chargedPulseEnergyCost)
				.withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
