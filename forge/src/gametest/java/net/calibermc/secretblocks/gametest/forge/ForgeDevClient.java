package net.calibermc.secretblocks.gametest.forge;

import net.calibermc.secretblocks.gametest.driver.DevClientDriver;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;

/** Starts the development client driver on Forge when asked for (client only). */
final class ForgeDevClient {

	private ForgeDevClient() {
	}

	static void init() {
		DevClientDriver driver = DevClientDriver.start();
		if (driver != null) {
			TickEvent.ClientTickEvent.Post.BUS.addListener(event -> driver.tick(Minecraft.getInstance()));
		}
	}
}
