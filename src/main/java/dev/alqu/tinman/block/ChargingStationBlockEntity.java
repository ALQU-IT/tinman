package dev.alqu.tinman.block;

import dev.alqu.tinman.config.TinManConfig;
import dev.alqu.tinman.item.Energy;
import dev.alqu.tinman.menu.ChargingStationMenu;
import dev.alqu.tinman.registry.ModBlockEntities;
import dev.alqu.tinman.registry.ModItems;
import dev.alqu.tinman.suit.SuitEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Burns Voltite Ingots into an energy buffer and trickles that buffer into any gear it can reach:
 * the four items in its own slots, and the suits worn by players standing nearby.
 */
public class ChargingStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int FUEL_SLOT = 0;
	public static final int FIRST_GEAR_SLOT = 1;
	public static final int GEAR_SLOTS = 4;
	public static final int CONTAINER_SIZE = FIRST_GEAR_SLOT + GEAR_SLOTS;

	public static final int DATA_STORED = 0;
	public static final int DATA_CAPACITY = 1;
	public static final int DATA_COUNT = 2;

	/** Radius within which worn suits are topped up. */
	private static final double RANGE = 4.0;

	private static final int[] SLOTS_FUEL = {FUEL_SLOT};
	private static final int[] SLOTS_GEAR = {1, 2, 3, 4};

	private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
	private int stored;
	/**
	 * Fractional energy carried between ticks. Without this a rate that does not divide into 20
	 * (anything under 20/second) truncates to zero every tick and the station silently never
	 * charges. Not persisted: it is always worth less than one energy.
	 */
	private double chargeCarry;
	/**
	 * Rotates which target receives the remainder of an uneven split. Without it the same piece
	 * collects the leftover every tick and charges measurably faster than the rest.
	 */
	private int spreadCursor;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case DATA_STORED -> ChargingStationBlockEntity.this.stored;
				case DATA_CAPACITY -> capacity();
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			if (index == DATA_STORED) {
				ChargingStationBlockEntity.this.stored = value;
			}
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CHARGING_STATION, pos, state);
	}

	private static int capacity() {
		return Math.max(1, TinManConfig.get().suit.energyPerIngot) * 2;
	}

	public ContainerData getData() {
		return this.data;
	}

	@Override
	public int getContainerSize() {
		return CONTAINER_SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.tinman.charging_station");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChargingStationMenu(containerId, inventory, this, this.data);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
		this.stored = input.getIntOr("StoredEnergy", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		output.putInt("StoredEnergy", this.stored);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingStationBlockEntity entity) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		TinManConfig config = TinManConfig.get();
		int perIngot = Math.max(1, config.suit.energyPerIngot);

		// Top the buffer up from the fuel slot when there is room for a whole ingot.
		ItemStack fuel = entity.items.get(FUEL_SLOT);

		if (entity.stored + perIngot <= capacity() && fuel.is(ModItems.VOLTITE_INGOT)) {
			fuel.shrink(1);
			entity.stored += perIngot;
			entity.setChanged();
		}

		if (entity.stored <= 0) {
			// Don't bank carry while empty, or it would dump in one burst when fuel arrives.
			entity.chargeCarry = 0.0;
			entity.updateActiveState(serverLevel, pos, state, false);
			return;
		}

		entity.chargeCarry += Math.max(0, config.suit.chargingStationRate) / 20.0;
		int budget = (int) entity.chargeCarry;
		entity.chargeCarry -= budget;
		budget = Math.min(budget, entity.stored);

		if (budget <= 0) {
			entity.updateActiveState(serverLevel, pos, state, false);
			return;
		}

		int spent = entity.distribute(serverLevel, pos, budget);

		if (spent > 0) {
			entity.stored -= spent;
			entity.setChanged();
		}

		entity.updateActiveState(serverLevel, pos, state, spent > 0);
	}

	/**
	 * Feeds the gear slots first, then any suits worn within {@link #RANGE}.
	 *
	 * <p>Within each of those two groups the budget is split evenly, so a whole suit sitting in
	 * the slots fills together rather than one piece at a time.
	 */
	private int distribute(ServerLevel level, BlockPos pos, int budget) {
		int offset = this.spreadCursor++;
		int spent = spreadEvenly(this.gearTargets(), budget, offset);

		if (spent < budget) {
			spent += spreadEvenly(this.wornTargets(level, pos), budget - spent, offset);
		}

		return spent;
	}

	private List<ItemStack> gearTargets() {
		List<ItemStack> targets = new ArrayList<>(GEAR_SLOTS);

		for (int slot = FIRST_GEAR_SLOT; slot < CONTAINER_SIZE; slot++) {
			targets.add(this.items.get(slot));
		}

		return targets;
	}

	private List<ItemStack> wornTargets(ServerLevel level, BlockPos pos) {
		List<ItemStack> targets = new ArrayList<>();

		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(RANGE))) {
			targets.addAll(SuitEvents.suitPieces(player));
		}

		return targets;
	}

	/**
	 * Charges every target at the same time by handing each an equal share of the budget.
	 *
	 * <p>Runs in passes: anything that fills up drops out and its unused share is re-split among
	 * whatever still has room, so no energy is stranded on an almost-full piece.
	 *
	 * @return how much was actually spent
	 */
	private static int spreadEvenly(List<ItemStack> targets, int budget, int offset) {
		targets.removeIf(stack -> !Energy.stores(stack) || Energy.get(stack) >= Energy.max());

		if (targets.isEmpty()) {
			return 0;
		}

		// Shift the starting point each tick so the remainder of an uneven split lands on a
		// different piece every time, instead of always favouring the first one.
		Collections.rotate(targets, -Math.floorMod(offset, targets.size()));

		int spent = 0;

		while (spent < budget && !targets.isEmpty()) {
			// At least 1 apiece, so a budget smaller than the group still makes progress.
			int share = Math.max(1, (budget - spent) / targets.size());
			int before = spent;

			for (Iterator<ItemStack> it = targets.iterator(); it.hasNext() && spent < budget; ) {
				ItemStack stack = it.next();
				spent += Energy.charge(stack, Math.min(share, budget - spent));

				if (Energy.get(stack) >= Energy.max()) {
					it.remove();
				}
			}

			if (spent == before) {
				// Nothing could take any more; stop rather than spin.
				break;
			}
		}

		return spent;
	}

	private void updateActiveState(ServerLevel level, BlockPos pos, BlockState state, boolean active) {
		if (state.getValue(ChargingStationBlock.ACTIVE) != active) {
			level.setBlock(pos, state.setValue(ChargingStationBlock.ACTIVE, active), Block.UPDATE_ALL);
		}
	}

	// --- hoppers: ingots in from any side, finished gear out of the bottom

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? SLOTS_GEAR : (side == Direction.UP ? SLOTS_GEAR : SLOTS_FUEL);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return this.canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		// Only let a hopper pull gear back out once it is actually full.
		return slot >= FIRST_GEAR_SLOT && Energy.stores(stack) && Energy.get(stack) >= Energy.max();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == FUEL_SLOT ? stack.is(ModItems.VOLTITE_INGOT) : Energy.stores(stack);
	}
}
