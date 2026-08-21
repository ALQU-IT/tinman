package it.alqu.tinman.block;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.item.Energy;
import it.alqu.tinman.menu.ChargingStationMenu;
import it.alqu.tinman.registry.ModBlockEntities;
import it.alqu.tinman.registry.ModBlocks;
import it.alqu.tinman.registry.ModItems;
import it.alqu.tinman.suit.SuitEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
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
import java.util.List;

/**
 * Burns Voltite Ingots into an energy buffer and pours that buffer into any gear it can reach:
 * the four items in its own slots, and the suits worn by players standing nearby.
 *
 * <p>Since the Assembler stopped charging, this is the only way to put energy into gear.
 *
 * <p>Fuel is Voltite in either form. A Block of Voltite is nine ingots, so it burns as nine
 * ingots' worth — feeding the station a stack of blocks rather than ingots is nine times the
 * fuel in the same slot, and worth exactly the same per ingot.
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

	/** Ingots in a Block of Voltite, and so the ratio between their fuel values. */
	private static final int INGOTS_PER_BLOCK = 9;

	private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
	private int stored;
	/**
	 * Energy left in the fuel item currently being burned.
	 *
	 * <p>Fuel is opened into this and then trickled into the buffer, rather than dropped into the
	 * buffer whole. Otherwise the buffer would have to be big enough to swallow a Block of
	 * Voltite in one go, and a station holding that much would be an expensive thing to break.
	 */
	private int fuelRemainder;
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

	/** Energy one item of this stack is worth as fuel, or 0 if it does not burn here. */
	public static int fuelValue(ItemStack stack) {
		int perIngot = Math.max(1, TinManConfig.get().suit.energyPerIngot);

		if (stack.is(ModItems.VOLTITE_INGOT)) {
			return perIngot;
		}

		return stack.is(ModBlocks.VOLTITE_BLOCK.asItem()) ? perIngot * INGOTS_PER_BLOCK : 0;
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
		this.fuelRemainder = input.getIntOr("FuelRemainder", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		output.putInt("StoredEnergy", this.stored);
		output.putInt("FuelRemainder", this.fuelRemainder);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingStationBlockEntity entity) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		TinManConfig config = TinManConfig.get();

		entity.refuel();

		if (entity.stored <= 0) {
			// Don't bank carry while empty, or it would dump in one burst when fuel arrives.
			entity.chargeCarry = 0.0;
			entity.updateActiveState(serverLevel, pos, state, false);
			return;
		}

		entity.chargeCarry += Math.max(0, config.suit.chargingStationRate) / 20.0;
		int perTarget = (int) entity.chargeCarry;
		entity.chargeCarry -= perTarget;

		if (perTarget <= 0) {
			entity.updateActiveState(serverLevel, pos, state, false);
			return;
		}

		int spent = entity.distribute(serverLevel, pos, perTarget);

		if (spent > 0) {
			entity.stored -= spent;
			entity.setChanged();
		}

		entity.updateActiveState(serverLevel, pos, state, spent > 0);
	}

	/**
	 * Opens the next fuel item once the last one is spent, then trickles it into the buffer.
	 *
	 * <p>An ingot and a Block of Voltite go through the same path; the block simply lasts nine
	 * times as long, so nothing has to know how big a single fuel item is.
	 */
	private void refuel() {
		if (this.fuelRemainder <= 0) {
			ItemStack fuel = this.items.get(FUEL_SLOT);
			int value = fuelValue(fuel);

			if (value > 0) {
				fuel.shrink(1);
				this.fuelRemainder = value;
				this.setChanged();
			}
		}

		int room = capacity() - this.stored;

		if (this.fuelRemainder > 0 && room > 0) {
			int moved = Math.min(this.fuelRemainder, room);
			this.fuelRemainder -= moved;
			this.stored += moved;
			this.setChanged();
		}
	}

	/**
	 * Hands back whatever energy the station was holding, as the ingots it came from, so breaking
	 * a fuelled station costs you the rounding and nothing else.
	 */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);

		int perIngot = Math.max(1, TinManConfig.get().suit.energyPerIngot);
		int ingots = (this.stored + this.fuelRemainder) / perIngot;
		this.stored = 0;
		this.fuelRemainder = 0;

		if (ingots > 0 && this.level != null) {
			Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(),
				new ItemStack(ModItems.VOLTITE_INGOT, ingots));
		}
	}

	/**
	 * Feeds the gear slots first, then any suits worn within {@link #RANGE}.
	 *
	 * <p>The rate is per target, not a pot shared between them: everything in reach charges at
	 * full speed at the same time, and the buffer simply drains proportionally faster. Splitting
	 * one budget would have meant a full suit in the slots charging a quarter as fast as a lone
	 * battery, which is the opposite of what a charging station is for.
	 */
	private int distribute(ServerLevel level, BlockPos pos, int perTarget) {
		int offset = this.spreadCursor++;
		int spent = charge(this.gearTargets(), perTarget, this.stored, offset);
		spent += charge(this.wornTargets(level, pos), perTarget, this.stored - spent, offset);
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
	 * Gives every target its own full tick's worth, until the buffer runs out.
	 *
	 * @param perTarget energy each target is offered this tick
	 * @param available what is left in the buffer for this group
	 * @return how much was actually spent
	 */
	private static int charge(List<ItemStack> targets, int perTarget, int available, int offset) {
		targets.removeIf(stack -> !Energy.stores(stack) || Energy.get(stack) >= Energy.max());

		if (targets.isEmpty() || available <= 0) {
			return 0;
		}

		// When the buffer cannot cover everyone, whoever misses out should be a different piece
		// each tick rather than always the last one in the list.
		Collections.rotate(targets, -Math.floorMod(offset, targets.size()));

		int spent = 0;

		for (ItemStack stack : targets) {
			if (spent >= available) {
				break;
			}

			spent += Energy.charge(stack, Math.min(perTarget, available - spent));
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
		return slot == FUEL_SLOT ? fuelValue(stack) > 0 : Energy.stores(stack);
	}
}
