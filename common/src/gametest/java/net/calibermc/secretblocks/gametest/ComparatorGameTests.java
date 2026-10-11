package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.SecretComparatorBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Comparator is a vanilla comparator in a full disguised block.
 * Reading, modes, side input, updates from containers and item frames,
 * placement, wire connections and floor removal are each compared with a
 * vanilla comparator in the same test; use, the saved data and the recipe
 * have no vanilla twin to compare with.
 */
public class ComparatorGameTests {

	private static final Block SECRET = ModBlocks.SECRET_COMPARATOR;

	/** A comparator facing WEST: it reads what is at x - 1 (its back) and outputs toward x + 1. */
	private static BlockState comparator(Block block, ComparatorMode mode) {
		return block.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST).setValue(ComparatorBlock.MODE, mode);
	}

	/** The output stored in the comparator's block entity (what vanilla's own code reads). */
	private static int output(GameTestHelper h, int x, int y, int z) {
		BlockEntity entity = h.getBlockEntity(new BlockPos(x, y, z));
		if (entity instanceof ComparatorBlockEntity comparator) {
			return comparator.getOutputSignal();
		}
		h.fail("expected a comparator block entity, found " + entity, new BlockPos(x, y, z));
		throw new AssertionError("unreachable");
	}

	/**
	 * Fill a chest with stacks of bricks and tell its comparators, as a menu or
	 * hopper would. Bricks, and emptied again before each test ends, so no other
	 * test's item counts see what a cleared chest drops.
	 */
	private static void fill(GameTestHelper h, BlockPos chest, int stacks) {
		Container container = (Container) h.getBlockEntity(chest);
		container.clearContent();
		for (int slot = 0; slot < stacks; slot++) {
			container.setItem(slot, new ItemStack(Items.BRICK, 64));
		}
		container.setChanged();
	}

	/** A chest behind it, empty, part full and full: the same output as a vanilla comparator (0, 6, 15). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void chestFillLevelsMatchVanilla(GameTestHelper h) {
		floor(h);
		// Level k on y = 1 + 2k: chest x = 1, comparator x = 2; Secret on z = 1, vanilla on z = 5 (on a stone, it needs a floor).
		int[] stacks = {0, 10, 27};
		int[] expected = {0, 6, 15};
		for (int k = 0; k < 3; k++) {
			int y = 1 + 2 * k;
			for (int z : new int[] {1, 5}) {
				h.setBlock(2, y - 1, z, Blocks.STONE);
				h.setBlock(1, y, z, Blocks.CHEST);
				h.setBlock(2, y, z, comparator(z == 1 ? SECRET : Blocks.COMPARATOR, ComparatorMode.COMPARE));
			}
		}
		// Fill after the comparators are in place: a comparator set by setBlock computes on its next update.
		for (int k = 0; k < 3; k++) {
			for (int z : new int[] {1, 5}) {
				fill(h, new BlockPos(1, 1 + 2 * k, z), stacks[k]);
			}
		}
		h.runAfterDelay(8, () -> {
			for (int k = 0; k < 3; k++) {
				int y = 1 + 2 * k;
				check(h, output(h, 2, y, 5) == expected[k], "control: a vanilla comparator behind " + stacks[k] + " stacks should read " + expected[k] + ", reads " + output(h, 2, y, 5));
				check(h, output(h, 2, y, 1) == expected[k], "the Secret Comparator behind " + stacks[k] + " stacks should read " + expected[k] + ", reads " + output(h, 2, y, 1));
				fill(h, new BlockPos(1, y, 1), 0);
				fill(h, new BlockPos(1, y, 5), 0);
			}
			h.succeed();
		});
	}

	/**
	 * Back input 15, side input 13 from a wire line: compare mode gives 15, subtract
	 * mode gives 2, for the Secret and the vanilla comparator alike. The wire piece
	 * touching their sides is {@code sideWire}.
	 */
	private static void modesWithSideInput(GameTestHelper h, Block sideWire) {
		floor(h);
		// Pair A (compare): Secret z = 0, vanilla z = 2, sharing the wire line z = 1.
		// Pair B (subtract): Secret z = 4, vanilla z = 6, sharing the wire line z = 5.
		ComparatorMode[] modes = {ComparatorMode.COMPARE, ComparatorMode.SUBTRACT};
		for (int pair = 0; pair < 2; pair++) {
			int z = pair * 4;
			h.setBlock(4, 1, z + 1, sideWire);
			h.setBlock(5, 1, z + 1, Blocks.REDSTONE_WIRE);
			h.setBlock(6, 1, z + 1, Blocks.REDSTONE_WIRE);
			h.setBlock(7, 1, z + 1, Blocks.REDSTONE_BLOCK);
			h.setBlock(4, 1, z, comparator(SECRET, modes[pair]));
			h.setBlock(4, 1, z + 2, comparator(Blocks.COMPARATOR, modes[pair]));
			h.setBlock(3, 1, z, Blocks.REDSTONE_BLOCK);
			h.setBlock(3, 1, z + 2, Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(10, () -> {
			check(h, h.getBlockState(new BlockPos(4, 1, 1)).getValue(RedStoneWireBlock.POWER) == 13,
					"control: the side wire should carry 13, carries " + h.getBlockState(new BlockPos(4, 1, 1)).getValue(RedStoneWireBlock.POWER));
			int[] expected = {15, 2};
			for (int pair = 0; pair < 2; pair++) {
				int z = pair * 4;
				check(h, output(h, 4, 1, z + 2) == expected[pair], "control: a vanilla comparator in " + modes[pair] + " should give " + expected[pair] + ", gives " + output(h, 4, 1, z + 2));
				check(h, output(h, 4, 1, z) == expected[pair], "the Secret Comparator in " + modes[pair] + " should give " + expected[pair] + ", gives " + output(h, 4, 1, z));
			}
			h.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void compareAndSubtractWithVanillaWireAtTheSide(GameTestHelper h) {
		modesWithSideInput(h, Blocks.REDSTONE_WIRE);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void compareAndSubtractWithSecretDustAtTheSide(GameTestHelper h) {
		modesWithSideInput(h, ModBlocks.SECRET_REDSTONE);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void compareAndSubtractWithSecretWireAtTheSide(GameTestHelper h) {
		modesWithSideInput(h, ModBlocks.SECRET_REDSTONE_WIRE);
	}

	/** Use toggles compare ↔ subtract and keeps the disguise; a player who may not build changes nothing. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void useTogglesTheMode(GameTestHelper h) {
		floor(h);
		BlockPos pos = new BlockPos(2, 1, 2);
		h.setBlock(pos, comparator(SECRET, ComparatorMode.COMPARE));
		secret(h, pos).setState(Blocks.BRICKS.defaultBlockState());
		Player player = h.makeMockPlayer();
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
		for (ComparatorMode expected : new ComparatorMode[] {ComparatorMode.SUBTRACT, ComparatorMode.COMPARE}) {
			InteractionResult result = h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
			check(h, result.consumesAction(), "use should toggle the mode, got " + result);
			check(h, h.getBlockState(pos).getValue(ComparatorBlock.MODE) == expected, "mode should be " + expected + ", is " + h.getBlockState(pos).getValue(ComparatorBlock.MODE));
		}
		checkBlock(h, pos, SECRET);
		checkFaces(h, pos, Blocks.BRICKS);
		player.getAbilities().mayBuild = false;
		InteractionResult refused = h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
		check(h, refused == InteractionResult.PASS && h.getBlockState(pos).getValue(ComparatorBlock.MODE) == ComparatorMode.COMPARE,
				"a player who may not build must not change the mode, got " + refused);
		h.succeed();
	}

	/**
	 * It hears of a container change: a chest filled after the comparator was
	 * placed raises its output, directly behind it and through a solid block,
	 * as for a vanilla comparator.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void containerChangeUpdatesIt(GameTestHelper h) {
		floor(h);
		// z = 1: chest, Secret Comparator. z = 3: chest, stone, Secret Comparator. z = 5: chest, vanilla. z = 7: chest, stone, vanilla.
		for (int z : new int[] {1, 3, 5, 7}) {
			boolean through = z == 3 || z == 7;
			h.setBlock(1, 1, z, Blocks.CHEST);
			if (through) {
				h.setBlock(2, 1, z, Blocks.STONE);
			}
			h.setBlock(through ? 3 : 2, 1, z, comparator(z < 5 ? SECRET : Blocks.COMPARATOR, ComparatorMode.COMPARE));
		}
		h.runAfterDelay(6, () -> {
			for (int z : new int[] {1, 3, 5, 7}) {
				check(h, output(h, z == 3 || z == 7 ? 3 : 2, 1, z) == 0, "row z=" + z + ": an empty chest should read 0");
				fill(h, new BlockPos(1, 1, z), 27);
			}
		});
		h.runAfterDelay(14, () -> {
			check(h, output(h, 2, 1, 5) == 15 && output(h, 3, 1, 7) == 15, "control: the vanilla comparators should read the filled chests as 15, read "
					+ output(h, 2, 1, 5) + " and " + output(h, 3, 1, 7));
			check(h, output(h, 2, 1, 1) == 15, "the Secret Comparator behind the chest should read 15 after it was filled, reads " + output(h, 2, 1, 1));
			check(h, output(h, 3, 1, 3) == 15, "the Secret Comparator one solid block from the chest should read 15 after it was filled, reads " + output(h, 3, 1, 3));
			for (int z : new int[] {1, 3, 5, 7}) {
				fill(h, new BlockPos(1, 1, z), 0);
			}
			h.succeed();
		});
	}

	/**
	 * The use it is made for: it reads a secret container. A Secret Chest filled
	 * after the comparator was placed reads 15, for a Secret Comparator as for a
	 * vanilla one.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void readsASecretChestFilledLater(GameTestHelper h) {
		floor(h);
		// z = 1: Secret Chest, Secret Comparator. z = 4: Secret Chest, vanilla comparator.
		for (int z : new int[] {1, 4}) {
			h.setBlock(1, 1, z, ModBlocks.SECRET_CHEST);
			h.setBlock(2, 1, z, comparator(z == 1 ? SECRET : Blocks.COMPARATOR, ComparatorMode.COMPARE));
		}
		h.runAfterDelay(6, () -> {
			for (int z : new int[] {1, 4}) {
				check(h, output(h, 2, 1, z) == 0, "row z=" + z + ": an empty Secret Chest should read 0");
				fill(h, new BlockPos(1, 1, z), 27);
			}
		});
		h.runAfterDelay(14, () -> {
			check(h, output(h, 2, 1, 4) == 15, "control: a vanilla comparator should read the full Secret Chest as 15, reads " + output(h, 2, 1, 4));
			check(h, output(h, 2, 1, 1) == 15, "the Secret Comparator should read the full Secret Chest as 15, reads " + output(h, 2, 1, 1));
			fill(h, new BlockPos(1, 1, 1), 0);
			fill(h, new BlockPos(1, 1, 4), 0);
			h.succeed();
		});
	}

	/**
	 * A piston may extend into it, as into a vanilla comparator (which it
	 * breaks) and stone (which it pushes). Breaking is tested with the other
	 * redstone in {@code PushableGameTests}.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void pistonsMayExtendIntoItAsVanilla(GameTestHelper h) {
		floor(h);
		Block[] blocks = {SECRET, Blocks.COMPARATOR, Blocks.STONE};
		boolean[] pushable = {true, true, true};
		for (int i = 0; i < 3; i++) {
			BlockPos pos = new BlockPos(1 + 2 * i, 1, 2);
			h.setBlock(pos, blocks[i]);
			boolean result = net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos), Direction.NORTH, true, Direction.NORTH);
			check(h, result == pushable[i], "a piston facing " + blocks[i] + " may extend: expected " + pushable[i] + ", got " + result);
		}
		h.succeed();
	}

	/** An item frame on the far side of a solid block behind it: rotation 3 reads 4, as for a vanilla comparator. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void itemFrameThroughABlock(GameTestHelper h) {
		floor(h);
		// Comparator x = 4, stone x = 3, the frame hangs on the stone's west face at x = 2. Secret z = 1, vanilla z = 5.
		// The test world is kept between runs, and two frames in one spot read as none: start clean.
		discardFrames(h);
		for (int z : new int[] {1, 5}) {
			h.setBlock(3, 1, z, Blocks.STONE);
			h.setBlock(4, 1, z, comparator(z == 1 ? SECRET : Blocks.COMPARATOR, ComparatorMode.COMPARE));
		}
		h.runAfterDelay(4, () -> {
			for (int z : new int[] {1, 5}) {
				ItemFrame frame = new ItemFrame(h.getLevel(), h.absolutePos(new BlockPos(2, 1, z)), Direction.WEST);
				h.getLevel().addFreshEntity(frame);
				frame.setItem(new ItemStack(Items.FEATHER));
				frame.setRotation(3);
			}
		});
		h.runAfterDelay(12, () -> {
			check(h, output(h, 4, 1, 5) == 4, "control: a vanilla comparator should read the frame as 4, reads " + output(h, 4, 1, 5));
			check(h, output(h, 4, 1, 1) == 4, "the Secret Comparator should read the frame as 4, reads " + output(h, 4, 1, 1));
			discardFrames(h);
			h.succeed();
		});
	}

	private static void discardFrames(GameTestHelper h) {
		h.getLevel().getEntitiesOfClass(ItemFrame.class, new AABB(h.absolutePos(new BlockPos(3, 1, 3))).inflate(6)).forEach(ItemFrame::discard);
	}

	/** A powered comparator at a repeater's side locks it, Secret and vanilla in both combinations. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void locksARepeaterAtItsSide(GameTestHelper h) {
		floor(h);
		// Row z = 0: a Secret Comparator locks a vanilla repeater. Row z = 4: a vanilla comparator locks a Secret Repeater.
		Block[][] rows = {{SECRET, Blocks.REPEATER}, {Blocks.COMPARATOR, ModBlocks.SECRET_REPEATER}};
		for (int i = 0; i < 2; i++) {
			int z = i * 4;
			h.setBlock(3, 1, z, rows[i][1].defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST));
			h.setBlock(3, 1, z + 1, rows[i][0].defaultBlockState().setValue(ComparatorBlock.FACING, Direction.SOUTH));
			h.setBlock(3, 1, z + 2, Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(10, () -> {
			for (int i = 0; i < 2; i++) {
				BlockState repeater = h.getBlockState(new BlockPos(3, 1, i * 4));
				check(h, repeater.getValue(RepeaterBlock.LOCKED), rows[i][1] + " should be locked by the powered " + rows[i][0] + " at its side, is " + repeater);
			}
			h.succeed();
		});
	}

	/**
	 * Output to the front only and no strong power through it: a lamp in front
	 * lights and a lamp at the side does not (as with a vanilla comparator); a
	 * powered repeater pointing into its side leaves the lamp beyond it off,
	 * while the same through stone lights the lamp.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void outputToTheFrontAndNotAConductor(GameTestHelper h) {
		floor(h);
		// z = 0: source, comparator, lamp in front; a lamp at its side on z = 1. Secret x = 0..2, vanilla x = 4..6.
		for (int x : new int[] {0, 4}) {
			h.setBlock(x + 2, 1, 0, Blocks.REDSTONE_LAMP);
			h.setBlock(x + 1, 1, 1, Blocks.REDSTONE_LAMP);
			h.setBlock(x + 1, 1, 0, comparator(x == 0 ? SECRET : Blocks.COMPARATOR, ComparatorMode.COMPARE));
			h.setBlock(x, 1, 0, Blocks.REDSTONE_BLOCK);
		}
		// z = 4: source, repeater, a Secret Comparator on the z axis (entered from its side), lamp. z = 7: stone instead.
		h.setBlock(3, 1, 4, SECRET.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
		h.setBlock(3, 1, 7, Blocks.STONE);
		for (int z : new int[] {4, 7}) {
			h.setBlock(4, 1, z, Blocks.REDSTONE_LAMP);
			h.setBlock(2, 1, z, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST));
			h.setBlock(1, 1, z, Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(10, () -> {
			check(h, lit(h, 6, 1, 0) && !lit(h, 5, 1, 1), "control: a vanilla comparator lights the lamp in front and not the one at its side");
			check(h, lit(h, 2, 1, 0), "the lamp in front of the Secret Comparator should light");
			check(h, !lit(h, 1, 1, 1), "the lamp at the Secret Comparator's side must stay off");
			check(h, lit(h, 4, 1, 7), "control: strong power through stone should light the lamp");
			check(h, !lit(h, 4, 1, 4), "strong power must not pass through the Secret Comparator");
			check(h, !SECRET.defaultBlockState().isRedstoneConductor(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 4))),
					"the Secret Comparator must not be a redstone conductor");
			h.succeed();
		});
	}

	private static boolean lit(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(RedstoneLampBlock.LIT);
	}

	/** Wire beside it takes the same shape as beside a vanilla comparator, at its side and at its input. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void wireConnectsAsToAVanillaComparator(GameTestHelper h) {
		floor(h);
		Block[] blocks = {SECRET, Blocks.COMPARATOR};
		for (int i = 0; i < 2; i++) {
			int x = 2 + i * 4;
			// Wire first: the comparator's placement then reshapes the wire beside it.
			h.setBlock(x, 1, 2, Blocks.REDSTONE_WIRE);
			h.setBlock(x - 1, 1, 3, Blocks.REDSTONE_WIRE);
			h.setBlock(x, 1, 3, comparator(blocks[i], ComparatorMode.COMPARE));
		}
		h.runAfterDelay(4, () -> {
			for (int[] wire : new int[][] {{0, 2}, {-1, 3}}) {
				BlockState atSecret = h.getBlockState(new BlockPos(2 + wire[0], 1, wire[1]));
				BlockState atVanilla = h.getBlockState(new BlockPos(6 + wire[0], 1, wire[1]));
				for (var side : new net.minecraft.world.level.block.state.properties.EnumProperty[] {RedStoneWireBlock.NORTH, RedStoneWireBlock.SOUTH, RedStoneWireBlock.EAST, RedStoneWireBlock.WEST}) {
					check(h, atSecret.getValue(side) == atVanilla.getValue(side), "wire beside the Secret Comparator is " + atSecret + ", beside the vanilla one " + atVanilla);
				}
			}
			BlockState input = h.getBlockState(new BlockPos(5, 1, 3));
			check(h, input.getValue(RedStoneWireBlock.EAST).isConnected(), "control: wire at a vanilla comparator's input points at it, got " + input);
			h.succeed();
		});
	}

	/** Placed through the item, it faces as a vanilla comparator does, in compare mode. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void placementMatchesVanilla(GameTestHelper h) {
		floor(h);
		place(h, SECRET.asItem(), new Vec3(1.5, 1, 4.9), new BlockPos(1, 0, 3), Direction.UP, new Vec3(1.5, 1.0, 3.5));
		place(h, Items.COMPARATOR, new Vec3(5.5, 1, 4.9), new BlockPos(5, 0, 3), Direction.UP, new Vec3(5.5, 1.0, 3.5));
		checkBlock(h, new BlockPos(1, 1, 3), SECRET);
		checkBlock(h, new BlockPos(5, 1, 3), Blocks.COMPARATOR);
		BlockState secret = h.getBlockState(new BlockPos(1, 1, 3));
		BlockState vanilla = h.getBlockState(new BlockPos(5, 1, 3));
		check(h, secret.getValue(ComparatorBlock.FACING) == vanilla.getValue(ComparatorBlock.FACING),
				"the Secret Comparator faces " + secret.getValue(ComparatorBlock.FACING) + ", the vanilla one " + vanilla.getValue(ComparatorBlock.FACING));
		check(h, vanilla.getValue(ComparatorBlock.FACING) == Direction.SOUTH, "control: a player looking north places a comparator facing south, got " + vanilla.getValue(ComparatorBlock.FACING));
		check(h, secret.getValue(ComparatorBlock.MODE) == ComparatorMode.COMPARE, "a new Secret Comparator should be in compare mode");
		check(h, h.getBlockEntity(new BlockPos(1, 1, 3)) instanceof SecretComparatorBlockEntity, "the placed block should have the Secret Comparator's block entity");
		check(h, secret.isCollisionShapeFullBlock(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 3))), "the Secret Comparator should be a full block");
		check(h, !vanilla.isCollisionShapeFullBlock(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 3))), "control: a vanilla comparator is not a full block");
		h.succeed();
	}

	/** It stays when the block under it goes (a vanilla comparator pops). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void staysWhenTheFloorGoes(GameTestHelper h) {
		floor(h);
		h.setBlock(2, 1, 2, comparator(SECRET, ComparatorMode.COMPARE));
		h.setBlock(5, 1, 2, comparator(Blocks.COMPARATOR, ComparatorMode.COMPARE));
		h.setBlock(2, 0, 2, Blocks.AIR);
		h.setBlock(5, 0, 2, Blocks.AIR);
		h.runAfterDelay(2, () -> {
			checkBlock(h, new BlockPos(2, 1, 2), SECRET);
			checkBlock(h, new BlockPos(5, 1, 2), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Saved data: its own block-entity id, the output signal and the disguise,
	 * all read back by a fresh block entity. Clients get the disguise only.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void savesItsOutputAndDisguise(GameTestHelper h) {
		floor(h);
		BlockPos pos = new BlockPos(2, 1, 2);
		h.setBlock(pos, comparator(SECRET, ComparatorMode.COMPARE));
		secret(h, pos).setState(Blocks.BRICKS.defaultBlockState());
		h.setBlock(1, 1, 2, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(6, () -> {
			check(h, output(h, 2, 1, 2) == 15, "the Secret Comparator behind a redstone block should read 15, reads " + output(h, 2, 1, 2));
			BlockEntity entity = h.getBlockEntity(pos);
			CompoundTag saved = entity.saveWithFullMetadata();
			check(h, "secretblocks:secret_comparator".equals(saved.getString("id")), "saved id should be secretblocks:secret_comparator, is " + saved.getString("id"));
			check(h, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString().equals("secretblocks:secret_comparator"), "its type should be ours, is " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()));
			check(h, saved.getInt("OutputSignal") == 15, "the saved data should hold OutputSignal 15: " + saved);
			check(h, saved.contains("northState"), "the saved data should hold the disguise: " + saved);
			BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(pos), h.getBlockState(pos), saved);
			check(h, loaded instanceof SecretComparatorBlockEntity, "the saved data should load as a Secret Comparator block entity, got " + loaded);
			SecretComparatorBlockEntity again = (SecretComparatorBlockEntity) loaded;
			check(h, again.getOutputSignal() == 15, "the loaded output should be 15, is " + again.getOutputSignal());
			check(h, again.getDisguise().save(new CompoundTag()).equals(((SecretComparatorBlockEntity) entity).getDisguise().save(new CompoundTag())),
					"the loaded disguise should equal the saved one");
			check(h, !entity.getUpdateTag().contains("OutputSignal"), "clients must not receive the output signal: " + entity.getUpdateTag());
			h.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void recipe(GameTestHelper h) {
		Optional<ItemStack> made = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.COMPARATOR);
		check(h, made.isPresent() && made.get().is(SECRET.asItem()), "Dummy Block + comparator should make the Secret Comparator, got " + made);
		h.succeed();
	}
}
