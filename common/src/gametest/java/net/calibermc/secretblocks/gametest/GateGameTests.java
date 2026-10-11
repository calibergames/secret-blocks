package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.SecretGate;
import net.calibermc.secretblocks.blocks.SecretGate.Line;
import net.calibermc.secretblocks.blocks.SecretGate.Spot;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretGateSegmentBlockEntity;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Gate: a powered gate builds up to 9 segments in the direction it
 * faces, copying its disguise; gates its line meets open too, one tick per
 * hop. Layout: stone floor at y = 0. Lines that must reach their full length
 * point up, so they leave the test area through the top, not into a
 * neighbouring test; what is left there removes itself once the gate is gone.
 */
public class GateGameTests {

	private static final Block GATE = ModBlocks.SECRET_GATE;
	private static final Block SEGMENT = ModBlocks.SECRET_GATE_SEGMENT;

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	/** A gate at {@code rel} facing {@code facing}, disguised as {@code disguise}. */
	private static void gate(GameTestHelper h, BlockPos rel, Direction facing, Block disguise) {
		h.setBlock(rel, GATE.defaultBlockState().setValue(SecretGate.FACING, facing));
		secret(h, rel).setDisguise(all(disguise));
	}

	private static boolean open(GameTestHelper h, BlockPos rel) {
		return h.getBlockState(rel).getValue(SecretGate.OPEN);
	}

	/** Checks positions 1..{@code length} along the line are the gate's segments, disguised as {@code disguise} (if given). */
	private static void line(GameTestHelper h, BlockPos gate, Direction facing, int length, Block disguise) {
		BlockPos gateAbs = h.absolutePos(gate);
		for (int i = 1; i <= length; i++) {
			BlockPos pos = gate.relative(facing, i);
			checkBlock(h, pos, SEGMENT);
			SecretGateSegmentBlockEntity segment = (SecretGateSegmentBlockEntity) h.getBlockEntity(pos);
			check(h, gateAbs.equals(segment.getOwner()), "segment " + i + " should belong to the gate at " + gateAbs + ", belongs to " + segment.getOwner());
			if (disguise != null) {
				check(h, segment.getState(Direction.NORTH).is(disguise), "segment " + i + " should be disguised as " + disguise + ", is " + segment.getState(Direction.NORTH));
			}
		}
	}

	private static void noSegments(GameTestHelper h, BlockPos gate, Direction facing, int length) {
		for (int i = 1; i <= length; i++) {
			check(h, !h.getBlockState(gate.relative(facing, i)).is(SEGMENT), "position " + i + " of the gate at " + gate + " should hold no segment");
		}
	}

