package dev.alqu.tinman.item;

import dev.alqu.tinman.component.ModComponents;
import dev.alqu.tinman.config.TinManConfig;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Read/write helpers for the energy data component.
 *
 * <p>Everything that stores energy — the four suit pieces, the Pulse Gauntlet and the Voltite
 * Blade — goes through here, so the clamping rules live in exactly one place.
 */
public final class Energy {
	private Energy() {
	}

	public static int max() {
		return Math.max(1, TinManConfig.get().suit.maxEnergy);
	}

	public static boolean stores(ItemStack stack) {
		return stack.getItem() instanceof EnergyStoring;
	}

	public static int get(ItemStack stack) {
		return Math.clamp(stack.getOrDefault(ModComponents.ENERGY, 0), 0, max());
	}

	public static void set(ItemStack stack, int energy) {
		stack.set(ModComponents.ENERGY, Math.clamp(energy, 0, max()));
	}

	/** Adds energy and returns how much was actually accepted. */
	public static int charge(ItemStack stack, int amount) {
		if (amount <= 0 || !stores(stack)) {
			return 0;
		}

		int before = get(stack);
		int accepted = Math.min(amount, max() - before);
		set(stack, before + accepted);
		return accepted;
	}

	/** Removes energy and returns how much was actually taken. */
	public static int drain(ItemStack stack, int amount) {
		if (amount <= 0 || !stores(stack)) {
			return 0;
		}

		int before = get(stack);
		int taken = Math.min(amount, before);
		set(stack, before - taken);
		return taken;
	}

	public static int total(List<ItemStack> stacks) {
		int total = 0;

		for (ItemStack stack : stacks) {
			if (stores(stack)) {
				total += get(stack);
			}
		}

		return total;
	}

	/**
	 * Drains {@code amount} spread across the given stacks, taking from the fullest first so the
	 * set runs down evenly rather than killing one piece at a time.
	 *
	 * @return how much was actually drained
	 */
	public static int drainSpread(List<ItemStack> stacks, int amount) {
		int remaining = amount;

		while (remaining > 0) {
			ItemStack fullest = null;
			int best = 0;

			for (ItemStack stack : stacks) {
				if (stores(stack)) {
					int energy = get(stack);

					if (energy > best) {
						best = energy;
						fullest = stack;
					}
				}
			}

			if (fullest == null) {
				break;
			}

			remaining -= drain(fullest, Math.min(remaining, best));
		}

		return amount - remaining;
	}

	/** Marker for items that carry the energy component. */
	public interface EnergyStoring {
	}
}
