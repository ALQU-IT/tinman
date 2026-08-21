package dev.alqu.tinman.item;

import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.item.Power;
import dev.alqu.tinman.suit.SuitEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A sword that hits harder while its wielder has charge to spend.
 *
 * <p>The bonus is applied in {@link #getAttackDamageBonus} so it folds into the normal damage
 * calculation, and the energy is actually spent in {@link #postHurtEnemy} once the hit has landed.
 */
public class VoltiteBladeItem extends Item {
	private static final int BAR_COLOUR = 0x3FE0E8;

	public VoltiteBladeItem(Properties properties) {
		super(properties);
	}

	@Override
	public float getAttackDamageBonus(Entity target, float baseDamage, DamageSource source) {
		float bonus = super.getAttackDamageBonus(target, baseDamage, source);
		TinManConfig.Weapons config = TinManConfig.get().weapons;

		if (!(source.getEntity() instanceof LivingEntity attacker)) {
			return bonus;
		}

		ItemStack weapon = attacker.getItemBySlot(EquipmentSlot.MAINHAND);

		if (weapon.getItem() != this) {
			return bonus;
		}

		// Only promise the bonus if the energy is actually there to pay for it, at the
		// Conservation-discounted price the hit will really be charged.
		if (Power.canPay(attacker, config.bladeEnergyCostPerHit)) {
			return bonus + (float) config.bladeEnergyBonusDamage;
		}

		return bonus;
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.postHurtEnemy(stack, target, attacker);

		TinManConfig.Weapons config = TinManConfig.get().weapons;

		if (!SuitEvents.drawPower(attacker, stack, config.bladeEnergyCostPerHit)) {
			return;
		}

		if (attacker.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
				12, 0.3, 0.3, 0.3, 0.25);
			level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
				SoundSource.PLAYERS, 0.35F, 2.0F);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);

		TinManConfig.Weapons config = TinManConfig.get().weapons;

		tooltip.accept(Component.translatable("tooltip.tinman.needs_battery")
			.withStyle(ChatFormatting.DARK_GRAY));
		tooltip.accept(Component.translatable("tooltip.tinman.blade",
				String.format("%.1f", config.bladeEnergyBonusDamage), config.bladeEnergyCostPerHit)
			.withStyle(ChatFormatting.DARK_GRAY));
	}
}