	private static List<ItemEntity> items(GameTestHelper h) {
		// Wider than the area: a dropped item can fly a little way out of it.
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(8, 12, 8))).inflate(3));
	}

	/** Placed through the item it faces the side it was placed on and copies the clicked block, as other secret blocks do. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void facesTheSideItWasPlacedOn(GameTestHelper h) {
		floor(h);
		h.setBlock(new BlockPos(5, 1, 3), Blocks.BRICKS);
		place(h, GATE.asItem(), new Vec3(2.5, 1, 5.5), new BlockPos(2, 0, 3), Direction.UP, new Vec3(2.5, 1.0, 3.5));
		place(h, GATE.asItem(), new Vec3(2.5, 1, 6.5), new BlockPos(5, 1, 3), Direction.WEST, new Vec3(5.0, 1.5, 3.5));
		checkBlock(h, new BlockPos(2, 1, 3), GATE);
		checkBlock(h, new BlockPos(4, 1, 3), GATE);
		check(h, h.getBlockState(new BlockPos(2, 1, 3)).getValue(SecretGate.FACING) == Direction.UP, "placed on a floor it should face up");
		check(h, h.getBlockState(new BlockPos(4, 1, 3)).getValue(SecretGate.FACING) == Direction.WEST, "placed on a wall's west face it should face west");
		checkFaces(h, new BlockPos(2, 1, 3), Blocks.STONE);
		checkFaces(h, new BlockPos(4, 1, 3), Blocks.BRICKS);
		h.succeed();
	}

	/**
	 * Powered, it builds 9 segments disguised as itself, and no tenth;
	 * unpowered, it removes them, and nothing drops.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void buildsNineSegmentsAndRemovesThem(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(3, 1, 3);
		gate(h, gate, Direction.UP, Blocks.BRICKS);
		h.runAfterDelay(3, () -> {
			noSegments(h, gate, Direction.UP, 9);
			h.setBlock(gate.west(), Blocks.REDSTONE_BLOCK);
		});
		h.runAfterDelay(8, () -> {
			check(h, open(h, gate) && h.getBlockState(gate).getValue(SecretGate.POWERED), "the powered gate should be open");
			line(h, gate, Direction.UP, 9, Blocks.BRICKS);
			checkBlock(h, gate.above(10), Blocks.AIR);
			h.setBlock(gate.west(), Blocks.AIR);
		});
		h.runAfterDelay(13, () -> {
			check(h, !open(h, gate), "unpowered, the gate should be closed");
			for (int i = 1; i <= 10; i++) {
				checkBlock(h, gate.above(i), Blocks.AIR);
			}
			check(h, items(h).isEmpty(), "nothing should drop, dropped " + items(h).stream().map(e -> e.getItem().toString()).toList());
			h.succeed();
		});
	}

	/**
	 * It stops at the first block that is not replaceable, which stays.
	 * Replaceable blocks (grass, water) are replaced by segments, and are gone
	 * when the gate closes.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void stopsAtABlockAndReplacesReplaceableOnes(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(0, 1, 3);
		gate(h, gate, Direction.EAST, Blocks.BRICKS);
		h.setBlock(new BlockPos(1, 1, 3), Blocks.GRASS);
		h.setBlock(new BlockPos(2, 1, 3), Blocks.WATER);
		h.setBlock(new BlockPos(5, 1, 3), Blocks.STONE);
		h.setBlock(gate.north(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			line(h, gate, Direction.EAST, 4, Blocks.BRICKS);
			checkBlock(h, new BlockPos(5, 1, 3), Blocks.STONE);
			h.setBlock(gate.north(), Blocks.AIR);
		});
		h.runAfterDelay(10, () -> {
			for (int x = 1; x <= 4; x++) {
				checkBlock(h, new BlockPos(x, 1, 3), Blocks.AIR);
			}
			checkBlock(h, new BlockPos(5, 1, 3), Blocks.STONE);
			h.succeed();
		});
	}

	/** A block exactly at the ninth position stops the line at eight and stays. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBlockAtTheNinthPositionStaysAndStopsTheLine(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(3, 1, 3);
		gate(h, gate, Direction.UP, Blocks.BRICKS);
		h.setBlock(gate.above(9), Blocks.STONE);
		h.setBlock(gate.west(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			line(h, gate, Direction.UP, 8, Blocks.BRICKS);
			checkBlock(h, gate.above(9), Blocks.STONE);
			h.setBlock(gate.west(), Blocks.AIR);
		});
		h.runAfterDelay(10, () -> {
			noSegments(h, gate, Direction.UP, 8);
			checkBlock(h, gate.above(9), Blocks.STONE);
			h.setBlock(gate.above(9), Blocks.AIR);
			h.succeed();
		});
	}

	/** Its line stops at another gate's segment, and closing removes only its own segments. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void removesOnlyItsOwnSegments(GameTestHelper h) {
		floor(h);
		BlockPos up = new BlockPos(3, 1, 3);
		BlockPos east = new BlockPos(0, 3, 3);
		gate(h, up, Direction.UP, Blocks.BRICKS);
		gate(h, east, Direction.EAST, Blocks.OAK_PLANKS);
		h.setBlock(east.below(), Blocks.STONE);
		h.setBlock(up.west(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(4, () -> h.setBlock(east.above(), Blocks.REDSTONE_BLOCK));
		h.runAfterDelay(9, () -> {
			line(h, up, Direction.UP, 9, Blocks.BRICKS);
			// East along y = 3: x = 1, 2, then the upward line's segment at x = 3 stops it.
			line(h, east, Direction.EAST, 2, Blocks.OAK_PLANKS);
			h.setBlock(east.above(), Blocks.AIR);
		});
		h.runAfterDelay(14, () -> {
			noSegments(h, east, Direction.EAST, 2);
			line(h, up, Direction.UP, 9, Blocks.BRICKS);
			h.setBlock(up.west(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * A line that ends at a gate opens it, one tick later; closing closes it
	 * again, and its segments go too.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void opensAndClosesTheGateItMeets(GameTestHelper h) {
		floor(h);
		BlockPos first = new BlockPos(0, 1, 3);
		BlockPos second = new BlockPos(4, 1, 3);
		gate(h, first, Direction.EAST, Blocks.BRICKS);
		gate(h, second, Direction.UP, Blocks.OAK_PLANKS);
		h.setBlock(first.north(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(8, () -> {
			line(h, first, Direction.EAST, 3, Blocks.BRICKS);
			check(h, open(h, second) && !h.getBlockState(second).getValue(SecretGate.POWERED), "the gate met should be open, not powered");
			line(h, second, Direction.UP, 9, Blocks.OAK_PLANKS);
			h.setBlock(first.north(), Blocks.AIR);
		});
		h.runAfterDelay(14, () -> {
			check(h, !open(h, first) && !open(h, second), "both should be closed");
			noSegments(h, first, Direction.EAST, 3);
			noSegments(h, second, Direction.UP, 9);
			h.succeed();
		});
	}

	/** A gate beside the line (across it) opens too, as in the original. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void opensTheGatesBesideItsLine(GameTestHelper h) {
		floor(h);
		BlockPos first = new BlockPos(0, 1, 3);
		BlockPos beside = new BlockPos(3, 1, 4);
		gate(h, first, Direction.EAST, Blocks.BRICKS);
		gate(h, beside, Direction.UP, Blocks.OAK_PLANKS);
		h.setBlock(new BlockPos(6, 1, 3), Blocks.STONE);
		h.setBlock(first.north(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(8, () -> {
			line(h, first, Direction.EAST, 5, Blocks.BRICKS);
			check(h, open(h, beside), "the gate beside the line should be open");
			line(h, beside, Direction.UP, 9, Blocks.OAK_PLANKS);
			h.setBlock(first.north(), Blocks.AIR);
		});
		h.runAfterDelay(14, () -> {
			check(h, !open(h, beside), "the gate beside should close again");
			noSegments(h, beside, Direction.UP, 9);
			h.succeed();
		});
	}

	/**
	 * Gates facing each other cannot loop: nothing is left waiting to happen once
	 * they settle, whether a gap lies between them or they touch. (The second
	 * gate of the pair with a gap is blocked by the first one's segments and
	 * looks again once a second, so only its partner is checked for that.)
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void gatesFacingEachOtherSettle(GameTestHelper h) {
		floor(h);
		BlockPos[][] pairs = {{new BlockPos(0, 1, 1), new BlockPos(5, 1, 1)}, {new BlockPos(2, 1, 5), new BlockPos(3, 1, 5)}};
		for (BlockPos[] pair : pairs) {
			gate(h, pair[0], Direction.EAST, Blocks.BRICKS);
			gate(h, pair[1], Direction.WEST, Blocks.OAK_PLANKS);
			h.setBlock(pair[0].below(), Blocks.REDSTONE_BLOCK);
		}
		BlockPos[] settled = {pairs[0][0], pairs[1][0], pairs[1][1]};
		h.runAfterDelay(15, () -> {
			line(h, pairs[0][0], Direction.EAST, 4, Blocks.BRICKS);
			for (BlockPos[] pair : pairs) {
				check(h, open(h, pair[0]) && open(h, pair[1]), "both gates of the pair at " + pair[0] + " should be open");
			}
			for (BlockPos pos : settled) {
				check(h, !h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(pos), GATE), "the gate at " + pos + " should have nothing left to do");
			}
			// The second gate opened, but its first position is the first gate's segment (checked above): it built nothing.
			for (BlockPos[] pair : pairs) {
				h.setBlock(pair[0].below(), Blocks.STONE);
			}
		});
		h.runAfterDelay(25, () -> {
			for (BlockPos[] pair : pairs) {
				check(h, !open(h, pair[0]) && !open(h, pair[1]), "both gates of the pair at " + pair[0] + " should be closed");
			}
			for (BlockPos pos : settled) {
				check(h, !h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(pos), GATE), "the gate at " + pos + " should have nothing left to do");
			}
			noSegments(h, pairs[0][0], Direction.EAST, 4);
			h.succeed();
		});
	}

	/** A gate just past the ninth segment opens too, as in the original. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aGateJustPastAFullLineOpens(GameTestHelper h) {
		floor(h);
		BlockPos first = new BlockPos(3, 1, 3);
		BlockPos past = first.above(10);
		gate(h, first, Direction.UP, Blocks.BRICKS);
		gate(h, past, Direction.EAST, Blocks.OAK_PLANKS);
		h.setBlock(past.east(), Blocks.STONE);
		h.setBlock(first.west(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(6, () -> {
			line(h, first, Direction.UP, 9, Blocks.BRICKS);
			check(h, open(h, past), "the gate just past the ninth segment should be open");
			h.setBlock(first.west(), Blocks.AIR);
		});
		h.runAfterDelay(12, () -> {
			check(h, !open(h, past), "it should close with the line");
			h.setBlock(past, Blocks.AIR);
			h.setBlock(past.east(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * A gate that is closed does nothing to the gates in front of it: placed, or
	 * broken, in front of a gate that another holds open, it leaves it open.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aClosedGateLeavesAHeldOpenGateAlone(GameTestHelper h) {
		floor(h);
		BlockPos opener = new BlockPos(0, 1, 3);
		BlockPos held = new BlockPos(4, 1, 3);
		gate(h, opener, Direction.EAST, Blocks.BRICKS);
		gate(h, held, Direction.UP, Blocks.OAK_PLANKS);
		h.setBlock(opener.north(), Blocks.REDSTONE_BLOCK);
		BlockPos closed = new BlockPos(4, 1, 6);
		h.runAfterDelay(6, () -> {
			check(h, open(h, held), "control: the gate is held open");
			// A closed gate facing it, two blocks away: placed by a player against the stone behind it.
			h.setBlock(closed.south(), Blocks.STONE);
			place(h, GATE.asItem(), new Vec3(6.5, 1, 6.5), closed.south(), Direction.NORTH, new Vec3(4.5, 1.5, 7.0));
			checkBlock(h, closed, GATE);
			check(h, h.getBlockState(closed).getValue(SecretGate.FACING) == Direction.NORTH, "control: the closed gate faces the held one");
		});
		h.runAfterDelay(10, () -> {
			check(h, open(h, held), "placing a closed gate in front of it should leave it open");
			h.getLevel().destroyBlock(h.absolutePos(closed), false);
		});
		h.runAfterDelay(14, () -> {
			check(h, open(h, held), "breaking a closed gate in front of it should leave it open");
			line(h, held, Direction.UP, 9, Blocks.OAK_PLANKS);
			h.setBlock(opener.north(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * A gap in the line when it closes (a segment just broken) does not leave
	 * the gates further on open.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aGapDoesNotLeaveTheChainOpen(GameTestHelper h) {
		floor(h);
		BlockPos first = new BlockPos(0, 1, 3);
		BlockPos end = new BlockPos(5, 1, 3);
		BlockPos beside = new BlockPos(3, 1, 4);
		gate(h, first, Direction.EAST, Blocks.BRICKS);
		gate(h, end, Direction.UP, Blocks.OAK_PLANKS);
		gate(h, beside, Direction.UP, Blocks.OAK_PLANKS);
		h.setBlock(first.north(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(6, () -> {
			check(h, open(h, end) && open(h, beside), "control: both gates are open");
			h.getLevel().destroyBlock(h.absolutePos(new BlockPos(2, 1, 3)), false);
			h.setBlock(first.north(), Blocks.AIR);
		});
		h.runAfterDelay(12, () -> {
			check(h, !open(h, end), "the gate at the end, past the gap, should close");
			check(h, !open(h, beside), "the gate beside the line, past the gap, should close");
			noSegments(h, first, Direction.EAST, 4);
			noSegments(h, end, Direction.UP, 9);
			noSegments(h, beside, Direction.UP, 9);
			h.succeed();
		});
	}

	/** Breaking one segment leaves the rest of the line standing, tick after tick, until the gap is built again. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void breakingOneSegmentLeavesTheRestStanding(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(0, 1, 3);
		gate(h, gate, Direction.EAST, Blocks.BRICKS);
		h.setBlock(new BlockPos(7, 1, 3), Blocks.STONE);
		h.setBlock(gate.north(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> h.getLevel().destroyBlock(h.absolutePos(new BlockPos(2, 1, 3)), false));
		for (int t = 6; t < 5 + SecretGate.RETRY_DELAY; t++) {
			h.runAfterDelay(t, () -> {
				for (int x = 3; x <= 6; x++) {
					checkBlock(h, new BlockPos(x, 1, 3), SEGMENT);
				}
			});
		}
		h.runAfterDelay(5 + SecretGate.RETRY_DELAY + 3, () -> {
			line(h, gate, Direction.EAST, 6, Blocks.BRICKS);
			h.setBlock(gate.north(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Two powered gates facing each other: one builds, the other is blocked by
	 * its segments. When the first is switched off, the second builds its own
	 * line within a second.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBlockedGateBuildsWhenTheOtherLineGoes(GameTestHelper h) {
		floor(h);
		BlockPos west = new BlockPos(0, 1, 3);
		BlockPos east = new BlockPos(5, 1, 3);
		gate(h, west, Direction.EAST, Blocks.BRICKS);
		gate(h, east, Direction.WEST, Blocks.OAK_PLANKS);
		h.setBlock(west.north(), Blocks.REDSTONE_BLOCK);
		h.setBlock(east.north(), Blocks.REDSTONE_BLOCK);
		BlockPos[] other = new BlockPos[1];
		h.runAfterDelay(6, () -> {
			BlockPos owner = ((SecretGateSegmentBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 3))).getOwner();
			boolean westBuilt = h.absolutePos(west).equals(owner);
			check(h, westBuilt || h.absolutePos(east).equals(owner), "one of the two should have built the line, owner " + owner);
			BlockPos builder = westBuilt ? west : east;
			other[0] = westBuilt ? east : west;
			h.setBlock(builder.north(), Blocks.AIR);
		});
		h.runAfterDelay(6 + SecretGate.RETRY_DELAY + 4, () -> {
			Direction facing = h.getBlockState(other[0]).getValue(SecretGate.FACING);
			line(h, other[0], facing, 4, null);
			h.setBlock(other[0].north(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * A gate beside a line that closes by its own power stays closed when the
	 * line's gate tries again (here paused by an armor stand, so it retries each
	 * second).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void aRetryDoesNotReopenAGateThatClosedItself(GameTestHelper h) {
		floor(h);
		BlockPos first = new BlockPos(0, 1, 3);
		BlockPos beside = new BlockPos(2, 1, 4);
		gate(h, first, Direction.EAST, Blocks.BRICKS);
		gate(h, beside, Direction.UP, Blocks.OAK_PLANKS);
		ArmorStand stand = EntityType.ARMOR_STAND.create(h.getLevel());
		Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 1, 3)));
		stand.moveTo(at.x, at.y, at.z);
		h.getLevel().addFreshEntity(stand);
		h.setBlock(first.north(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			line(h, first, Direction.EAST, 3, Blocks.BRICKS);
			check(h, open(h, beside), "control: the gate beside the line is open");
			h.setBlock(beside.south(), Blocks.REDSTONE_BLOCK);
		});
		h.runAfterDelay(8, () -> h.setBlock(beside.south(), Blocks.AIR));
		h.runAfterDelay(11, () -> check(h, !open(h, beside), "control: its own power off closed it"));
		h.runAfterDelay(11 + 2 * SecretGate.RETRY_DELAY, () -> {
			check(h, !open(h, beside), "the line's retries should leave it closed");
			noSegments(h, beside, Direction.UP, 9);
			stand.discard();
			h.setBlock(first.north(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Placed by a player beside powered redstone dust, it builds with the
	 * disguise the placement gives it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void placedBesidePoweredDustItBuildsWithItsDisguise(GameTestHelper h) {
		floor(h);
		h.setBlock(new BlockPos(1, 1, 3), Blocks.REDSTONE_BLOCK);
		h.setBlock(new BlockPos(2, 1, 3), Blocks.REDSTONE_WIRE);
		h.setBlock(new BlockPos(3, 0, 3), Blocks.BRICKS);
		place(h, GATE.asItem(), new Vec3(3.5, 1, 6.5), new BlockPos(3, 0, 3), Direction.UP, new Vec3(3.5, 1.0, 3.5));
		h.runAfterDelay(5, () -> {
			BlockPos gate = new BlockPos(3, 1, 3);
			check(h, h.getBlockState(gate).getValue(SecretGate.POWERED), "control: the dust powers the gate");
			checkFaces(h, gate, Blocks.BRICKS);
			line(h, gate, Direction.UP, 9, Blocks.BRICKS);
			h.setBlock(new BlockPos(1, 1, 3), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * A neighbour update that reaches a powered gate before its placement has
	 * given it a disguise (something reacting to the placement at once) does
	 * not build yet: it waits a tick, and the segments wear the disguise set
	 * in between.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aNeighbourUpdateBeforeTheDisguiseWaits(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(3, 1, 3);
		h.setBlock(gate.west(), Blocks.REDSTONE_BLOCK);
		h.setBlock(gate, GATE.defaultBlockState().setValue(SecretGate.FACING, Direction.UP));
		check(h, secret(h, gate).getState(Direction.NORTH).isAir(), "control: no disguise yet");
		BlockState state = h.getBlockState(gate);
		state.neighborChanged(h.getLevel(), h.absolutePos(gate), Blocks.REDSTONE_WIRE, h.absolutePos(gate.west()), false);
		checkBlock(h, gate.above(), Blocks.AIR);
		check(h, !h.getBlockState(gate).getValue(SecretGate.POWERED), "it should not act yet");
		// What the placement does next, in the same tick.
		secret(h, gate).setDisguise(all(Blocks.BRICKS));
		h.runAfterDelay(3, () -> {
			line(h, gate, Direction.UP, 9, Blocks.BRICKS);
			h.setBlock(gate.west(), Blocks.AIR);
			h.succeed();
		});
	}

	/** A gate that is powered itself stays open when the gate that opened it closes. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPoweredGateStaysOpenWhenTheChainCloses(GameTestHelper h) {
		floor(h);
		BlockPos first = new BlockPos(0, 1, 3);
		BlockPos second = new BlockPos(4, 1, 3);
		gate(h, first, Direction.EAST, Blocks.BRICKS);
		gate(h, second, Direction.UP, Blocks.OAK_PLANKS);
		h.setBlock(first.north(), Blocks.REDSTONE_BLOCK);
		h.setBlock(second.south(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(8, () -> h.setBlock(first.north(), Blocks.AIR));
		h.runAfterDelay(14, () -> {
			check(h, !open(h, first), "the first gate should be closed");
			check(h, open(h, second), "the powered second gate should stay open");
			line(h, second, Direction.UP, 9, Blocks.OAK_PLANKS);
			h.setBlock(second.south(), Blocks.AIR);
			h.succeed();
		});
	}

	/** Breaking an open gate removes its segments and closes the gates it opened. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void breakingTheGateRemovesItsLine(GameTestHelper h) {
		floor(h);
		BlockPos first = new BlockPos(0, 1, 3);
		BlockPos second = new BlockPos(4, 1, 3);
		gate(h, first, Direction.EAST, Blocks.BRICKS);
		gate(h, second, Direction.UP, Blocks.OAK_PLANKS);
		h.setBlock(first.north(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(8, () -> {
			line(h, second, Direction.UP, 9, Blocks.OAK_PLANKS);
			h.getLevel().destroyBlock(h.absolutePos(first), true);
		});
		h.runAfterDelay(14, () -> {
			noSegments(h, first, Direction.EAST, 3);
			check(h, !open(h, second), "the gate it opened should close");
			noSegments(h, second, Direction.UP, 9);
			List<ItemEntity> drops = items(h);
			check(h, drops.size() == 1 && drops.get(0).getItem().is(GATE.asItem()), "only the gate itself should drop, dropped " + drops.stream().map(e -> e.getItem().toString()).toList());
			drops.forEach(ItemEntity::discard);
			h.succeed();
		});
	}

	/**
	 * An entity in the way (one a player could not place a block into) pauses
	 * the line there; it carries on within a second of the entity leaving.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void anEntityInTheWayPausesTheLine(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(0, 1, 3);
		gate(h, gate, Direction.EAST, Blocks.BRICKS);
		h.setBlock(new BlockPos(6, 1, 3), Blocks.STONE);
		ArmorStand stand = EntityType.ARMOR_STAND.create(h.getLevel());
		Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 1, 3)));
		stand.moveTo(at.x, at.y, at.z);
		h.getLevel().addFreshEntity(stand);
		h.setBlock(gate.north(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(6, () -> {
			line(h, gate, Direction.EAST, 2, Blocks.BRICKS);
			for (int x = 3; x <= 5; x++) {
				checkBlock(h, new BlockPos(x, 1, 3), Blocks.AIR);
			}
			check(h, !stand.isRemoved() && stand.isAlive(), "the armor stand should not be harmed");
			stand.discard();
		});
		h.runAfterDelay(6 + SecretGate.RETRY_DELAY + 3, () -> {
			line(h, gate, Direction.EAST, 5, Blocks.BRICKS);
			h.setBlock(gate.north(), Blocks.AIR);
			h.succeed();
		});
	}

	/** A segment broken by a player while the gate is open drops nothing and is built again a second later. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBrokenSegmentIsRebuilt(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(0, 1, 3);
		gate(h, gate, Direction.EAST, Blocks.BRICKS);
		h.setBlock(new BlockPos(6, 1, 3), Blocks.STONE);
		h.setBlock(gate.north(), Blocks.REDSTONE_BLOCK);
		BlockPos broken = new BlockPos(3, 1, 3);
		h.runAfterDelay(5, () -> {
			line(h, gate, Direction.EAST, 5, Blocks.BRICKS);
			ServerPlayer player = player(h, new Vec3(3.5, 1, 6.5), new Vec3(3.5, 1.5, 3.5));
			player.setGameMode(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
			check(h, player.gameMode.destroyBlock(h.absolutePos(broken)), "the player should break the segment");
			checkBlock(h, broken, Blocks.AIR);
		});
		h.runAfterDelay(5 + SecretGate.RETRY_DELAY + 3, () -> {
			line(h, gate, Direction.EAST, 5, Blocks.BRICKS);
			check(h, items(h).isEmpty(), "the broken segment should drop nothing");
			h.setBlock(gate.north(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Orphans clean themselves up: with the gate closed behind their back (its
	 * state set directly, as a crash or an edit could leave it), its segments go
	 * on their next check.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void segmentsOfAClosedGateRemoveThemselves(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(3, 1, 3);
		gate(h, gate, Direction.UP, Blocks.BRICKS);
		h.setBlock(gate.west(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			line(h, gate, Direction.UP, 9, Blocks.BRICKS);
			// Closed without its own code running: no neighbour updates, no tick.
			h.getLevel().setBlock(h.absolutePos(gate), h.getBlockState(gate).setValue(SecretGate.OPEN, false).setValue(SecretGate.POWERED, false),
					Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			h.setBlock(gate.west(), Blocks.STONE);
			h.getLevel().getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(h.absolutePos(gate)));
		});
		h.runAfterDelay(5 + SecretGateSegmentBlockEntity.CHECK_INTERVAL + 5, () -> {
			noSegments(h, gate, Direction.UP, 9);
			h.succeed();
		});
	}

	/**
	 * A segment copied onto the gate's own line but past the block that stops it
	 * (by {@code /clone}) is not one the gate built: it goes.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aCopyPastTheObstructionIsRemoved(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(0, 1, 3);
		gate(h, gate, Direction.EAST, Blocks.BRICKS);
		h.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		h.setBlock(gate.north(), Blocks.REDSTONE_BLOCK);
		BlockPos copy = new BlockPos(5, 1, 3);
		h.runAfterDelay(5, () -> {
			line(h, gate, Direction.EAST, 2, Blocks.BRICKS);
			CommandSourceStack source = h.getLevel().getServer().createCommandSourceStack().withSuppressedOutput().withLevel(h.getLevel());
			BlockPos from = h.absolutePos(new BlockPos(1, 1, 3));
			BlockPos to = h.absolutePos(copy);
			String at = from.getX() + " " + from.getY() + " " + from.getZ();
			h.getLevel().getServer().getCommands().performPrefixedCommand(source, "clone " + at + " " + at + " " + to.getX() + " " + to.getY() + " " + to.getZ());
			checkBlock(h, copy, SEGMENT);
		});
		h.runAfterDelay(9, () -> {
			checkBlock(h, copy, Blocks.AIR);
			line(h, gate, Direction.EAST, 2, Blocks.BRICKS);
			h.setBlock(gate.north(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Saved and loaded (block entities rebuilt from their saved data, as when the
	 * chunk loads), the gate keeps its disguise and its segments stay, owned and
	 * disguised.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void disguiseAndSegmentsSurviveSaveAndLoad(GameTestHelper h) {
		floor(h);
		BlockPos gate = new BlockPos(3, 1, 3);
		gate(h, gate, Direction.UP, Blocks.BRICKS);
		h.setBlock(gate.west(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			for (int i = 0; i <= 9; i++) {
				BlockPos pos = gate.above(i);
				CompoundTag saved = h.getBlockEntity(pos).saveWithFullMetadata();
				h.getLevel().setBlockEntity(BlockEntity.loadStatic(h.absolutePos(pos), h.getBlockState(pos), saved));
			}
		});
		h.runAfterDelay(5 + 2 * SecretGateSegmentBlockEntity.CHECK_INTERVAL, () -> {
			check(h, h.getBlockEntity(gate) instanceof SecretBlockEntity, "the gate should have its block entity");
			checkFaces(h, gate, Blocks.BRICKS);
			line(h, gate, Direction.UP, 9, Blocks.BRICKS);
			h.setBlock(gate.west(), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * Placed by a player beside a redstone block, it builds once the placement
	 * has given it its disguise, so the segments wear it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void placedPoweredItBuildsWithItsDisguise(GameTestHelper h) {
		floor(h);
		h.setBlock(new BlockPos(2, 1, 3), Blocks.REDSTONE_BLOCK);
		h.setBlock(new BlockPos(3, 0, 3), Blocks.BRICKS);
		place(h, GATE.asItem(), new Vec3(3.5, 1, 6.5), new BlockPos(3, 0, 3), Direction.UP, new Vec3(3.5, 1.0, 3.5));
		h.runAfterDelay(5, () -> {
			BlockPos gate = new BlockPos(3, 1, 3);
			checkFaces(h, gate, Blocks.BRICKS);
			line(h, gate, Direction.UP, 9, Blocks.BRICKS);
			h.setBlock(new BlockPos(2, 1, 3), Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * The line plan on its own: it builds over free positions and its own
	 * segments, and stops at an entity, a gate, a block or a position that is not
	 * loaded (a game test cannot unload a chunk beside a running test).
	 * {@code spotAt} reports a far position as not loaded without loading it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theLinePlan(GameTestHelper h) {
		check(h, SecretGate.plan(i -> Spot.FREE).equals(new Line(9, null)), "all free: 9, got " + SecretGate.plan(i -> Spot.FREE));
		check(h, SecretGate.plan(i -> i % 2 == 0 ? Spot.OWN : Spot.FREE).equals(new Line(9, null)), "its own segments count as built");
		check(h, SecretGate.plan(i -> i == 10 ? Spot.GATE : Spot.FREE).equals(new Line(9, Spot.GATE)), "a gate just past a full line should be met");
		check(h, SecretGate.plan(i -> i == 10 ? Spot.BLOCKED : Spot.FREE).equals(new Line(9, null)), "a block just past a full line does not matter");
		for (Spot stop : new Spot[] {Spot.OCCUPIED, Spot.GATE, Spot.UNLOADED, Spot.FOREIGN, Spot.BLOCKED}) {
			Line line = SecretGate.plan(i -> i < 4 ? Spot.FREE : stop);
			check(h, line.equals(new Line(3, stop)), stop + " at 4 should stop the line at 3, got " + line);
		}
		BlockPos far = h.absolutePos(BlockPos.ZERO).offset(30000, 0, 0);
		check(h, SecretGate.spotAt(h.getLevel(), far, h.absolutePos(BlockPos.ZERO)) == Spot.UNLOADED, "a far position should not be loaded");
		check(h, !h.getLevel().isLoaded(far), "looking at it must not load it");
		h.succeed();
	}

	/** Wool-like, a conductor, a full block with the shared block entity, no counterpart; the recipe. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void propertiesAndRecipe(GameTestHelper h) {
		floor(h);
		BlockPos rel = new BlockPos(2, 1, 2);
		h.setBlock(rel, GATE);
		BlockState state = h.getBlockState(rel);
		BlockPos abs = h.absolutePos(rel);
		check(h, state.getDestroySpeed(h.getLevel(), abs) == Blocks.WHITE_WOOL.defaultBlockState().getDestroySpeed(h.getLevel(), abs),
				"it should be as hard as wool (0.8), is " + state.getDestroySpeed(h.getLevel(), abs));
		check(h, !state.isRedstoneConductor(h.getLevel(), abs) && state.isCollisionShapeFullBlock(h.getLevel(), abs), "it should be a full block and not a conductor");
		check(h, h.getBlockEntity(rel) instanceof SecretBlockEntity, "it should have the shared block entity");
		check(h, Counterparts.hasNone(GATE), "it should have no counterpart");
		Optional<ItemStack> result = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.PISTON, Items.REDSTONE);
		check(h, result.isPresent() && result.get().is(GATE.asItem()) && result.get().getCount() == 1,
				"Dummy Block + piston + redstone should craft one Secret Gate, got " + result);
		h.succeed();
	}
}
