package net.calibermc.secretblocks.gametest.fabric;

import net.calibermc.secretblocks.gametest.support.TestBlocks;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** The Fabric test mod's start: registers the blocks only the game tests use. */
public class FabricTestMod implements ModInitializer {

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.BLOCK, TestBlocks.TEST_GATE_OWNER_ID, TestBlocks.createTestGateOwner());
	}
}
