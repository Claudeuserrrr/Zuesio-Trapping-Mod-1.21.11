package com.example.timber;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class ZeusTrappingUtilityClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BountyCopy.register();
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(ZeusTrappingUtility.MOD_ID, "counters"),
				EntityCounterHud::render);
	}
}
