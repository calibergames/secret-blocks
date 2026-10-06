package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Placement cases changed on purpose from 1.2.0: P6, P2, P10, E1 and P3b.
 * Seven of the eight fail on the 1.2.0 code; the snow layer gives the same
 * result under both rules and is kept to pin it.
 */
public class ApprovedPlacementGameTests implements FabricGameTest {

	private static final BlockPos TARGET = new BlockPos(3, 1, 5);
	private static final BlockPos PLACED = new BlockPos(3, 1, 4);
	private static final Vec3 FEET = new Vec3(3.5, 1, 1.5);
	private static final Vec3 NORTH_FACE_CENTER = new Vec3(3.5, 1.5, 5.0);

	/** A Dummy Block at TARGET with a different disguise on every face (like the reference world's B0). */
	private static void sixFacedDummy(GameTestHelper h) {
		h.setBlock(TARGET, ModBlocks.SOLID_BLOCK);
		Map<Direction, net.minecraft.world.level.block.state.BlockState> faces = new EnumMap<>(Direction.class);
		faces.put(Direction.UP, Blocks.GOLD_BLOCK.defaultBlockState());
		faces.put(Direction.DOWN, Blocks.DIAMOND_BLOCK.defaultBlockState());
		faces.put(Direction.NORTH, Blocks.EMERALD_BLOCK.defaultBlockState());
		faces.put(Direction.SOUTH, Blocks.LAPIS_BLOCK.defaultBlockState());
		faces.put(Direction.WEST, Blocks.AMETHYST_BLOCK.defaultBlockState());
		faces.put(Direction.EAST, Blocks.COPPER_BLOCK.defaultBlockState());
		secret(h, TARGET).setDisguise(faces);
	}

	/** P6, different type: copies the clicked face (1.2.0 copied the down face, diamond). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void differentSecretTypeCopiesClickedFace(GameTestHelper h) {
		floor(h);
		sixFacedDummy(h);
		place(h, ModBlocks.GHOST_BLOCK.asItem(), FEET, TARGET, Direction.NORTH, NORTH_FACE_CENTER);
		checkFaces(h, PLACED, Blocks.EMERALD_BLOCK);
		h.succeed();
	}

	/** P6, same type: copies the clicked face (1.2.0 looked through it and got stone). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void sameSecretTypeCopiesClickedFace(GameTestHelper h) {
		floor(h);
		sixFacedDummy(h);
		place(h, ModBlocks.SOLID_BLOCK.asItem(), FEET, TARGET, Direction.NORTH, NORTH_FACE_CENTER);
		checkFaces(h, PLACED, Blocks.EMERALD_BLOCK);
		h.succeed();
	}

	/** P2: clicking tall grass copies the block it stood on (1.2.0 copied the bricks behind it). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void tallGrassCopiesTheGroundBelow(GameTestHelper h) {
		floor(h);
		h.setBlock(PLACED.below(), Blocks.GRASS_BLOCK);
		h.setBlock(PLACED, Blocks.GRASS);
		h.setBlock(TARGET, Blocks.BRICKS);
		place(h, ModBlocks.SOLID_BLOCK.asItem(), FEET, PLACED, Direction.NORTH, new Vec3(3.5, 1.3, 4.5));
		checkBlock(h, PLACED, ModBlocks.SOLID_BLOCK);
		checkFaces(h, PLACED, Blocks.GRASS_BLOCK);
		h.succeed();
	}

	/** P2: clicking a snow layer copies the block it lay on. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void snowLayerCopiesTheGroundBelow(GameTestHelper h) {
		floor(h);
		h.setBlock(PLACED.below(), Blocks.GRASS_BLOCK);
		h.setBlock(PLACED, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1));
		place(h, ModBlocks.SOLID_BLOCK.asItem(), FEET, PLACED, Direction.UP, new Vec3(3.5, 1.125, 4.5));
		checkBlock(h, PLACED, ModBlocks.SOLID_BLOCK);
		checkFaces(h, PLACED, Blocks.GRASS_BLOCK);
		h.succeed();
	}

	/** P10: stacking a second slab keeps the first slab's disguise (1.2.0 re-picked it: stone here). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void stackedSlabKeepsItsDisguise(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.BRICKS);
		place(h, ModBlocks.SLAB_BLOCK.asItem(), FEET, TARGET, Direction.NORTH, new Vec3(3.5, 1.25, 5.0));
		check(h, h.getBlockState(PLACED).getValue(SlabBlock.TYPE) == SlabType.BOTTOM, "first slab should be a bottom slab");
		checkFaces(h, PLACED, Blocks.BRICKS);
		h.setBlock(TARGET, Blocks.AIR); // open space behind, where 1.2.0 would re-pick stone
		place(h, ModBlocks.SLAB_BLOCK.asItem(), FEET, PLACED, Direction.UP, new Vec3(3.5, 1.5, 4.5));
		check(h, h.getBlockState(PLACED).getValue(SlabBlock.TYPE) == SlabType.DOUBLE, "second slab should make a double slab");
		checkFaces(h, PLACED, Blocks.BRICKS);
		h.succeed();
	}

	/** E1: a click near the bottom edge of a face copies the clicked block (1.2.0 copied the block below). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void edgeClickCopiesTheClickedBlock(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.BRICKS);
		place(h, ModBlocks.SOLID_BLOCK.asItem(), FEET, TARGET, Direction.NORTH, new Vec3(3.5, 1.01, 5.0));
		checkFaces(h, PLACED, Blocks.BRICKS);
		h.succeed();
	}

	/** P3b: placement without a player works and copies the clicked block (1.2.0 would crash). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void placementWithoutPlayer(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.BRICKS);
		useOnWithoutPlayer(h, new ItemStack(ModBlocks.SOLID_BLOCK), TARGET, Direction.NORTH, NORTH_FACE_CENTER);
		checkBlock(h, PLACED, ModBlocks.SOLID_BLOCK);
		checkFaces(h, PLACED, Blocks.BRICKS);
		h.succeed();
	}

	/** P3b: One Way Glass placed without a player puts the glass on the face away from the clicked block. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void oneWayGlassWithoutPlayer(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.BRICKS);
		useOnWithoutPlayer(h, new ItemStack(ModBlocks.ONE_WAY_GLASS), TARGET, Direction.NORTH, NORTH_FACE_CENTER);
		checkFaces(h, PLACED, Blocks.BRICKS, only(Direction.NORTH, Blocks.GLASS));
		h.succeed();
	}
}
