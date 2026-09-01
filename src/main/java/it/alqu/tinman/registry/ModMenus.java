package it.alqu.tinman.registry;

import it.alqu.tinman.TinMan;
import it.alqu.tinman.menu.AssemblerMenu;
import it.alqu.tinman.menu.ChargingStationMenu;
import it.alqu.tinman.menu.TerminalMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	private ModMenus() {
	}

	public static final MenuType<AssemblerMenu> ASSEMBLER = Registry.register(
		BuiltInRegistries.MENU,
		TinMan.id("assembler"),
		new MenuType<>(AssemblerMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<ChargingStationMenu> CHARGING_STATION = Registry.register(
		BuiltInRegistries.MENU,
		TinMan.id("charging_station"),
		new MenuType<>(ChargingStationMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<TerminalMenu> STORAGE_TERMINAL = Registry.register(
		BuiltInRegistries.MENU,
		TinMan.id("storage_terminal"),
		new MenuType<>(TerminalMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static void init() {
		// Class-load the holder so the static initialisers above run.
	}
}
