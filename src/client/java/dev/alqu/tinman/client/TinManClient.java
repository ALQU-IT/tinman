package dev.alqu.tinman.client;

import dev.alqu.tinman.client.screen.AssemblerScreen;
import dev.alqu.tinman.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class TinManClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.ASSEMBLER, AssemblerScreen::new);
	}
}
