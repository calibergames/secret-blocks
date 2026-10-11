package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Secret Redstone Dust behaves like redstone wire in circuits. Each line is
 * built twice: at z = 2 with the secret/vanilla mix under test, at z = 5
 * with vanilla wire only (the control), and every wire's power must match
 * the control's.
 */
public class RedstoneGameTests {

	private static final int SECRET_ROW = 2;
	private static final int CONTROL_ROW = 5;

	/**
	 * Build a line along x: optionally a lamp at the far end, the given wires
	 * from x = 1, and last the redstone block at x = 0, so every piece sees the
	 * power arrive ({@code setBlock} does not make a lamp check its own power).
	 */
	private static List<BlockPos> line(GameTestHelper h, int z, Block[] wires, boolean lamp) {
		if (lamp) {
			h.setBlock(new BlockPos(1 + wires.length, 1, z), Blocks.REDSTONE_LAMP);
		}
		List<BlockPos> positions = new ArrayList<>();
		for (int i = 0; i < wires.length; i++) {
			BlockPos pos = new BlockPos(1 + i, 1, z);
			h.setBlock(pos, wires[i]);
			positions.add(pos);
		}
		h.setBlock(new BlockPos(0, 1, z), Blocks.REDSTONE_BLOCK);
		return positions;
	}

	private static int power(GameTestHelper h, BlockPos pos) {
		return h.getBlockState(pos).getValue(RedStoneWireBlock.POWER);
	}

	private static boolean lit(GameTestHelper h, BlockPos pos) {
		return h.getBlockState(pos).getValue(RedstoneLampBlock.LIT);
	}

	private static Block[] vanillaOnly(int n) {
		Block[] wires = new Block[n];
		java.util.Arrays.fill(wires, Blocks.REDSTONE_WIRE);
		return wires;
	}

	/** Every wire in the line has the same power as the vanilla control at the same distance. */
	private static void checkPowersMatchControl(GameTestHelper h, List<BlockPos> line, List<BlockPos> control, String when) {
		for (int i = 0; i < line.size(); i++) {
			int got = power(h, line.get(i));
			int want = power(h, control.get(i));
			check(h, got == want, when + ": wire " + (i + 1) + " (" + h.getBlockState(line.get(i)).getBlock()
					+ ") has power " + got + ", vanilla control has " + want);
		}
	}

