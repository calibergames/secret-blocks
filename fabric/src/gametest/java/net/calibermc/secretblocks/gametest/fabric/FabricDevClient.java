package net.calibermc.secretblocks.gametest.fabric;

import net.calibermc.secretblocks.gametest.driver.DevClientDriver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Starts the development client driver on Fabric when asked for. */
public class FabricDevClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		DevClientDriver driver = DevClientDriver.start();
		if (driver != null) {
			ClientTickEvents.END_CLIENT_TICK.register(driver::tick);
		}
	}
}
