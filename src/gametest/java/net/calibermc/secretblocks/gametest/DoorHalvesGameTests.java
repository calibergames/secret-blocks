package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.render.DoorHalves;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * When the Builder's Goggles' outline draws a door's two halves as one
 * panel: only a lower half with its own upper half above it. The outline
 * itself is drawn by client code, checked in the client.
 */
public class DoorHalvesGameTests implements FabricGameTest {

	private static boolean above(GameTestHelper h, BlockPos rel) {
		BlockPos abs = h.absolutePos(rel);
		return DoorHalves.joinsAbove(h.getLevel(), abs, h.getLevel().getBlockState(abs));
	}

	private static boolean below(GameTestHelper h, BlockPos rel) {
		BlockPos abs = h.absolutePos(rel);
		return DoorHalves.joinsBelow(h.getLevel(), abs, h.getLevel().getBlockState(abs));
	}

	/** Set a block without shape updates, so a door half with no partner stays (vanilla would remove it). */
	private static void setQuietly(GameTestHelper h, BlockPos rel, BlockState state) {
		h.getLevel().setBlock(h.absolutePos(rel), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
	}

	private static BlockState lower(Block door) {
		return door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
	}

	/** Secret doors, wooden and iron, closed and open: the lower half joins above, the upper half below. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aDoorsHalvesJoin(GameTestHelper h) {
		int x = 0;
		for (Block door : List.of(ModBlocks.DOOR_BLOCK, ModBlocks.IRON_DOOR_BLOCK)) {
			for (boolean open : new boolean[] {false, true}) {
				BlockPos rel = new BlockPos(x, 2, 1);
				BlockState lower = lower(door).setValue(DoorBlock.OPEN, open);
				h.setBlock(rel, lower);
				h.setBlock(rel.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
				String what = door + " open=" + open;
				check(h, above(h, rel) && !below(h, rel), what + ": the lower half should join above only");
				check(h, below(h, rel.above()) && !above(h, rel.above()), what + ": the upper half should join below only");
				x += 2;
			}
		}
		h.succeed();
	}

	/**
	 * Nothing joins without its own other half: a lone lower half, a lone
	 * upper half, a lower half under another lower half, a secret door's
	 * lower half under an iron door's upper half, and blocks that are not
	 * doors.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void nothingElseJoins(GameTestHelper h) {
		BlockState lower = lower(ModBlocks.DOOR_BLOCK);
		BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
		setQuietly(h, new BlockPos(0, 2, 1), lower);
		check(h, !above(h, new BlockPos(0, 2, 1)) && !below(h, new BlockPos(0, 2, 1)), "a lone lower half joins nothing");
		setQuietly(h, new BlockPos(2, 2, 1), upper);
		check(h, !above(h, new BlockPos(2, 2, 1)) && !below(h, new BlockPos(2, 2, 1)), "a lone upper half joins nothing");
		setQuietly(h, new BlockPos(4, 2, 1), lower);
		setQuietly(h, new BlockPos(4, 3, 1), lower);
		check(h, !above(h, new BlockPos(4, 2, 1)) && !below(h, new BlockPos(4, 3, 1)), "a lower half under a lower half joins nothing");
		setQuietly(h, new BlockPos(6, 2, 1), lower);
		setQuietly(h, new BlockPos(6, 3, 1), lower(ModBlocks.IRON_DOOR_BLOCK).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		check(h, !above(h, new BlockPos(6, 2, 1)) && !below(h, new BlockPos(6, 3, 1)), "halves of two different doors join nothing");
		h.setBlock(new BlockPos(8, 2, 1), ModBlocks.SOLID_BLOCK);
		h.setBlock(new BlockPos(8, 3, 1), ModBlocks.SOLID_BLOCK);
		h.setBlock(new BlockPos(10, 2, 1), ModBlocks.TRAPDOOR_BLOCK);
		check(h, !above(h, new BlockPos(8, 2, 1)) && !below(h, new BlockPos(8, 3, 1)) && !above(h, new BlockPos(10, 2, 1)),
				"blocks that are not doors join nothing");
		h.succeed();
	}
}
