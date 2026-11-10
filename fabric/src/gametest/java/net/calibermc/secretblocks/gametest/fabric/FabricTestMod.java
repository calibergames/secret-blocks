package net.calibermc.secretblocks.gametest.fabric;

import net.calibermc.secretblocks.gametest.support.SharedGameTests;
import net.calibermc.secretblocks.gametest.support.TestBlocks;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The Fabric test mod's start: registers the blocks only the game tests use, and the
 * shared game tests' functions and environment type ({@link SharedGameTests}).
 */
public class FabricTestMod implements ModInitializer {

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.BLOCK, TestBlocks.TEST_GATE_OWNER_ID, TestBlocks.createTestGateOwner());
		SharedGameTests.registerFunctions((id, function) -> Registry.register(BuiltInRegistries.TEST_FUNCTION, id, function));
		Registry.register(BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE, SharedGameTests.ENVIRONMENT_TYPE,
				SharedGameTests.ENVIRONMENT_CODEC);
	}
}
