package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Which secret blocks pass redstone power through themselves (conductors):
 * a block standing in for a redstone component does exactly as that vanilla
 * block does; the Gate and its segments do not; the plain building blocks
 * do, as stone.
 */
public class ConductorGameTests {

	private static final Map<Block, Block> TWINS = new LinkedHashMap<>();

	static {
		TWINS.put(ModBlocks.WOODEN_BUTTON, Blocks.OAK_BUTTON);
		TWINS.put(ModBlocks.STONE_BUTTON, Blocks.STONE_BUTTON);
		TWINS.put(ModBlocks.SECRET_LEVER, Blocks.LEVER);
		TWINS.put(ModBlocks.SECRET_OBSERVER, Blocks.OBSERVER);
		TWINS.put(ModBlocks.SECRET_HOPPER, Blocks.HOPPER);
		TWINS.put(ModBlocks.SECRET_CHEST, Blocks.CHEST);
		TWINS.put(ModBlocks.SECRET_TRAPPED_CHEST, Blocks.TRAPPED_CHEST);
		TWINS.put(ModBlocks.SECRET_DISPENSER, Blocks.DISPENSER);
		TWINS.put(ModBlocks.SECRET_DROPPER, Blocks.DROPPER);
		TWINS.put(ModBlocks.SECRET_SCULK_SENSOR, Blocks.SCULK_SENSOR);
		TWINS.put(ModBlocks.SECRET_REPEATER, Blocks.REPEATER);
		TWINS.put(ModBlocks.SECRET_COMPARATOR, Blocks.COMPARATOR);
		TWINS.put(ModBlocks.SECRET_DAYLIGHT_DETECTOR, Blocks.DAYLIGHT_DETECTOR);
		TWINS.put(ModBlocks.SECRET_REDSTONE, Blocks.REDSTONE_WIRE);
		TWINS.put(ModBlocks.SECRET_REDSTONE_WIRE, Blocks.REDSTONE_WIRE);
		TWINS.put(ModBlocks.SECRET_PISTON, Blocks.PISTON);
		TWINS.put(ModBlocks.SECRET_STICKY_PISTON, Blocks.STICKY_PISTON);
		TWINS.put(ModBlocks.PRESSURE_PLATE, Blocks.OAK_PRESSURE_PLATE);
		TWINS.put(ModBlocks.PLAYER_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE);
		TWINS.put(ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
		TWINS.put(ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
	}

	private static boolean conducts(GameTestHelper h, Block block) {
		BlockPos at = new BlockPos(1, 2, 1);
		h.setBlock(at, block);
		boolean conducts = h.getBlockState(at).isRedstoneConductor(h.getLevel(), h.absolutePos(at));
		h.setBlock(at, Blocks.AIR);
		return conducts;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void conductorsMatchTheirVanillaTwins(GameTestHelper h) {
		floor(h);
		StringBuilder wrong = new StringBuilder();
		TWINS.forEach((secret, vanilla) -> {
			boolean expected = conducts(h, vanilla);
			if (conducts(h, secret) != expected) {
				wrong.append(BuiltInRegistries.BLOCK.getKey(secret)).append(" should ").append(expected ? "" : "not ").append("conduct, as ")
						.append(BuiltInRegistries.BLOCK.getKey(vanilla)).append("; ");
			}
		});
		for (Block block : new Block[] {ModBlocks.SECRET_GATE, ModBlocks.SECRET_GATE_SEGMENT}) {
			if (conducts(h, block)) {
				wrong.append(BuiltInRegistries.BLOCK.getKey(block)).append(" should not conduct; ");
			}
		}
		for (Block block : new Block[] {ModBlocks.SOLID_BLOCK, Blocks.STONE}) {
			if (!conducts(h, block)) {
				wrong.append(BuiltInRegistries.BLOCK.getKey(block)).append(" should conduct, as stone; ");
			}
		}
		check(h, wrong.length() == 0, wrong.toString());
		h.succeed();
	}

	/** A repeater feeding a Secret Button, Lever or Chest does not light a lamp on its far side; through a Dummy Block it does. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aRepeaterDoesNotPowerThrough(GameTestHelper h) {
		floor(h);
		Block[] blocks = {ModBlocks.WOODEN_BUTTON, ModBlocks.SECRET_LEVER, ModBlocks.SECRET_CHEST, ModBlocks.SOLID_BLOCK};
		for (int i = 0; i < blocks.length; i++) {
			BlockPos block = new BlockPos(3, 1, 1 + i * 2);
			h.setBlock(block, blocks[i]);
			h.setBlock(block.west(), Blocks.REPEATER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
			h.setBlock(block.east(), Blocks.REDSTONE_LAMP);
			// Last: a repeater set by command notices its input only when a neighbour changes.
			h.setBlock(block.west(2), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(6, () -> {
			for (int i = 0; i < blocks.length; i++) {
				boolean lit = h.getBlockState(new BlockPos(4, 1, 1 + i * 2)).getValue(RedstoneLampBlock.LIT);
				boolean dummy = blocks[i] == ModBlocks.SOLID_BLOCK;
				BlockPos block = new BlockPos(3, 1, 1 + i * 2);
				check(h, h.getBlockState(block.west()).getValue(BlockStateProperties.POWERED), "control: the repeater should be powered");
				check(h, lit == dummy, BuiltInRegistries.BLOCK.getKey(blocks[i]) + (dummy ? " should pass the power to the lamp" : " must not pass the power to the lamp")
						+ " (repeater " + h.getBlockState(block.west()) + ", signal into the lamp " + h.getLevel().getSignal(h.absolutePos(block), Direction.EAST) + ")");
			}
			h.succeed();
		});
	}
}
