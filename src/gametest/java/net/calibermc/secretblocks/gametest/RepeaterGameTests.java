package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Repeater is a vanilla repeater in a full disguised block. Timing, placement, locking,
 * output direction, wire connections and floor removal are each compared with
 * a vanilla repeater in the same test; use and the recipe have no vanilla
 * twin to compare with.
 */
public class RepeaterGameTests implements FabricGameTest {

	private static final Block SECRET = ModBlocks.SECRET_REPEATER;

	/** A repeater at (2, 1, z): FACING is its input side, so WEST means input from x = 1, output to x = 3. */
	private static BlockState repeater(Block block, int delay) {
		return block.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST).setValue(RepeaterBlock.DELAY, delay);
	}

	private static boolean lit(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(RedstoneLampBlock.LIT);
	}

	/**
	 * For delay 1 to 4, the lamp in front lights on the same tick as behind the
	 * vanilla repeater, 2 ticks per delay step.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void delayMatchesVanilla(GameTestHelper h) {
		floor(h);
		// Delay d on row z = 2(d - 1), rows one apart so neighbouring lamps (solid
		// blocks the repeaters power strongly) cannot light each other. Secret
		// Repeater: source x = 0, repeater x = 1, lamp x = 2; vanilla: x = 4, 5, 6.
		long[][] litAt = new long[4][2];
		for (int d = 1; d <= 4; d++) {
			int z = 2 * (d - 1);
			for (int side = 0; side < 2; side++) {
				int x = side * 4;
				h.setBlock(x + 2, 1, z, Blocks.REDSTONE_LAMP);
				h.setBlock(x + 1, 1, z, repeater(side == 0 ? SECRET : Blocks.REPEATER, d));
			}
		}
		long start = h.getTick();
		for (int d = 1; d <= 4; d++) {
			h.setBlock(0, 1, 2 * (d - 1), Blocks.REDSTONE_BLOCK);
			h.setBlock(4, 1, 2 * (d - 1), Blocks.REDSTONE_BLOCK);
		}
		h.onEachTick(() -> {
			for (int d = 0; d < 4; d++) {
				for (int side = 0; side < 2; side++) {
					if (litAt[d][side] == 0 && lit(h, side * 4 + 2, 1, 2 * d)) {
						litAt[d][side] = h.getTick() - start;
					}
				}
			}
		});
		h.runAfterDelay(30, () -> {
			for (int d = 0; d < 4; d++) {
				check(h, litAt[d][1] > 0, "control: the vanilla repeater with delay " + (d + 1) + " should light its lamp");
				check(h, litAt[d][0] == litAt[d][1], "delay " + (d + 1) + ": the Secret Repeater lit its lamp after " + litAt[d][0]
						+ " ticks, the vanilla one after " + litAt[d][1]);
			}
			check(h, litAt[3][1] - litAt[0][1] == 6, "control: 2 ticks per delay step, got " + (litAt[3][1] - litAt[0][1]) + " from delay 1 to 4");
			h.succeed();
		});
	}

	/** Use cycles the delay 1 → 2 → 3 → 4 → 1; a player who may not build changes nothing. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void useCyclesTheDelay(GameTestHelper h) {
		floor(h);
		BlockPos pos = new BlockPos(2, 1, 2);
		h.setBlock(pos, repeater(SECRET, 1));
		secret(h, pos).setState(Blocks.BRICKS.defaultBlockState());
		Player player = h.makeMockPlayer();
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
		for (int expected : new int[] {2, 3, 4, 1}) {
			InteractionResult result = h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
			check(h, result.consumesAction(), "use should cycle the delay, got " + result);
			check(h, h.getBlockState(pos).getValue(RepeaterBlock.DELAY) == expected, "delay should be " + expected + ", is " + h.getBlockState(pos).getValue(RepeaterBlock.DELAY));
		}
		checkBlock(h, pos, SECRET);
		checkFaces(h, pos, Blocks.BRICKS);
		player.getAbilities().mayBuild = false;
		InteractionResult refused = h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
		check(h, refused == InteractionResult.PASS && h.getBlockState(pos).getValue(RepeaterBlock.DELAY) == 1,
				"a player who may not build must not change the delay, got " + refused);
		h.succeed();
	}

	/** A powered diode at the side locks a repeater, both ways between Secret and vanilla repeaters. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void sideLockingBothWays(GameTestHelper h) {
		floor(h);
		// Row z = 2: a Secret Repeater (x = 3) locked by a powered vanilla repeater at its south side.
		// Row z = 5: a vanilla repeater locked by a powered Secret Repeater. The lockers face NORTH
		// (input from the south), fed by a redstone block, so they output north into the locked one.
		Block[][] rows = {{SECRET, Blocks.REPEATER}, {Blocks.REPEATER, SECRET}};
		for (int i = 0; i < 2; i++) {
			int z = 2 + i * 3;
			h.setBlock(3, 1, z, repeater(rows[i][0], 1));
			h.setBlock(3, 1, z + 1, rows[i][1].defaultBlockState().setValue(RepeaterBlock.FACING, Direction.SOUTH));
			h.setBlock(3, 1, z + 2, Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(10, () -> {
			for (int i = 0; i < 2; i++) {
				int z = 2 + i * 3;
				BlockState locked = h.getBlockState(new BlockPos(3, 1, z));
				boolean isLocked = ((RepeaterBlock) locked.getBlock()).isLocked(h.getLevel(), h.absolutePos(new BlockPos(3, 1, z)), locked);
				check(h, isLocked, locked.getBlock() + " should be locked by the powered " + rows[i][1] + " at its side");
			}
			h.succeed();
		});
	}

	/**
	 * A locked repeater holds its output: locked while off, it stays off when its
	 * input comes on (Secret and vanilla alike), while an unlocked Secret Repeater
	 * with the same input lights its lamp.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lockedRepeaterHoldsItsOutput(GameTestHelper h) {
		floor(h);
		// Rows z = 0 (Secret) and z = 4 (vanilla): repeater x = 3 into a lamp at x = 4, locked from the south.
		Block[] locked = {SECRET, Blocks.REPEATER};
		for (int i = 0; i < 2; i++) {
			int z = i * 4;
			h.setBlock(3, 1, z, repeater(locked[i], 1));
			h.setBlock(4, 1, z, Blocks.REDSTONE_LAMP);
			h.setBlock(3, 1, z + 1, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.SOUTH));
			h.setBlock(3, 1, z + 2, Blocks.REDSTONE_BLOCK);
		}
		// Control, two blocks up: an unlocked Secret Repeater (it needs no floor).
		h.setBlock(3, 3, 0, repeater(SECRET, 1));
		h.setBlock(4, 3, 0, Blocks.REDSTONE_LAMP);
		h.runAfterDelay(8, () -> {
			for (int i = 0; i < 2; i++) {
				check(h, h.getBlockState(new BlockPos(3, 1, i * 4)).getValue(RepeaterBlock.LOCKED), locked[i] + " should show locked=true");
				h.setBlock(2, 1, i * 4, Blocks.REDSTONE_BLOCK);
			}
			h.setBlock(2, 3, 0, Blocks.REDSTONE_BLOCK);
		});
		h.runAfterDelay(20, () -> {
			check(h, lit(h, 4, 3, 0), "control: the unlocked Secret Repeater should light its lamp");
			for (int i = 0; i < 2; i++) {
				check(h, !lit(h, 4, 1, i * 4), "the locked " + locked[i] + " must keep its output off when its input comes on");
				check(h, !h.getBlockState(new BlockPos(3, 1, i * 4)).getValue(RepeaterBlock.POWERED), "the locked " + locked[i] + " must stay unpowered");
			}
			h.succeed();
		});
	}

	/** The other half: locked while on, it stays on when its input goes (an unlocked one turns off). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lockedRepeaterStaysOnWithoutInput(GameTestHelper h) {
		floor(h);
		Block[] locked = {SECRET, Blocks.REPEATER};
		for (int i = 0; i < 2; i++) {
			int z = i * 4;
			h.setBlock(3, 1, z, repeater(locked[i], 1));
			h.setBlock(4, 1, z, Blocks.REDSTONE_LAMP);
			h.setBlock(2, 1, z, Blocks.REDSTONE_BLOCK);
		}
		h.setBlock(3, 3, 0, repeater(SECRET, 1));
		h.setBlock(4, 3, 0, Blocks.REDSTONE_LAMP);
		h.setBlock(2, 3, 0, Blocks.REDSTONE_BLOCK);
		// Once they are on, lock the two on the floor from the south.
		h.runAfterDelay(8, () -> {
			for (int i = 0; i < 2; i++) {
				check(h, lit(h, 4, 1, i * 4), "control: " + locked[i] + " should be on before it is locked");
				h.setBlock(3, 1, i * 4 + 1, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.SOUTH));
				h.setBlock(3, 1, i * 4 + 2, Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(16, () -> {
			for (int i = 0; i < 2; i++) {
				check(h, h.getBlockState(new BlockPos(3, 1, i * 4)).getValue(RepeaterBlock.LOCKED), locked[i] + " should show locked=true");
				h.setBlock(2, 1, i * 4, Blocks.AIR);
			}
			h.setBlock(2, 3, 0, Blocks.AIR);
		});
		h.runAfterDelay(28, () -> {
			check(h, !lit(h, 4, 3, 0), "control: the unlocked Secret Repeater should turn its lamp off");
			for (int i = 0; i < 2; i++) {
				check(h, lit(h, 4, 1, i * 4), "the locked " + locked[i] + " must keep its output on when its input goes");
			}
			h.succeed();
		});
	}

	/** Placed through the item, it faces as a vanilla repeater does, and is locked at once beside a powered diode. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void placementMatchesVanilla(GameTestHelper h) {
		floor(h);
		// Row z = 1: plain placement. Row z = 5: a powered repeater at x + 1 points into the spot's side.
		for (int x : new int[] {1, 5}) {
			h.setBlock(x + 1, 1, 5, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.EAST));
			h.setBlock(x + 2, 1, 5, Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(8, () -> {
			for (int z : new int[] {1, 5}) {
				place(h, SECRET.asItem(), new Vec3(1.5, 1, z + 1.9), new BlockPos(1, 0, z), Direction.UP, new Vec3(1.5, 1.0, z + 0.5));
				place(h, Items.REPEATER, new Vec3(5.5, 1, z + 1.9), new BlockPos(5, 0, z), Direction.UP, new Vec3(5.5, 1.0, z + 0.5));
				checkBlock(h, new BlockPos(1, 1, z), SECRET);
				checkBlock(h, new BlockPos(5, 1, z), Blocks.REPEATER);
				BlockState secret = h.getBlockState(new BlockPos(1, 1, z));
				BlockState vanilla = h.getBlockState(new BlockPos(5, 1, z));
				check(h, secret.getValue(RepeaterBlock.FACING) == vanilla.getValue(RepeaterBlock.FACING),
						"the Secret Repeater faces " + secret.getValue(RepeaterBlock.FACING) + ", the vanilla one " + vanilla.getValue(RepeaterBlock.FACING));
				check(h, vanilla.getValue(RepeaterBlock.FACING) == Direction.SOUTH, "control: a player looking north places a repeater facing south, got " + vanilla.getValue(RepeaterBlock.FACING));
				check(h, secret.getValue(RepeaterBlock.LOCKED) == vanilla.getValue(RepeaterBlock.LOCKED) && vanilla.getValue(RepeaterBlock.LOCKED) == (z == 5),
						"row z=" + z + ": locked at placement should be " + (z == 5) + " for both, Secret " + secret.getValue(RepeaterBlock.LOCKED) + ", vanilla " + vanilla.getValue(RepeaterBlock.LOCKED));
			}
			h.succeed();
		});
	}

	/**
	 * Vanilla wire points at a repeater only along its axis. Wire at the side of a
	 * Secret Repeater must take the same shape as at the side of a vanilla one (no
	 * arm toward it), and wire at its input the same as at a vanilla one's input.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void wireConnectsAsToAVanillaRepeater(GameTestHelper h) {
		floor(h);
		// Repeaters at x = 2 (Secret) and x = 6 (vanilla), z = 3, on the x axis; wire at their north side and at their input.
		Block[] blocks = {SECRET, Blocks.REPEATER};
		for (int i = 0; i < 2; i++) {
			int x = 2 + i * 4;
			// Wire first: the repeater's placement then reshapes the wire beside it.
			h.setBlock(x, 1, 2, Blocks.REDSTONE_WIRE);
			h.setBlock(x - 1, 1, 3, Blocks.REDSTONE_WIRE);
			h.setBlock(x, 1, 3, repeater(blocks[i], 1));
		}
		h.runAfterDelay(4, () -> {
			BlockState sideSecret = h.getBlockState(new BlockPos(2, 1, 2));
			BlockState sideVanilla = h.getBlockState(new BlockPos(6, 1, 2));
			check(h, sideVanilla.getValue(RedStoneWireBlock.SOUTH) == RedstoneSide.NONE, "control: wire at a vanilla repeater's side has no arm toward it, got " + sideVanilla);
			check(h, sideSecret.getValue(RedStoneWireBlock.SOUTH) == RedstoneSide.NONE, "wire at the Secret Repeater's side must have no arm toward it, got " + sideSecret);
			BlockState inputSecret = h.getBlockState(new BlockPos(1, 1, 3));
			BlockState inputVanilla = h.getBlockState(new BlockPos(5, 1, 3));
			check(h, inputVanilla.getValue(RedStoneWireBlock.EAST) == RedstoneSide.SIDE, "control: wire at a vanilla repeater's input points at it, got " + inputVanilla);
			check(h, inputSecret.getValue(RedStoneWireBlock.EAST) == RedstoneSide.SIDE, "wire at the Secret Repeater's input should point at it, got " + inputSecret);
			h.succeed();
		});
	}

	/**
	 * Strong power does not pass through it: a powered repeater pointing into the
	 * Secret Repeater's side leaves the lamp beyond it off, while the same through
	 * stone lights the lamp.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void strongPowerDoesNotPassThrough(GameTestHelper h) {
		floor(h);
		// Row z = 1: source, repeater, Secret Repeater (on the z axis, so entered from its side), lamp. Row z = 4: stone instead.
		h.setBlock(3, 1, 1, SECRET.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH));
		h.setBlock(3, 1, 4, Blocks.STONE);
		for (int z : new int[] {1, 4}) {
			h.setBlock(4, 1, z, Blocks.REDSTONE_LAMP);
			h.setBlock(2, 1, z, repeater(Blocks.REPEATER, 1));
			h.setBlock(1, 1, z, Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(10, () -> {
			check(h, lit(h, 4, 1, 4), "control: strong power through stone should light the lamp");
			check(h, !lit(h, 4, 1, 1), "strong power must not pass through the Secret Repeater");
			h.succeed();
		});
	}

	/** A powered repeater powers the lamp in front of it only, not a lamp at its side; the Secret one is not a conductor. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void outputOnlyToTheFront(GameTestHelper h) {
		floor(h);
		h.setBlock(3, 1, 2, Blocks.REDSTONE_LAMP);
		h.setBlock(2, 1, 3, Blocks.REDSTONE_LAMP);
		h.setBlock(2, 1, 2, repeater(SECRET, 1));
		h.setBlock(1, 1, 2, Blocks.REDSTONE_BLOCK);
		// The same with a vanilla repeater, three rows on.
		h.setBlock(3, 1, 5, Blocks.REDSTONE_LAMP);
		h.setBlock(2, 1, 6, Blocks.REDSTONE_LAMP);
		h.setBlock(2, 1, 5, repeater(Blocks.REPEATER, 1));
		h.setBlock(1, 1, 5, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(10, () -> {
			check(h, lit(h, 3, 1, 5) && !lit(h, 2, 1, 6), "control: a vanilla repeater lights the lamp in front and not the one at its side");
			check(h, lit(h, 3, 1, 2), "the lamp in front should light");
			check(h, !lit(h, 2, 1, 3), "the lamp at the side must stay off");
			check(h, !SECRET.defaultBlockState().isRedstoneConductor(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2))),
					"the Secret Repeater must not be a redstone conductor");
			h.succeed();
		});
	}

	/** The Secret Repeater stays when the block under it goes (a vanilla repeater pops). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void staysWhenTheFloorGoes(GameTestHelper h) {
		floor(h);
		h.setBlock(2, 1, 2, repeater(SECRET, 1));
		h.setBlock(5, 1, 2, repeater(Blocks.REPEATER, 1));
		h.setBlock(2, 0, 2, Blocks.AIR);
		h.setBlock(5, 0, 2, Blocks.AIR);
		h.runAfterDelay(2, () -> {
			checkBlock(h, new BlockPos(2, 1, 2), SECRET);
			checkBlock(h, new BlockPos(5, 1, 2), Blocks.AIR);
			h.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void recipe(GameTestHelper h) {
		Optional<ItemStack> made = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.REPEATER);
		check(h, made.isPresent() && made.get().is(SECRET.asItem()), "Dummy Block + repeater should make the Secret Repeater, got " + made);
		h.succeed();
	}
}
