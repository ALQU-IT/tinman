package it.alqu.tinman.registry;

import it.alqu.tinman.TinMan;
import it.alqu.tinman.block.AssemblerBlockEntity;
import it.alqu.tinman.block.ChargingStationBlockEntity;
import it.alqu.tinman.block.TerminalBlockEntity;
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

	public static final BlockEntityType<ChargingStationBlockEntity> CHARGING_STATION = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		TinMan.id("charging_station"),
		new BlockEntityType<>(ChargingStationBlockEntity::new, Set.of(ModBlocks.CHARGING_STATION))
	);

	public static final BlockEntityType<TerminalBlockEntity> STORAGE_TERMINAL = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		TinMan.id("storage_terminal"),
		new BlockEntityType<>(TerminalBlockEntity::new, Set.of(ModBlocks.STORAGE_TERMINAL))
	);

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
