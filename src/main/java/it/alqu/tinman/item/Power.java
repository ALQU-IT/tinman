package it.alqu.tinman.item;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.enchantment.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Where powered gear gets its energy: batteries carried in the player's inventory.
 *
 * <p>The suit and the weapons store nothing themselves, so every cost in the mod is charged
 * through here.
 */
public final class Power {
	private Power() {
	}

	/** Every battery in the entity's inventory. Only players carry inventories, so others get none. */
	public static List<ItemStack> batteries(LivingEntity entity) {
		if (!(entity instanceof Player player)) {
			return List.of();
		}

		Inventory inventory = player.getInventory();
		List<ItemStack> found = new ArrayList<>();

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (Energy.stores(stack)) {
				found.add(stack);
			}
		}

		return found;
	}

	public static int available(LivingEntity entity) {
		return Energy.total(batteries(entity));
	}

	/**
	 * The best Conservation level across the carried batteries.
	 *
	 * <p>Taking the highest rather than the level of whichever battery happens to be drained first
	 * keeps costs predictable: the same action costs the same regardless of pack order.
	 */
	public static int conservationLevel(LivingEntity entity) {
		int best = 0;

		for (ItemStack battery : batteries(entity)) {
			best = Math.max(best, ModEnchantments.conservationLevel(entity.level(), battery));
		}

		return best;
	}

	/** What {@code amount} actually costs this entity once Conservation is applied. */
	public static int effectiveCost(LivingEntity entity, int amount) {
		if (amount <= 0) {
			return 0;
		}

		double perLevel = Math.max(0.0, TinManConfig.get().suit.conservationPerLevel);
		double factor = Math.clamp(1.0 - conservationLevel(entity) * perLevel, 0.05, 1.0);

		// Never round a real cost away to nothing.
		return Math.max(1, (int) Math.round(amount * factor));
	}

	/** True if the entity could pay {@code amount} right now. */
	public static boolean canPay(LivingEntity entity, int amount) {
		return amount <= 0 || available(entity) >= effectiveCost(entity, amount);
	}

	/**
	 * Charges {@code amount} against the carried batteries, all or nothing.
	 *
	 * @return true if it was paid in full
	 */
	public static boolean pay(LivingEntity entity, int amount) {
		if (amount <= 0) {
			return true;
		}

		int cost = effectiveCost(entity, amount);
		List<ItemStack> batteries = batteries(entity);

		if (Energy.total(batteries) < cost) {
			return false;
		}

		Energy.drainSpread(batteries, cost);
		return true;
	}
}
