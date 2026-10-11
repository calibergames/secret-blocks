package net.calibermc.secretblocks.gametest.support;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Blocks the game tests need that the mod does not have (yet). Registered by the game-test mod only, never shipped. */
public class TestBlocks implements ModInitializer {

	public static Block TEST_GATE_OWNER;

	@Override
	public void onInitialize() {
		TEST_GATE_OWNER = Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("secretblocks-gametest", "test_gate_owner"),
				new TestGateOwner(FabricBlockSettings.copyOf(Blocks.STONE)));
	}
}
