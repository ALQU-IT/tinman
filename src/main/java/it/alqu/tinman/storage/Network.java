package it.alqu.tinman.storage;

import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The set of containers a terminal can reach, and the pooled contents it presents as one inventory.
 *
 * <p>Rebuilt on demand rather than cached: chests near a terminal are opened, filled and broken by
 * hand all the time, and an index that has to be invalidated by every one of those paths is an
 * index that will eventually be wrong. Scanning is cheap enough to simply redo — the work is bounded
 * by the containers actually in range, not by the volume of the radius.
 */
public final class Network {
	private Network() {
	}

	/**
	 * Containers within range of the terminal, nearest first.
	 *
	 * <p>Found by walking the block entities the overlapping chunks already hold rather than by
	 * probing every block position: a 15-block radius is nearly thirty thousand positions and only
	 * a handful of them are ever a chest. Chunks that are not loaded are skipped rather than
	 * loaded, so a terminal cannot reach into terrain nobody is keeping alive.
	 */
	public static List<Container> containers(ServerLevel level, BlockPos origin) {
		double radius = Math.max(0.0, TinManConfig.get().storage.terminalRadius);
		double radiusSq = radius * radius;
		List<Found> found = new ArrayList<>();

		int minX = SectionPos.blockToSectionCoord(origin.getX() - radius);
		int maxX = SectionPos.blockToSectionCoord(origin.getX() + radius);
		int minZ = SectionPos.blockToSectionCoord(origin.getZ() - radius);
		int maxZ = SectionPos.blockToSectionCoord(origin.getZ() + radius);

		for (int chunkX = minX; chunkX <= maxX; chunkX++) {
			for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);

				if (chunk == null) {
					continue;
				}

				chunk.getBlockEntities().forEach((pos, blockEntity) -> {
					if (blockEntity instanceof Container container
						&& pos.distSqr(origin) <= radiusSq
						&& level.getBlockState(pos).is(ModTags.Blocks.TERMINAL_SOURCES)) {
						found.add(new Found(pos.immutable(), container));
					}
				});
			}
		}

		// A chunk hands its block entities over in hash order, which is arbitrary and can differ
		// between two scans of the same unchanged room. Left alone that would reshuffle the grid
		// under the player's cursor on every refresh, so the order is pinned here: nearest first,
		// then by position, which also means withdrawals drain the closest chest first.
		found.sort(Comparator.<Found>comparingDouble(entry -> entry.pos.distSqr(origin))
			.thenComparingInt(entry -> entry.pos.getY())
			.thenComparingInt(entry -> entry.pos.getX())
			.thenComparingInt(entry -> entry.pos.getZ()));

		return found.stream().map(Found::container).toList();
	}

	private record Found(BlockPos pos, Container container) {
	}

	/**
	 * Every stack in reach, pooled by item so the terminal shows one entry per kind rather than
	 * one per chest slot.
	 *
	 * <p>Insertion order is kept, so an item stays put in the grid between refreshes instead of
	 * jumping around as counts change.
	 */
	public static List<Entry> pool(List<Container> containers) {
		List<Entry> entries = new ArrayList<>();

		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);

				if (stack.isEmpty()) {
					continue;
				}

				Entry match = null;

				for (Entry entry : entries) {
					if (ItemStack.isSameItemSameComponents(entry.sample, stack)) {
						match = entry;
						break;
					}
				}

				if (match == null) {
					match = new Entry(stack.copyWithCount(1));
					entries.add(match);
				}

				match.total += stack.getCount();
				match.sources.add(new Source(container, slot));
			}
		}

		return entries;
	}

	/** One kind of item, and every place in reach that some of it is sitting. */
	public static final class Entry {
		private final ItemStack sample;
		private int total;
		private final List<Source> sources = new ArrayList<>();

		private Entry(ItemStack sample) {
			this.sample = sample;
		}

		/** A single item of this kind, for display. */
		public ItemStack sample() {
			return this.sample;
		}

		/** How many there are across every container in reach. */
		public int total() {
			return this.total;
		}

		/**
		 * Pulls up to {@code wanted} of this item out of the chests holding it.
		 *
		 * @return what was actually taken, which may be less if the chests changed underneath
		 */
		public ItemStack take(int wanted) {
			ItemStack taken = this.sample.copyWithCount(0);
			int remaining = Math.min(wanted, this.total);

			for (Source source : this.sources) {
				if (remaining <= 0) {
					break;
				}

				ItemStack held = source.container.getItem(source.slot);

				// Re-checked rather than trusted: the scan that built this entry may be a few
				// ticks old, and a hopper does not wait for the terminal.
				if (!ItemStack.isSameItemSameComponents(held, this.sample)) {
					continue;
				}

				int moved = Math.min(remaining, held.getCount());
				source.container.setItem(source.slot, held.copyWithCount(held.getCount() - moved));
				source.container.setChanged();
				taken.grow(moved);
				remaining -= moved;
			}

			return taken;
		}
	}

	private record Source(Container container, int slot) {
	}
}
