package it.alqu.tinman.block;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.item.Energy;
import it.alqu.tinman.recipe.AssemblingRecipe;
import it.alqu.tinman.recipe.ModRecipes;
import it.alqu.tinman.registry.ModBlockEntities;
import it.alqu.tinman.registry.ModItems;
import it.alqu.tinman.menu.AssemblerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Backing inventory and crafting logic for the Assembler.
 *
 * <p>Slot layout: 0-8 are the 3x3 grid, 9 is the power cell (Voltite Ingots only), 10 is the
 * output. Crafting runs over time like a furnace rather than on-click, which gives the block
 * something to animate and makes it automatable with hoppers.
 *
 * <p>All recipe matching and item consumption happens here, on the server. The menu never decides
 * what may be crafted; it only mirrors slots and the progress bar to the client.
 */
public class AssemblerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int GRID_SIZE = 9;
	public static final int POWER_SLOT = 9;
	public static final int OUTPUT_SLOT = 10;
	public static final int CONTAINER_SIZE = 11;

	/** Ticks a single assembly takes. */
	public static final int ASSEMBLY_TIME = 60;

	public static final int DATA_PROGRESS = 0;
	public static final int DATA_TOTAL = 1;
	public static final int DATA_COUNT = 2;

	private static final int[] SLOTS_TOP = {0, 1, 2, 3, 4, 5, 6, 7, 8};
	private static final int[] SLOTS_SIDE = {POWER_SLOT};
	private static final int[] SLOTS_BOTTOM = {OUTPUT_SLOT};

	private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
	private int progress;

	private final RecipeManager.CachedCheck<CraftingInput, AssemblingRecipe> quickCheck =
		RecipeManager.createCheck(ModRecipes.ASSEMBLING);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case DATA_PROGRESS -> AssemblerBlockEntity.this.progress;
				case DATA_TOTAL -> ASSEMBLY_TIME;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			if (index == DATA_PROGRESS) {
				AssemblerBlockEntity.this.progress = value;
			}
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public AssemblerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ASSEMBLER, pos, state);
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
		return Component.translatable("container.tinman.assembler");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new AssemblerMenu(containerId, inventory, this, this.data);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
		this.progress = input.getIntOr("AssemblyProgress", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		output.putInt("AssemblyProgress", this.progress);
	}

	/** The 3x3 grid as a recipe input. */
	private CraftingInput gridInput() {
		return CraftingInput.of(3, 3, List.copyOf(this.items.subList(0, GRID_SIZE)));
	}

	private boolean hasPower(int cost) {
		ItemStack cell = this.items.get(POWER_SLOT);
		return cell.is(ModItems.VOLTITE_INGOT) && cell.getCount() >= cost;
	}

	private boolean canOutput(ItemStack result) {
		if (result.isEmpty()) {
			return false;
		}

		ItemStack out = this.items.get(OUTPUT_SLOT);

		if (out.isEmpty()) {
			return true;
		}

		if (!ItemStack.isSameItemSameComponents(out, result)) {
			return false;
		}

		return out.getCount() + result.getCount() <= out.getMaxStackSize();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, AssemblerBlockEntity entity) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		RecipeHolder<AssemblingRecipe> holder = entity.quickCheck.getRecipeFor(entity.gridInput(), serverLevel).orElse(null);
		boolean wasCrafting = state.getValue(AssemblerBlock.CRAFTING);
		boolean crafting = false;

		if (holder != null) {
			AssemblingRecipe recipe = holder.value();
			ItemStack result = recipe.assemble(entity.gridInput());

			if (entity.hasPower(recipe.powerCost()) && entity.canOutput(result)) {
				crafting = true;
				entity.progress++;

				if (entity.progress >= ASSEMBLY_TIME) {
					entity.progress = 0;
					entity.craft(recipe, result);
				}
			}
		} else if (entity.canRecharge()) {
			crafting = true;
			entity.progress++;

			if (entity.progress >= ASSEMBLY_TIME) {
				entity.progress = 0;
				entity.recharge();
			}
		}

		if (!crafting && entity.progress != 0) {
			// Lose progress if the recipe or the power is taken away mid-assembly.
			entity.progress = 0;
			entity.setChanged();
		}

		if (wasCrafting != crafting) {
			level.setBlock(pos, state.setValue(AssemblerBlock.CRAFTING, crafting), Block.UPDATE_ALL);
			entity.setChanged();
		}
	}

	private void craft(AssemblingRecipe recipe, ItemStack result) {
		// One item from each occupied grid slot, mirroring a crafting table.
		for (int slot = 0; slot < GRID_SIZE; slot++) {
			ItemStack stack = this.items.get(slot);

			if (!stack.isEmpty()) {
				stack.shrink(1);
			}
		}

		this.items.get(POWER_SLOT).shrink(recipe.powerCost());

		ItemStack out = this.items.get(OUTPUT_SLOT);

		if (out.isEmpty()) {
			this.items.set(OUTPUT_SLOT, result);
		} else {
			out.grow(result.getCount());
		}

		this.setChanged();
	}

	/**
	 * A lone piece of energy gear in the grid is a recharge job rather than a recipe: each cycle
	 * burns one Voltite Ingot from the power cell into the item.
	 *
	 * @return the grid slot holding the item, or -1 if this is not a recharge
	 */
	private int rechargeSlot() {
		int found = -1;

		for (int slot = 0; slot < GRID_SIZE; slot++) {
			ItemStack stack = this.items.get(slot);

			if (stack.isEmpty()) {
				continue;
			}

			// Anything else in the grid means the player meant to craft, not to charge.
			if (found != -1 || !Energy.stores(stack) || stack.getCount() != 1) {
				return -1;
			}

			found = slot;
		}

		return found;
	}

	private boolean canRecharge() {
		int slot = this.rechargeSlot();
		return slot >= 0 && Energy.get(this.items.get(slot)) < Energy.max() && this.hasPower(1);
	}

	private void recharge() {
		int slot = this.rechargeSlot();

		if (slot < 0) {
			return;
		}

		this.items.get(POWER_SLOT).shrink(1);

		ItemStack gear = this.items.get(slot);
		Energy.charge(gear, TinManConfig.get().suit.energyPerIngot);

		// Eject to the output once it is full, so hoppers can collect finished gear.
		if (Energy.get(gear) >= Energy.max() && this.items.get(OUTPUT_SLOT).isEmpty()) {
			this.items.set(OUTPUT_SLOT, gear.copy());
			this.items.set(slot, ItemStack.EMPTY);
		}

		this.setChanged();
	}

	// --- WorldlyContainer: hoppers feed the grid from above, the cell from the sides,
	// --- and pull finished items out of the bottom.

	@Override
	public int[] getSlotsForFace(Direction side) {
		return switch (side) {
			case DOWN -> SLOTS_BOTTOM;
			case UP -> SLOTS_TOP;
			default -> SLOTS_SIDE;
		};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		if (!this.canPlaceItem(slot, stack)) {
			return false;
		}

		if (slot >= GRID_SIZE) {
			return true;
		}

		// A hopper fills the first slot that will take the item, which would pile a whole stack
		// into one grid cell and never match a multi-slot recipe. Only accept into the emptiest
		// cell so repeated inserts spread across the grid instead.
		int here = this.acceptableCount(slot, stack);

		if (here < 0) {
			return false;
		}

		for (int other = 0; other < GRID_SIZE; other++) {
			int count = this.acceptableCount(other, stack);

			if (count >= 0 && count < here) {
				return false;
			}
		}

		return true;
	}

	/**
	 * How many of {@code stack}'s item the given grid slot already holds, or -1 if that slot
	 * cannot take any more of it.
	 */
	private int acceptableCount(int slot, ItemStack stack) {
		ItemStack existing = this.items.get(slot);

		if (existing.isEmpty()) {
			return 0;
		}

		if (!ItemStack.isSameItemSameComponents(existing, stack) || existing.getCount() >= existing.getMaxStackSize()) {
			return -1;
		}

		return existing.getCount();
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == OUTPUT_SLOT) {
			return false;
		}

		if (slot == POWER_SLOT) {
			return stack.is(ModItems.VOLTITE_INGOT);
		}

		return slot < GRID_SIZE;
	}
}
