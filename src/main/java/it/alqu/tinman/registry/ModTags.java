package it.alqu.tinman.registry;

import it.alqu.tinman.TinMan;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Tag keys the mod's own code reads, as opposed to the ones only the data files mention. */
public final class ModTags {
	private ModTags() {
	}

	public static final class Blocks {
		private Blocks() {
		}

		/**
		 * Containers a Storage Terminal will pool. Chests, barrels and shulker boxes by default.
		 *
		 * <p>A tag rather than a hard-coded list so a pack can add its own containers, and so the
		 * terminal does not quietly start emptying the fuel slot of every nearby furnace — every
		 * block entity with an inventory is a {@code Container}, including ones nobody thinks of
		 * as storage.
		 */
		public static final TagKey<Block> TERMINAL_SOURCES =
			TagKey.create(Registries.BLOCK, TinMan.id("terminal_sources"));
	}
}
