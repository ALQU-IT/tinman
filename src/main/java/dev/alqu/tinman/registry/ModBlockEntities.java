package dev.alqu.tinman.registry;

import dev.alqu.tinman.TinMan;
import dev.alqu.tinman.block.AssemblerBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModBlockEntities {
	private ModBlockEntities() {
	}

	public static final BlockEntityType<AssemblerBlockEntity> ASSEMBLER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		TinMan.id("assembler"),
		new BlockEntityType<>(AssemblerBlockEntity::new, Set.of(ModBlocks.ASSEMBLER))
	);

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
