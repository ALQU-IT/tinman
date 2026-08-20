package dev.alqu.tinman.menu;

import dev.alqu.tinman.block.AssemblerBlockEntity;
import dev.alqu.tinman.registry.ModBlocks;
import dev.alqu.tinman.registry.ModItems;
import dev.alqu.tinman.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AssemblerMenu extends AbstractContainerMenu {
	private static final int GRID_START = 0;
	private static final int GRID_END = AssemblerBlockEntity.GRID_SIZE;
	private static final int POWER_SLOT = AssemblerBlockEntity.POWER_SLOT;
	private static final int OUTPUT_SLOT = AssemblerBlockEntity.OUTPUT_SLOT;
	private static final int CONTAINER_END = AssemblerBlockEntity.CONTAINER_SIZE;
	private static final int PLAYER_INVENTORY_END = CONTAINER_END + 27;
	private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

	private final Container container;
	private final ContainerData data;

	/** Client-side constructor: the real contents arrive through the usual container sync. */
	public AssemblerMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(AssemblerBlockEntity.CONTAINER_SIZE),
			new SimpleContainerData(AssemblerBlockEntity.DATA_COUNT));
	}

	public AssemblerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
		super(ModMenus.ASSEMBLER, containerId);
		checkContainerSize(container, AssemblerBlockEntity.CONTAINER_SIZE);
		checkContainerDataCount(data, AssemblerBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;

		// 3x3 assembly grid
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				this.addSlot(new Slot(container, col + row * 3, 30 + col * 18, 17 + row * 18));
			}
		}

		// Power cell: Voltite Ingots only.
		this.addSlot(new Slot(container, POWER_SLOT, 30, 75) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModItems.VOLTITE_INGOT);
			}
		});

		// Output: take-only.
		this.addSlot(new Slot(container, OUTPUT_SLOT, 124, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});

		this.addStandardInventorySlots(playerInventory, 8, 104);
		this.addDataSlots(data);
	}

	/** 0-1 fraction of the current assembly, for the progress arrow. */
	public float assemblyProgress() {
		int total = this.data.get(AssemblerBlockEntity.DATA_TOTAL);
		int progress = this.data.get(AssemblerBlockEntity.DATA_PROGRESS);
		return total == 0 ? 0.0F : Math.clamp(progress / (float) total, 0.0F, 1.0F);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index < CONTAINER_END) {
			// Anything in the machine goes back to the player.
			if (!this.moveItemStackTo(stack, CONTAINER_END, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			slot.onQuickCraft(stack, original);
		} else if (stack.is(ModItems.VOLTITE_INGOT) && this.moveItemStackTo(stack, POWER_SLOT, POWER_SLOT + 1, false)) {
			// Voltite Ingots fill the power cell first.
		} else if (this.moveItemStackTo(stack, GRID_START, GRID_END, false)) {
			// Then the assembly grid.
		} else if (index < PLAYER_INVENTORY_END) {
			if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, CONTAINER_END, PLAYER_INVENTORY_END, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		slot.onTake(player, stack);
		return original;
	}
}
