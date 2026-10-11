package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Secret Redstone Wire, the original mod's six-sided wire. Every expected
 * value follows the wire's rule: 15 next to the source, 1 less per block in
 * any direction and across Wire, Dust and vanilla wire, 0 once the source is
 * gone.
 */
public class RedstoneWireGameTests {

	private static final Block WIRE = ModBlocks.SECRET_REDSTONE_WIRE;

	private static int power(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(RedStoneWireBlock.POWER);
	}

	private static boolean lit(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(RedstoneLampBlock.LIT);
	}

	/** A line of Wire from a redstone block reads 15, 14, 13, ... and lights the lamp at its end. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lineLosesOnePerBlock(GameTestHelper h) {
		floor(h);
		h.setBlock(7, 1, 2, Blocks.REDSTONE_LAMP);
		for (int x = 1; x <= 6; x++) {
			h.setBlock(x, 1, 2, WIRE);
		}
		h.setBlock(0, 1, 2, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			for (int x = 1; x <= 6; x++) {
				check(h, power(h, x, 1, 2) == 16 - x, "Wire " + x + " should be " + (16 - x) + ", is " + power(h, x, 1, 2));
			}
			check(h, lit(h, 7, 1, 2), "the lamp at the end should light");
			h.succeed();
		});
	}

	/** A column carries the signal up from a source below and down from a source above. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void columnCarriesUpAndDown(GameTestHelper h) {
		floor(h);
		for (int y = 2; y <= 6; y++) {
			h.setBlock(1, y, 1, WIRE);
		}
		h.setBlock(1, 1, 1, Blocks.REDSTONE_BLOCK);
		for (int y = 1; y <= 5; y++) {
			h.setBlock(5, y, 5, WIRE);
		}
		h.setBlock(5, 6, 5, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			for (int i = 0; i < 5; i++) {
				check(h, power(h, 1, 2 + i, 1) == 15 - i, "going up, Wire " + i + " should be " + (15 - i) + ", is " + power(h, 1, 2 + i, 1));
				check(h, power(h, 5, 5 - i, 5) == 15 - i, "going down, Wire " + i + " should be " + (15 - i) + ", is " + power(h, 5, 5 - i, 5));
			}
			h.succeed();
		});
	}

	/** Wire, Dust and vanilla wire mixed: 1 lost per block; all 0 within 10 ticks of the source going (no latch). */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void mixedWithDustAndVanillaWire(GameTestHelper h) {
		floor(h);
		Block[] line = {WIRE, ModBlocks.SECRET_REDSTONE, Blocks.REDSTONE_WIRE, WIRE, ModBlocks.SECRET_REDSTONE, Blocks.REDSTONE_WIRE};
		for (int i = 0; i < line.length; i++) {
			h.setBlock(1 + i, 1, 3, line[i]);
		}
		h.setBlock(0, 1, 3, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			for (int i = 0; i < line.length; i++) {
				check(h, power(h, 1 + i, 1, 3) == 15 - i, line[i] + " at " + i + " should be " + (15 - i) + ", is " + power(h, 1 + i, 1, 3));
			}
			h.setBlock(0, 1, 3, Blocks.AIR);
		});
		h.runAfterDelay(15, () -> {
			for (int i = 0; i < line.length; i++) {
				check(h, power(h, 1 + i, 1, 3) == 0, line[i] + " at " + i + " should be 0 after the source went, is " + power(h, 1 + i, 1, 3));
			}
			h.succeed();
		});
	}

	/** A lamp on each of the five free faces of a powered Wire lights. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lampOnEveryFaceLights(GameTestHelper h) {
		floor(h);
		int[][] lamps = {{4, 3, 3}, {3, 3, 4}, {3, 3, 2}, {3, 4, 3}, {3, 2, 3}};
		for (int[] lamp : lamps) {
			h.setBlock(lamp[0], lamp[1], lamp[2], Blocks.REDSTONE_LAMP);
		}
		h.setBlock(3, 3, 3, WIRE);
		h.setBlock(2, 3, 3, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			for (int[] lamp : lamps) {
				check(h, lit(h, lamp[0], lamp[1], lamp[2]), "the lamp at " + lamp[0] + "," + lamp[1] + "," + lamp[2] + " should light");
			}
			h.succeed();
		});
	}

	/**
	 * Weak power only: a lamp behind a block the Wire touches stays off. The
	 * control, vanilla wire pointing into a block, powers it strongly, so its
	 * lamp lights.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void weakPowerOnly(GameTestHelper h) {
		floor(h);
		for (int z : new int[] {2, 5}) {
			h.setBlock(4, 1, z, Blocks.REDSTONE_LAMP);
			h.setBlock(3, 1, z, Blocks.STONE);
			h.setBlock(2, 1, z, z == 2 ? WIRE : Blocks.REDSTONE_WIRE);
			h.setBlock(1, 1, z, Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(5, () -> {
			check(h, power(h, 2, 1, 2) == 15, "the Wire should be powered, is " + power(h, 2, 1, 2));
			check(h, lit(h, 4, 1, 5), "control: vanilla wire powers the stone strongly, so the lamp behind it lights");
			check(h, !lit(h, 4, 1, 2), "the Wire must not power through the stone");
			check(h, !WIRE.defaultBlockState().isRedstoneConductor(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2))),
					"the Wire must not be a redstone conductor");
			h.succeed();
		});
	}

	/** Breaking a Wire in the middle of a line leaves the far side unpowered. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void breakingTheMiddleSplitsTheLine(GameTestHelper h) {
		floor(h);
		for (int x = 1; x <= 6; x++) {
			h.setBlock(x, 1, 4, WIRE);
		}
		h.setBlock(0, 1, 4, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			for (int x = 4; x <= 6; x++) {
				check(h, power(h, x, 1, 4) == 16 - x, "before the break, Wire " + x + " should be " + (16 - x) + ", is " + power(h, x, 1, 4));
			}
			h.setBlock(3, 1, 4, Blocks.AIR);
		});
		h.runAfterDelay(15, () -> {
			check(h, power(h, 1, 1, 4) == 15 && power(h, 2, 1, 4) == 14, "the near side should stay 15, 14");
			for (int x = 4; x <= 6; x++) {
				check(h, power(h, x, 1, 4) == 0, "Wire " + x + " beyond the break should be 0, is " + power(h, x, 1, 4));
			}
			h.succeed();
		});
	}

	/** Vanilla wire and Dust resting on a Wire read it (power - 1), and a Wire resting on vanilla wire reads it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void verticalJoinsWithDustAndVanillaWire(GameTestHelper h) {
		floor(h);
		// Wire at y = 1 powered by a redstone block beside it; vanilla wire and Dust on top.
		for (int z : new int[] {1, 3}) {
			h.setBlock(2, 1, z, WIRE);
			h.setBlock(2, 2, z, z == 1 ? Blocks.REDSTONE_WIRE : ModBlocks.SECRET_REDSTONE);
			h.setBlock(1, 1, z, Blocks.REDSTONE_BLOCK);
		}
		// Vanilla wire at y = 1 powered by a redstone block; a Wire resting on top of it.
		h.setBlock(5, 1, 5, Blocks.REDSTONE_WIRE);
		h.setBlock(5, 2, 5, WIRE);
		h.setBlock(4, 1, 5, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			check(h, power(h, 2, 1, 1) == 15, "the Wire should be 15");
			check(h, power(h, 2, 2, 1) == 14, "vanilla wire on the Wire should be 14, is " + power(h, 2, 2, 1));
			check(h, power(h, 2, 2, 3) == 14, "Dust on the Wire should be 14, is " + power(h, 2, 2, 3));
			check(h, power(h, 5, 1, 5) == 15, "the vanilla wire should be 15");
			check(h, power(h, 5, 2, 5) == 14, "the Wire on vanilla wire should be 14, is " + power(h, 5, 2, 5));
			h.succeed();
		});
	}

	/**
	 * A 4×4×4 cluster powers from one corner (power falls by the distance) and
	 * turns fully off when the source goes, in one tick each. The times are
	 * printed; the bound is generous because the test machine is slow.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void clusterPowersAndUnpowers(GameTestHelper h) {
		for (int x = 2; x <= 5; x++) {
			for (int y = 1; y <= 4; y++) {
				for (int z = 2; z <= 5; z++) {
					h.setBlock(x, y, z, WIRE);
				}
			}
		}
		long on = System.nanoTime();
		h.setBlock(1, 1, 2, Blocks.REDSTONE_BLOCK);
		long onMs = (System.nanoTime() - on) / 1_000_000;
		h.runAfterDelay(2, () -> {
			for (int x = 2; x <= 5; x++) {
				for (int y = 1; y <= 4; y++) {
					for (int z = 2; z <= 5; z++) {
						int expected = 15 - ((x - 2) + (y - 1) + (z - 2));
						check(h, power(h, x, y, z) == expected, "Wire " + x + "," + y + "," + z + " should be " + expected + ", is " + power(h, x, y, z));
					}
				}
			}
			long off = System.nanoTime();
			h.setBlock(1, 1, 2, Blocks.AIR);
			long offMs = (System.nanoTime() - off) / 1_000_000;
			System.out.println("[wire timing] 4x4x4 Wire cluster: power on " + onMs + " ms, off " + offMs + " ms");
			check(h, onMs < 250 && offMs < 250, "the cluster took " + onMs + " ms on and " + offMs + " ms off (bound 250 ms)");
		});
		h.runAfterDelay(4, () -> {
			for (int x = 2; x <= 5; x++) {
				for (int y = 1; y <= 4; y++) {
					for (int z = 2; z <= 5; z++) {
						check(h, power(h, x, y, z) == 0, "Wire " + x + "," + y + "," + z + " should be 0, is " + power(h, x, y, z));
					}
				}
			}
			h.succeed();
		});
	}

	/**
	 * A vanilla wire one step up, diagonally from a Wire (on a stone block,
	 * with air above the Wire), reads it (power - 1). The Wire notifies its
	 * neighbours' neighbours, so the stepped wire hears it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void steppedVanillaWireReadsTheWire(GameTestHelper h) {
		floor(h);
		h.setBlock(3, 1, 3, Blocks.STONE);
		h.setBlock(3, 2, 3, Blocks.REDSTONE_WIRE);
		h.setBlock(2, 1, 3, WIRE);
		h.setBlock(1, 1, 3, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			check(h, power(h, 2, 1, 3) == 15, "the Wire should be 15, is " + power(h, 2, 1, 3));
			check(h, power(h, 3, 2, 3) == 14, "the stepped vanilla wire should be 14, is " + power(h, 3, 2, 3));
			h.succeed();
		});
	}

	/** A powered Wire works the devices touching it: a repeater (into a lamp), a piston and a door. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void worksDevicesOnItsFaces(GameTestHelper h) {
		floor(h);
		h.setBlock(5, 1, 3, Blocks.REDSTONE_LAMP);
		h.setBlock(4, 1, 3, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST));
		h.setBlock(3, 1, 4, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.SOUTH));
		h.setBlock(3, 1, 2, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		h.setBlock(3, 2, 2, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		h.setBlock(3, 1, 3, WIRE);
		h.setBlock(2, 1, 3, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(10, () -> {
			check(h, lit(h, 5, 1, 3), "the repeater fed by the Wire should light its lamp");
			check(h, h.getBlockState(new BlockPos(3, 1, 4)).getValue(PistonBaseBlock.EXTENDED), "the piston touching the Wire should extend");
			check(h, h.getBlockState(new BlockPos(3, 1, 2)).getValue(DoorBlock.OPEN), "the door touching the Wire should open");
			h.succeed();
		});
	}

	/** Non-block sources feed a Wire: a lever and a repeater. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void fedByALeverAndARepeater(GameTestHelper h) {
		floor(h);
		h.setBlock(6, 1, 2, Blocks.REDSTONE_LAMP);
		h.setBlock(5, 1, 2, WIRE);
		h.setBlock(4, 1, 2, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.POWERED, true));
		// Repeater from x = 3 (input) to x = 5 (output): FACING is the input side.
		h.setBlock(6, 1, 5, Blocks.REDSTONE_LAMP);
		h.setBlock(5, 1, 5, WIRE);
		h.setBlock(4, 1, 5, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST));
		h.setBlock(3, 1, 5, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(10, () -> {
			check(h, power(h, 5, 1, 2) == 15 && lit(h, 6, 1, 2), "the lever should power the Wire (15) and its lamp, Wire is " + power(h, 5, 1, 2));
			check(h, power(h, 5, 1, 5) == 15 && lit(h, 6, 1, 5), "the repeater should power the Wire (15) and its lamp, Wire is " + power(h, 5, 1, 5));
			h.succeed();
		});
	}

	/** Dummy Block + redstone dust + redstone torch; Dummy Block + redstone dust still makes the Dust. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void recipes(GameTestHelper h) {
		Optional<ItemStack> wire = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.REDSTONE, Items.REDSTONE_TORCH);
		Optional<ItemStack> dust = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.REDSTONE);
		check(h, wire.isPresent() && wire.get().is(WIRE.asItem()), "Dummy Block + redstone + torch should make the Wire, got " + wire);
		check(h, dust.isPresent() && dust.get().is(ModBlocks.SECRET_REDSTONE.asItem()), "Dummy Block + redstone should still make the Dust, got " + dust);
		h.succeed();
	}
}
