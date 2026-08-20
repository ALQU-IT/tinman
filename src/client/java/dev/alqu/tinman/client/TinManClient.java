package dev.alqu.tinman.client;

import dev.alqu.tinman.TinMan;
import dev.alqu.tinman.client.hud.SuitHudElement;
import dev.alqu.tinman.client.screen.AssemblerScreen;
import dev.alqu.tinman.client.screen.ChargingStationScreen;
import dev.alqu.tinman.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.screens.MenuScreens;

public class TinManClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.ASSEMBLER, AssemblerScreen::new);
		MenuScreens.register(ModMenus.CHARGING_STATION, ChargingStationScreen::new);

		// Sits with the other status bars so it hides along with the rest of the HUD.
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR,
			TinMan.id("suit_overlay"), new SuitHudElement());
	}
}
