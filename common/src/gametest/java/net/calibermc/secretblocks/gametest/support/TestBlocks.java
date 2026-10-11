package net.calibermc.secretblocks.gametest.support;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Blocks the game tests need that the mod does not have; each loader's test mod registers them. */
public final class TestBlocks {

	public static final ResourceLocation TEST_GATE_OWNER_ID = new ResourceLocation("secretblocks_gametest", "test_gate_owner");

	public static Block TEST_GATE_OWNER;

	private TestBlocks() {
	}

	/** Makes the test gate owner; the loader registers it under {@link #TEST_GATE_OWNER_ID}. */
	public static Block createTestGateOwner() {
		TEST_GATE_OWNER = new TestGateOwner(BlockBehaviour.Properties.copy(Blocks.STONE));
		return TEST_GATE_OWNER;
	}
}
