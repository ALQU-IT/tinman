package it.alqu.tinman.menu;

import it.alqu.tinman.block.ChargingStationBlockEntity;
import it.alqu.tinman.item.Energy;
import it.alqu.tinman.registry.ModItems;
import it.alqu.tinman.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ChargingStationMenu extends AbstractContainerMenu {
	private static final int FUEL_SLOT = ChargingStationBlockEntity.FUEL_SLOT;
	private static final int FIRST_GEAR = ChargingStationBlockEntity.FIRST_GEAR_SLOT;
	private static final int CONTAINER_END = ChargingStationBlockEntity.CONTAINER_SIZE;
	private static final int PLAYER_INVENTORY_END = CONTAINER_END + 27;
	private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

	private final Container container;
	private final ContainerData data;

	public ChargingStationMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(ChargingStationBlockEntity.CONTAINER_SIZE),
			new SimpleContainerData(ChargingStationBlockEntity.DATA_COUNT));
	}

	public ChargingStationMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
		super(ModMenus.CHARGING_STATION, containerId);
		checkContainerSize(container, ChargingStationBlockEntity.CONTAINER_SIZE);
		checkContainerDataCount(data, ChargingStationBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;

		this.addSlot(new Slot(container, FUEL_SLOT, 26, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModItems.VOLTITE_INGOT);
			}
		});

		for (int i = 0; i < ChargingStationBlockEntity.GEAR_SLOTS; i++) {
			this.addSlot(new Slot(container, FIRST_GEAR + i, 80 + i * 18, 35) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return Energy.stores(stack);
				}
			});
		}

		this.addStandardInventorySlots(playerInventory, 8, 84);
		this.addDataSlots(data);
	}

	/** 0-1 fill of the internal buffer, for the charge gauge. */
	public float bufferFill() {
		int capacity = this.data.get(ChargingStationBlockEntity.DATA_CAPACITY);
		int stored = this.data.get(ChargingStationBlockEntity.DATA_STORED);
		return capacity == 0 ? 0.0F : Math.clamp(stored / (float) capacity, 0.0F, 1.0F);
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
			if (!this.moveItemStackTo(stack, CONTAINER_END, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			slot.onQuickCraft(stack, original);
		} else if (stack.is(ModItems.VOLTITE_INGOT) && this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
			// Fuel first.
		} else if (Energy.stores(stack) && this.moveItemStackTo(stack, FIRST_GEAR, CONTAINER_END, false)) {
			// Then anything chargeable.
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
