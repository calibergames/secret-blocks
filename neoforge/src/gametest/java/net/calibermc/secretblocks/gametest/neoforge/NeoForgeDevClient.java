package net.calibermc.secretblocks.gametest.neoforge;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.calibermc.secretblocks.gametest.driver.DevClientDriver;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;

/** Starts the development client driver on NeoForge when asked for (client only). */
final class NeoForgeDevClient {

	private NeoForgeDevClient() {
	}

	static void init() {
		DevClientDriver driver = DevClientDriver.start();
		if (driver != null) {
			NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> driver.tick(Minecraft.getInstance()));
		}
	}
}
