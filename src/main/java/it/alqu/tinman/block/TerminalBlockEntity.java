package it.alqu.tinman.block;

import it.alqu.tinman.menu.TerminalMenu;
import it.alqu.tinman.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Storage Terminal's block entity.
 *
 * <p>Holds nothing. It stores no items of its own and keeps no index of the chests around it — the
 * menu rebuilds that whenever it needs it. All this exists for is to give the block a menu and a
 * position to measure the radius from.
 */
public class TerminalBlockEntity extends BlockEntity implements MenuProvider {
	public TerminalBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STORAGE_TERMINAL, pos, state);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.tinman.storage_terminal");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new TerminalMenu(containerId, inventory, this);
	}
}