	/** A lamp right next to Secret Redstone Dust lights, as it would next to vanilla wire. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lampNextToSecretRedstoneLights(GameTestHelper h) {
		floor(h);
		line(h, SECRET_ROW, new Block[] {ModBlocks.SECRET_REDSTONE}, true);
		line(h, CONTROL_ROW, vanillaOnly(1), true);
		h.succeedWhen(() -> {
			check(h, lit(h, new BlockPos(2, 1, CONTROL_ROW)), "control: the lamp after vanilla wire should light");
			check(h, lit(h, new BlockPos(2, 1, SECRET_ROW)), "the lamp next to Secret Redstone Dust should light");
		});
	}

	/**
	 * The same, as a player builds it: the wire is already powered when a
	 * player places the lamp beside it (in 1.2.0 a lamp set beside it in game
	 * stayed off).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lampPlacedByAPlayerNextToPoweredSecretRedstoneLights(GameTestHelper h) {
		floor(h);
		line(h, SECRET_ROW, new Block[] {ModBlocks.SECRET_REDSTONE}, false);
		line(h, CONTROL_ROW, vanillaOnly(1), false);
		h.runAfterDelay(5, () -> {
			for (int z : new int[] {SECRET_ROW, CONTROL_ROW}) {
				place(h, Items.REDSTONE_LAMP, new Vec3(4.5, 1, z + 0.5), new BlockPos(2, 0, z), Direction.UP, new Vec3(2.5, 1.0, z + 0.5));
			}
		});
		h.runAfterDelay(10, () -> {
			checkBlock(h, new BlockPos(2, 1, CONTROL_ROW), Blocks.REDSTONE_LAMP);
			checkBlock(h, new BlockPos(2, 1, SECRET_ROW), Blocks.REDSTONE_LAMP);
			check(h, lit(h, new BlockPos(2, 1, CONTROL_ROW)), "control: a lamp placed next to powered vanilla wire should light");
			check(h, lit(h, new BlockPos(2, 1, SECRET_ROW)), "a lamp placed next to powered Secret Redstone Dust should light");
			h.succeed();
		});
	}

	/** Step by step: a player places the wire between a redstone block and a lamp already there. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void secretRedstonePlacedByAPlayerLightsTheLamp(GameTestHelper h) {
		floor(h);
		for (int z : new int[] {SECRET_ROW, CONTROL_ROW}) {
			h.setBlock(new BlockPos(0, 1, z), Blocks.REDSTONE_BLOCK);
			h.setBlock(new BlockPos(2, 1, z), Blocks.REDSTONE_LAMP);
		}
		place(h, ModBlocks.SECRET_REDSTONE.asItem(), new Vec3(1.5, 1, SECRET_ROW + 2.5), new BlockPos(1, 0, SECRET_ROW), Direction.UP,
				new Vec3(1.5, 1.0, SECRET_ROW + 0.5));
		place(h, Items.REDSTONE, new Vec3(1.5, 1, CONTROL_ROW + 2.5), new BlockPos(1, 0, CONTROL_ROW), Direction.UP,
				new Vec3(1.5, 1.0, CONTROL_ROW + 0.5));
		h.runAfterDelay(5, () -> {
			checkBlock(h, new BlockPos(1, 1, SECRET_ROW), ModBlocks.SECRET_REDSTONE);
			checkBlock(h, new BlockPos(1, 1, CONTROL_ROW), Blocks.REDSTONE_WIRE);
			check(h, lit(h, new BlockPos(2, 1, CONTROL_ROW)), "control: the lamp should light once vanilla wire is placed");
			check(h, lit(h, new BlockPos(2, 1, SECRET_ROW)), "the lamp should light once Secret Redstone Dust is placed");
			h.succeed();
		});
	}

	/** The lamp is set (not placed) after the wire is powered; it ends like the control's. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lampSetNextToPoweredSecretRedstoneMatchesVanilla(GameTestHelper h) {
		floor(h);
		line(h, SECRET_ROW, new Block[] {ModBlocks.SECRET_REDSTONE}, false);
		line(h, CONTROL_ROW, vanillaOnly(1), false);
		h.runAfterDelay(5, () -> {
			h.setBlock(new BlockPos(2, 1, SECRET_ROW), Blocks.REDSTONE_LAMP);
			h.setBlock(new BlockPos(2, 1, CONTROL_ROW), Blocks.REDSTONE_LAMP);
		});
		h.runAfterDelay(10, () -> {
			boolean control = lit(h, new BlockPos(2, 1, CONTROL_ROW));
			boolean secret = lit(h, new BlockPos(2, 1, SECRET_ROW));
			check(h, secret == control, "lamp set next to Secret Redstone Dust lit=" + secret + ", next to vanilla wire lit=" + control);
			h.succeed();
		});
	}

	/** Power drops by 1 per wire across a mixed line, exactly as along vanilla wire. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void mixedLineLosesOnePerWire(GameTestHelper h) {
		floor(h);
		Block s = ModBlocks.SECRET_REDSTONE;
		Block v = Blocks.REDSTONE_WIRE;
		List<BlockPos> mixed = line(h, SECRET_ROW, new Block[] {s, v, s, s, v, s}, true);
		List<BlockPos> control = line(h, CONTROL_ROW, vanillaOnly(6), true);
		h.succeedWhen(() -> {
			check(h, power(h, control.get(0)) == 15 && power(h, control.get(5)) == 10, "control line should read 15 down to 10");
			checkPowersMatchControl(h, mixed, control, "powered");
			check(h, lit(h, new BlockPos(7, 1, SECRET_ROW)), "the lamp at the end of the mixed line should light");
		});
	}

	/** Removing the source turns the whole mixed line off (no latching). */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void mixedLineTurnsOffWhenSourceRemoved(GameTestHelper h) {
		floor(h);
		Block s = ModBlocks.SECRET_REDSTONE;
		Block v = Blocks.REDSTONE_WIRE;
		List<BlockPos> mixed = line(h, SECRET_ROW, new Block[] {s, v, s, v}, true);
		List<BlockPos> control = line(h, CONTROL_ROW, vanillaOnly(4), true);
		h.runAfterDelay(10, () -> {
			check(h, power(h, mixed.get(0)) == 15 && lit(h, new BlockPos(5, 1, SECRET_ROW)), "the mixed line should be powered and its lamp lit first");
			checkPowersMatchControl(h, mixed, control, "powered");
			h.setBlock(new BlockPos(0, 1, SECRET_ROW), Blocks.AIR);
			h.setBlock(new BlockPos(0, 1, CONTROL_ROW), Blocks.AIR);
		});
		h.runAfterDelay(30, () -> {
			for (BlockPos pos : mixed) {
				check(h, power(h, pos) == 0, "after removing the source, " + h.getBlockState(pos).getBlock() + " at x=" + pos.getX() + " should be 0, is " + power(h, pos));
			}
			checkPowersMatchControl(h, mixed, control, "unpowered");
			check(h, !lit(h, new BlockPos(5, 1, SECRET_ROW)), "the lamp should turn off");
			h.succeed();
		});
	}

