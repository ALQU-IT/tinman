package it.alqu.tinman.menu;

import it.alqu.tinman.registry.ModMenus;
import it.alqu.tinman.storage.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The Storage Terminal's menu: every chest in range pooled into one searchable grid.
 *
 * <p>The grid is a display, not an inventory. Its slots are backed by a scratch container the
 * server refills from a fresh scan, and nothing may ever be put into them — the real items stay in
 * the chests until they are asked for. Clicking a slot is therefore handled here rather than by
 * the usual slot mechanics, which would happily let a player pick up a stack that does not exist.
 *
 * <p>Clicks never use the cursor, deliberately. Cursor-held items are the fiddliest thing to keep
 * in step between client and server, and a terminal has no need for them: a click puts the items
 * straight into the player's inventory.
 */
public class TerminalMenu extends AbstractContainerMenu {
	public static final int COLUMNS = 9;
	public static final int ROWS = 6;
	public static final int VIEW_SIZE = COLUMNS * ROWS;

	/** Ticks between rescans while the menu is open, so chests filled elsewhere still show up. */
	private static final int REFRESH_TICKS = 10;

	private final Container view = new SimpleContainer(VIEW_SIZE);
	private final @Nullable BlockEntity terminal;
	private final Player player;

	/** The pooled contents behind the current page, one per view slot; null where the grid is empty. */
	private final Network.Entry[] shown = new Network.Entry[VIEW_SIZE];

	private String filter = "";
	private int scrollRow;
	private int rowCount;
	private int refreshCounter;

