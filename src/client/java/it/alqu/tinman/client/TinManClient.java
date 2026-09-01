package it.alqu.tinman.client;

import it.alqu.tinman.TinMan;
import it.alqu.tinman.client.hud.SuitHudElement;
import it.alqu.tinman.client.input.ModKeys;
import it.alqu.tinman.client.render.FlightLean;
import it.alqu.tinman.client.render.MobScanner;
import it.alqu.tinman.client.render.UnibeamRenderer;
import it.alqu.tinman.config.TinManConfig;
import it.alqu.tinman.network.ConfigSyncPayload;
import it.alqu.tinman.client.particle.AssemblerSparkParticle;
import it.alqu.tinman.client.particle.ThrusterFlameParticle;
import it.alqu.tinman.client.screen.AssemblerScreen;
import it.alqu.tinman.client.screen.ChargingStationScreen;
import it.alqu.tinman.client.screen.TerminalScreen;
import it.alqu.tinman.registry.ModEntities;
import it.alqu.tinman.registry.ModParticles;
import it.alqu.tinman.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;

public class TinManClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModKeys.register();

		// Adopt the server's gameplay numbers while connected, and drop them again on the way out
		// so a later single-player world uses this installation's own file.
		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE,
			(payload, context) -> context.client().execute(() -> TinManConfig.get().applyServerValues(payload)));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			TinManConfig.reloadFromDisk();
			UnibeamRenderer.clear();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null) {
				FlightLean.tick(client.level);
				UnibeamRenderer.tick();
			}
		});

		// Outlines nearby creatures in the world while the full suit is worn.
		MobScanner.register();

		// Draws each unibeam shot as a solid beam for a few frames after it lands.
		UnibeamRenderer.register();

		// The bolt is drawn entirely by the particle trail the server broadcasts, so the
		// entity itself needs no model — but it still needs a renderer registered.
		EntityRendererRegistry.register(ModEntities.PULSE_BOLT, NoopRenderer::new);

		ParticleProviderRegistry.getInstance().register(ModParticles.THRUSTER_FLAME, ThrusterFlameParticle.Provider::new);
		ParticleProviderRegistry.getInstance().register(ModParticles.ASSEMBLER_SPARK, AssemblerSparkParticle.Provider::new);

		MenuScreens.register(ModMenus.ASSEMBLER, AssemblerScreen::new);
		MenuScreens.register(ModMenus.CHARGING_STATION, ChargingStationScreen::new);
		MenuScreens.register(ModMenus.STORAGE_TERMINAL, TerminalScreen::new);

		// Sits with the other status bars so it hides along with the rest of the HUD.
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR,
			TinMan.id("suit_overlay"), new SuitHudElement());
	}
}