	/** Like vanilla wire, Secret Redstone Dust is not a redstone conductor (a strongly powered block), though it is a full block. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void notARedstoneConductor(GameTestHelper h) {
		BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
		check(h, !ModBlocks.SECRET_REDSTONE.defaultBlockState().isRedstoneConductor(h.getLevel(), pos),
				"Secret Redstone Dust must not be a redstone conductor (vanilla wire is not)");
		check(h, !Blocks.REDSTONE_WIRE.defaultBlockState().isRedstoneConductor(h.getLevel(), pos), "control: vanilla wire is not a conductor");
		h.succeed();
	}

	/**
	 * A repeater feeds a straight line; a lamp beside the first wire, where the
	 * line does not point, stays off as it does beside vanilla wire (a
	 * conductor would pass the repeater's strong power into the lamp).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void lampBesideARepeaterFedLineMatchesVanilla(GameTestHelper h) {
		floor(h);
		for (int z : new int[] {SECRET_ROW, CONTROL_ROW}) {
			h.setBlock(new BlockPos(2, 1, z + 1), Blocks.REDSTONE_LAMP);
			h.setBlock(new BlockPos(2, 1, z), z == SECRET_ROW ? ModBlocks.SECRET_REDSTONE : Blocks.REDSTONE_WIRE);
			h.setBlock(new BlockPos(3, 1, z), Blocks.REDSTONE_WIRE);
			// A repeater's FACING is its input side: input from x = 0, output into x = 2.
			h.setBlock(new BlockPos(1, 1, z), Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST));
			h.setBlock(new BlockPos(0, 1, z), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(20, () -> {
			check(h, power(h, new BlockPos(2, 1, CONTROL_ROW)) == 15, "control: the repeater should power the first wire");
			check(h, power(h, new BlockPos(2, 1, SECRET_ROW)) == 15, "the repeater should power Secret Redstone Dust");
			boolean control = lit(h, new BlockPos(2, 1, CONTROL_ROW + 1));
			boolean secret = lit(h, new BlockPos(2, 1, SECRET_ROW + 1));
			check(h, !control, "control: a lamp beside vanilla wire where it does not point stays off");
			check(h, secret == control, "lamp beside Secret Redstone Dust lit=" + secret + ", beside vanilla wire lit=" + control);
			h.succeed();
		});
	}

	/**
	 * A comparator in subtract mode reads a wire at its side the same whether
	 * it is Secret Redstone Dust or vanilla wire (its back input 15 minus the side
	 * wire's 14). Vanilla reads wire there by its power level; Secret Redstone Dust
	 * gives the same value through its signal, so no mixin is needed.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void comparatorSideInputMatchesVanilla(GameTestHelper h) {
		floor(h);
		// A comparator's FACING is its input side: back input from x = 2, output toward x = 4.
		BlockState comparator = Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST)
				.setValue(ComparatorBlock.MODE, ComparatorMode.SUBTRACT);
		for (int z : new int[] {SECRET_ROW, CONTROL_ROW}) {
			h.setBlock(new BlockPos(3, 1, z), comparator);
			// A straight line along x past the comparator's south side.
			h.setBlock(new BlockPos(2, 1, z + 1), Blocks.REDSTONE_WIRE);
			h.setBlock(new BlockPos(3, 1, z + 1), z == SECRET_ROW ? ModBlocks.SECRET_REDSTONE : Blocks.REDSTONE_WIRE);
			h.setBlock(new BlockPos(4, 1, z + 1), Blocks.REDSTONE_WIRE);
			h.setBlock(new BlockPos(1, 1, z + 1), Blocks.REDSTONE_BLOCK);
			h.setBlock(new BlockPos(2, 1, z), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(10, () -> {
			int control = ((ComparatorBlockEntity) h.getBlockEntity(new BlockPos(3, 1, CONTROL_ROW))).getOutputSignal();
			int secret = ((ComparatorBlockEntity) h.getBlockEntity(new BlockPos(3, 1, SECRET_ROW))).getOutputSignal();
			check(h, control == 1, "control: 15 minus the side wire's 14 should be 1, is " + control);
			check(h, secret == control, "comparator beside Secret Redstone Dust outputs " + secret + ", beside vanilla wire " + control);
			h.succeed();
		});
	}

	/** Up a one-block step, vanilla wire and Secret Redstone Dust connect to each other (both ways) as two vanilla wires do. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void stepConnectsLikeVanilla(GameTestHelper h) {
		floor(h);
		// Row z: low wire at x = 1, a stone step at x = 2 with a wire on top. Secret Redstone Dust on top (z = 1)
		// or at the bottom (z = 3); the control (z = 5) is vanilla only.
		int[][] rows = {{1, 1}, {3, 0}, {5, -1}};
		for (int[] row : rows) {
			int z = row[0];
			h.setBlock(new BlockPos(2, 1, z), Blocks.STONE);
			h.setBlock(new BlockPos(1, 1, z), row[1] == 0 ? ModBlocks.SECRET_REDSTONE : Blocks.REDSTONE_WIRE);
			h.setBlock(new BlockPos(2, 2, z), row[1] == 1 ? ModBlocks.SECRET_REDSTONE : Blocks.REDSTONE_WIRE);
			h.setBlock(new BlockPos(0, 1, z), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(5, () -> {
			var low = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST);
			var high = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST);
			check(h, h.getBlockState(new BlockPos(1, 1, 5)).getValue(low) == RedstoneSide.UP, "control: the low wire should run up the step");
			check(h, h.getBlockState(new BlockPos(2, 2, 5)).getValue(high) == RedstoneSide.SIDE, "control: the high wire should connect down");
			check(h, h.getBlockState(new BlockPos(1, 1, 1)).getValue(low) == RedstoneSide.UP, "vanilla wire should run up to Secret Redstone Dust on the step");
			check(h, h.getBlockState(new BlockPos(2, 2, 1)).getValue(high) == RedstoneSide.SIDE, "Secret Redstone Dust on the step should connect down to vanilla wire");
			check(h, h.getBlockState(new BlockPos(1, 1, 3)).getValue(low) == RedstoneSide.UP, "Secret Redstone Dust should run up to vanilla wire on the step");
			check(h, h.getBlockState(new BlockPos(2, 2, 3)).getValue(high) == RedstoneSide.SIDE, "vanilla wire on the step should connect down to Secret Redstone Dust");
			check(h, power(h, new BlockPos(2, 2, 5)) == 14, "control: the wire on the step should have power 14, has " + power(h, new BlockPos(2, 2, 5)));
			for (int z : new int[] {1, 3}) {
				check(h, power(h, new BlockPos(2, 2, z)) == power(h, new BlockPos(2, 2, 5)),
						"the wire on the step at z=" + z + " has power " + power(h, new BlockPos(2, 2, z)) + ", control " + power(h, new BlockPos(2, 2, 5)));
			}
			h.succeed();
		});
	}

	/** A line of Secret Redstone Dust only: loses 1 per block and turns off when the source goes. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void secretOnlyLine(GameTestHelper h) {
		floor(h);
		Block s = ModBlocks.SECRET_REDSTONE;
		List<BlockPos> secret = line(h, SECRET_ROW, new Block[] {s, s, s, s}, true);
		List<BlockPos> control = line(h, CONTROL_ROW, vanillaOnly(4), true);
		h.runAfterDelay(10, () -> {
			check(h, power(h, secret.get(0)) == 15 && power(h, control.get(3)) == 12, "both lines should be powered first (15 down to 12)");
			checkPowersMatchControl(h, secret, control, "powered");
			h.setBlock(new BlockPos(0, 1, SECRET_ROW), Blocks.AIR);
			h.setBlock(new BlockPos(0, 1, CONTROL_ROW), Blocks.AIR);
		});
		h.runAfterDelay(30, () -> {
			checkPowersMatchControl(h, secret, control, "unpowered");
			h.succeed();
		});
	}
}