	public TerminalMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, null);
	}

	public TerminalMenu(int containerId, Inventory playerInventory, @Nullable BlockEntity terminal) {
		super(ModMenus.STORAGE_TERMINAL, containerId);
		this.terminal = terminal;
		this.player = playerInventory.player;

		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				this.addSlot(new ViewSlot(this.view, row * COLUMNS + column, 8 + column * 18, 18 + row * 18));
			}
		}

		this.addStandardInventorySlots(playerInventory, 8, 140);
		this.refresh();
	}

	/** A window onto what the chests hold. Take-only, and never a destination for anything. */
	private static class ViewSlot extends Slot {
		private ViewSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			// Every withdrawal goes through TerminalMenu#clicked, which knows how to take the
			// items out of the chests as well. Ordinary pickup would conjure them from nowhere.
			return false;
		}
	}

	/** How many rows of results the current filter produced, for the scrollbar. */
	public int rowCount() {
		return this.rowCount;
	}

	public int scrollRow() {
		return this.scrollRow;
	}

	/** Applies a new search term and page, and rebuilds the grid to match. */
	public void setView(String filter, int scrollRow) {
		this.filter = filter == null ? "" : filter;
		this.scrollRow = Math.max(0, scrollRow);
		this.refresh();
		this.broadcastFullState();
	}

	@Override
	public void broadcastChanges() {
		// Chests in range are filled and emptied by hoppers, other players and droppers while the
		// terminal is open, so the page is rebuilt periodically rather than only on interaction.
		if (++this.refreshCounter >= REFRESH_TICKS) {
			this.refreshCounter = 0;
			this.refresh();
		}

		super.broadcastChanges();
	}

	/** Rescans the chests in range and repaints the current page of results. */
	private void refresh() {
		java.util.Arrays.fill(this.shown, null);

		if (!(this.player.level() instanceof ServerLevel level) || this.terminal == null) {
			this.rowCount = 0;
			return;
		}

		List<Network.Entry> entries = Network.pool(Network.containers(level, this.terminal.getBlockPos()));
		entries.removeIf(entry -> !matches(entry, this.filter));

		this.rowCount = Math.ceilDiv(entries.size(), COLUMNS);
		int maxScroll = Math.max(0, this.rowCount - ROWS);
		this.scrollRow = Math.min(this.scrollRow, maxScroll);

		int first = this.scrollRow * COLUMNS;

		for (int slot = 0; slot < VIEW_SIZE; slot++) {
			int index = first + slot;

			if (index >= entries.size()) {
				this.view.setItem(slot, ItemStack.EMPTY);
				continue;
			}

			Network.Entry entry = entries.get(index);
			this.shown[slot] = entry;

			// The count shown is the real pooled total, not a stack: the whole point is to see
			// that there are 412 cobblestone in the room without opening nine chests.
			this.view.setItem(slot, entry.sample().copyWithCount(entry.total()));
		}
	}

	private static boolean matches(Network.Entry entry, String filter) {
		if (filter.isEmpty()) {
			return true;
		}

		String needle = filter.toLowerCase(Locale.ROOT);
		ItemStack stack = entry.sample();

		return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)
			|| stack.typeHolder().getRegisteredName().toLowerCase(Locale.ROOT).contains(needle);
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId < 0 || slotId >= VIEW_SIZE || !(player instanceof ServerPlayer)) {
			super.clicked(slotId, button, input, player);
			return;
		}

		Network.Entry entry = this.shown[slotId];

		if (entry == null || input == ContainerInput.QUICK_CRAFT) {
			return;
		}

		// Left for a stack, right for a single item. Nothing here touches the cursor.
		int wanted = button == 1 && input == ContainerInput.PICKUP
			? 1
			: entry.sample().getMaxStackSize();

		withdraw(entry, wanted);
		this.refresh();
		this.broadcastFullState();
	}

	/** Pulls items out of the chests and hands them to the player, dropping any that will not fit. */
	private void withdraw(Network.Entry entry, int wanted) {
		ItemStack taken = entry.take(wanted);

		if (taken.isEmpty()) {
			return;
		}

		if (!this.player.getInventory().add(taken) && !taken.isEmpty()) {
			this.player.drop(taken, false);
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < VIEW_SIZE) {
			// Shift-clicking a result is the same ask as clicking it: give me a stack of that.
			Network.Entry entry = this.shown[index];

			if (entry != null && player instanceof ServerPlayer) {
				withdraw(entry, entry.sample().getMaxStackSize());
				this.refresh();
				this.broadcastFullState();
			}

			return ItemStack.EMPTY;
		}

		return deposit(player, index);
	}

	/**
	 * Shift-clicking from the player's own inventory sends the stack out to the chests.
	 *
	 * <p>The other half of the same gesture: if the terminal can hand things out of the room's
	 * storage, the obvious thing for the reverse motion to do is put them back.
	 */
	private ItemStack deposit(Player player, int index) {
		if (!(player.level() instanceof ServerLevel level) || this.terminal == null) {
			return ItemStack.EMPTY;
		}

		Slot slot = this.slots.get(index);
		ItemStack stack = slot.getItem();

		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		List<Container> containers = Network.containers(level, this.terminal.getBlockPos());

		// Merging before filling empty slots keeps storage tidy, the same order a hopper uses.
		insert(containers, stack, true);
		insert(containers, stack, false);

		slot.setByPlayer(stack);
		this.refresh();
		return ItemStack.EMPTY;
	}

	private static void insert(List<Container> containers, ItemStack stack, boolean mergeOnly) {
		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
				ItemStack held = container.getItem(slot);

				if (held.isEmpty()) {
					if (mergeOnly || !container.canPlaceItem(slot, stack)) {
						continue;
					}

					int moved = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), container.getMaxStackSize()));
					container.setItem(slot, stack.split(moved));
					container.setChanged();
					continue;
				}

				if (!ItemStack.isSameItemSameComponents(held, stack)) {
					continue;
				}

				int room = Math.min(held.getMaxStackSize(), container.getMaxStackSize()) - held.getCount();
				int moved = Math.min(room, stack.getCount());

				if (moved > 0) {
					held.grow(moved);
					stack.shrink(moved);
					container.setChanged();
				}
			}

			if (stack.isEmpty()) {
				return;
			}
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return this.terminal == null || Container.stillValidBlockEntity(this.terminal, player);
	}
}
