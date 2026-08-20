package dev.alqu.tinman.client;

import dev.alqu.tinman.TinMan;
import dev.alqu.tinman.client.hud.SuitHudElement;
import dev.alqu.tinman.client.screen.AssemblerScreen;
import dev.alqu.tinman.client.screen.ChargingStationScreen;
import dev.alqu.tinman.registry.ModEntities;
import dev.alqu.tinman.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;

public class TinManClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The bolt is drawn entirely by the particle trail the server broadcasts, so the
		// entity itself needs no model — but it still needs a renderer registered.
		EntityRendererRegistry.register(ModEntities.PULSE_BOLT, NoopRenderer::new);

		MenuScreens.register(ModMenus.ASSEMBLER, AssemblerScreen::new);
		MenuScreens.register(ModMenus.CHARGING_STATION, ChargingStationScreen::new);

		// Sits with the other status bars so it hides along with the rest of the HUD.
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR,
			TinMan.id("suit_overlay"), new SuitHudElement());
	}
}
