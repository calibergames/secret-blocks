package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Placement cases that stay the same through the placement rework: copying
 * the clicked block, P1, P3, P4, P7, P8.
 *
 * Standard layout: stone floor at y = 0, the clicked block at (3, 1, 5), the new
 * block lands at (3, 1, 4), the player stands at (3.5, 1, 1.5) north of it.
 */
public class PlacementGameTests implements FabricGameTest {

	private static final BlockPos TARGET = new BlockPos(3, 1, 5);
	private static final BlockPos PLACED = new BlockPos(3, 1, 4);
	private static final Vec3 FEET = new Vec3(3.5, 1, 1.5);
	private static final Vec3 NORTH_FACE_CENTER = new Vec3(3.5, 1.5, 5.0);

	/** Place {@code block} against bricks; every face copies the bricks. */
	private static void againstBricks(GameTestHelper h, Block block) {
		floor(h);
		h.setBlock(TARGET, Blocks.BRICKS);
		place(h, block.asItem(), FEET, TARGET, Direction.NORTH, NORTH_FACE_CENTER);
		checkBlock(h, PLACED, block);
		if (block == ModBlocks.ONE_WAY_GLASS) {
			// P4: the face toward the placer is glass.
			checkFaces(h, PLACED, Blocks.BRICKS, only(Direction.NORTH, Blocks.GLASS));
		} else {
			checkFaces(h, PLACED, Blocks.BRICKS);
		}
		if (block == ModBlocks.DOOR_BLOCK || block == ModBlocks.IRON_DOOR_BLOCK) {
			// P8: the upper half is disguised too.
			checkBlock(h, PLACED.above(), block);
			checkFaces(h, PLACED.above(), Blocks.BRICKS);
		}
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void dummyBlockCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.SOLID_BLOCK); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void stairsCopyClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.STAIR_BLOCK); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void slabCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.SLAB_BLOCK); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void ghostBlockCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.GHOST_BLOCK); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void oneWayGlassFacesThePlacer(GameTestHelper h) { againstBricks(h, ModBlocks.ONE_WAY_GLASS); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void doorDisguisesBothHalves(GameTestHelper h) { againstBricks(h, ModBlocks.DOOR_BLOCK); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironDoorDisguisesBothHalves(GameTestHelper h) { againstBricks(h, ModBlocks.IRON_DOOR_BLOCK); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void trapdoorCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.TRAPDOOR_BLOCK); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironTrapdoorCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.IRON_TRAPDOOR_BLOCK); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void woodenButtonCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.WOODEN_BUTTON); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void stoneButtonCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.STONE_BUTTON); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void leverCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.SECRET_LEVER); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void secretRedstoneCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.SECRET_REDSTONE); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void secretChestCopiesClickedBlock(GameTestHelper h) { againstBricks(h, ModBlocks.SECRET_CHEST); }

	/**
	 * Breaking either half of a placed door removes both halves and both
	 * disguise block entities.
	 */
	private static void doorBrokenBy(GameTestHelper h, Block door, boolean upper) {
		floor(h);
		h.setBlock(TARGET, Blocks.BRICKS);
		place(h, door.asItem(), FEET, TARGET, Direction.NORTH, NORTH_FACE_CENTER);
		checkBlock(h, PLACED.above(), door);
		h.getLevel().destroyBlock(h.absolutePos(upper ? PLACED.above() : PLACED), false);
		for (BlockPos half : new BlockPos[] {PLACED, PLACED.above()}) {
			checkBlock(h, half, Blocks.AIR);
			check(h, h.getBlockEntity(half) == null, "a block entity was left behind at " + half);
		}
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void doorBrokenAtTheBottomLeavesNoBlockEntity(GameTestHelper h) { doorBrokenBy(h, ModBlocks.DOOR_BLOCK, false); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void doorBrokenAtTheTopLeavesNoBlockEntity(GameTestHelper h) { doorBrokenBy(h, ModBlocks.DOOR_BLOCK, true); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironDoorBrokenAtTheTopLeavesNoBlockEntity(GameTestHelper h) { doorBrokenBy(h, ModBlocks.IRON_DOOR_BLOCK, true); }

	/** P1: a single block placed under a disguised secret block leaves the block above alone. */
	private static void underDisguisedBlock(GameTestHelper h, Block block) {
		floor(h);
		h.setBlock(TARGET, Blocks.BRICKS);
		BlockPos above = PLACED.above();
		h.setBlock(above, ModBlocks.SOLID_BLOCK);
		secret(h, above).setState(Blocks.GOLD_BLOCK.defaultBlockState());
		place(h, block.asItem(), FEET, TARGET, Direction.NORTH, new Vec3(3.5, 1.3, 5.0));
		checkBlock(h, PLACED, block);
		checkFaces(h, PLACED, Blocks.BRICKS);
		checkFaces(h, above, Blocks.GOLD_BLOCK);
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void leverLeavesBlockAboveAlone(GameTestHelper h) { underDisguisedBlock(h, ModBlocks.SECRET_LEVER); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void chestLeavesBlockAboveAlone(GameTestHelper h) { underDisguisedBlock(h, ModBlocks.SECRET_CHEST); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void secretRedstoneLeavesBlockAboveAlone(GameTestHelper h) { underDisguisedBlock(h, ModBlocks.SECRET_REDSTONE); }

	/** P3: a block set without a player has no disguise (air on every face, drawn as stone). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void setBlockLeavesNoDisguise(GameTestHelper h) {
		h.setBlock(PLACED, ModBlocks.SOLID_BLOCK);
		checkFaces(h, PLACED, Blocks.AIR);
		for (Direction dir : Direction.values()) {
			check(h, secret(h, PLACED).getRotation(dir) == 0, dir + " rotation should be 0");
			check(h, secret(h, PLACED).getDir(dir) == dir, dir + " should draw its own side");
		}
		h.succeed();
	}

	/** P4: One Way Glass placed from the west puts the glass on the west face. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void oneWayGlassFromTheWest(GameTestHelper h) {
		floor(h);
		BlockPos target = new BlockPos(5, 1, 3);
		BlockPos placed = new BlockPos(4, 1, 3);
		h.setBlock(target, Blocks.BRICKS);
		place(h, ModBlocks.ONE_WAY_GLASS.asItem(), new Vec3(1.5, 1, 3.5), target, Direction.WEST, new Vec3(5.0, 1.5, 3.5));
		checkFaces(h, placed, Blocks.BRICKS, only(Direction.WEST, Blocks.GLASS));
		h.succeed();
	}

	/**
	 * One Way Glass placed on the floor at (3, 1, 3), the player standing at
	 * {@code feet} and looking at the top of the floor block there: steeply
	 * down from (3.5, 1, 2.5), mostly across from (3.5, 1, 0.5). Sneaking puts
	 * the glass on the face toward the player along their look.
	 */
	private static void oneWayGlassOnTheFloor(GameTestHelper h, Vec3 feet, boolean sneaking, Direction glass) {
		floor(h);
		h.setBlock(new BlockPos(3, 0, 3), Blocks.BRICKS);
		Vec3 hit = new Vec3(3.5, 1.0, 3.5);
		ServerPlayer player = player(h, feet, hit);
		player.setShiftKeyDown(sneaking);
		useOn(h, player, new ItemStack(ModBlocks.ONE_WAY_GLASS), new BlockPos(3, 0, 3), Direction.UP, hit);
		checkFaces(h, new BlockPos(3, 1, 3), Blocks.BRICKS, only(glass, Blocks.GLASS));
		h.succeed();
	}

	/** Sneaking and looking down at the floor: glass on top. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void oneWayGlassSneakingLookingDownGoesOnTop(GameTestHelper h) {
		oneWayGlassOnTheFloor(h, new Vec3(3.5, 1, 2.5), true, Direction.UP);
	}

	/** The same look without sneaking keeps the glass on the side facing the player (north of the block). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void oneWayGlassLookingDownWithoutSneakingGoesOnTheSide(GameTestHelper h) {
		oneWayGlassOnTheFloor(h, new Vec3(3.5, 1, 2.5), false, Direction.NORTH);
	}

	/** Sneaking but looking more across than down (a far floor block): the side facing the player. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void oneWayGlassSneakingLookingAcrossGoesOnTheSide(GameTestHelper h) {
		oneWayGlassOnTheFloor(h, new Vec3(3.5, 1, 0.5), true, Direction.NORTH);
	}

	/** Sneaking and looking up at a ceiling: glass on the bottom. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void oneWayGlassSneakingLookingUpGoesOnTheBottom(GameTestHelper h) {
		floor(h);
		BlockPos ceiling = new BlockPos(3, 4, 3);
		h.setBlock(ceiling, Blocks.BRICKS);
		Vec3 hit = new Vec3(3.5, 4.0, 3.5);
		ServerPlayer player = player(h, new Vec3(3.5, 1, 2.5), hit);
		player.setShiftKeyDown(true);
		useOn(h, player, new ItemStack(ModBlocks.ONE_WAY_GLASS), ceiling, Direction.DOWN, hit);
		checkFaces(h, ceiling.below(), Blocks.BRICKS, only(Direction.DOWN, Blocks.GLASS));
		h.succeed();
	}

	/** P7: clicking a block that is not a full cube gives stone; full cubes are copied. */
	private static void againstNeighbor(GameTestHelper h, net.minecraft.world.level.block.state.BlockState neighbor,
			Vec3 hit, Block expected) {
		floor(h);
		h.setBlock(TARGET, neighbor);
		place(h, ModBlocks.SOLID_BLOCK.asItem(), FEET, TARGET, Direction.NORTH, hit);
		checkBlock(h, PLACED, ModBlocks.SOLID_BLOCK);
		checkFaces(h, PLACED, expected);
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bottomSlabGivesStone(GameTestHelper h) {
		againstNeighbor(h, Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
				new Vec3(3.5, 1.25, 5.0), Blocks.STONE);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void fenceGivesStone(GameTestHelper h) {
		againstNeighbor(h, Blocks.OAK_FENCE.defaultBlockState(), new Vec3(3.5, 1.5, 5.375), Blocks.STONE);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void glassPaneGivesStone(GameTestHelper h) {
		againstNeighbor(h, Blocks.GLASS_PANE.defaultBlockState(), new Vec3(3.5, 1.5, 5.4375), Blocks.STONE);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void glassIsCopied(GameTestHelper h) {
		againstNeighbor(h, Blocks.GLASS.defaultBlockState(), NORTH_FACE_CENTER, Blocks.GLASS);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void leavesAreCopied(GameTestHelper h) {
		againstNeighbor(h, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
				NORTH_FACE_CENTER, Blocks.OAK_LEAVES);
	}
}
