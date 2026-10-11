package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.SecretGateSegmentBlockEntity;
import net.calibermc.secretblocks.gametest.support.TestBlocks;
import net.calibermc.secretblocks.gametest.support.TestGateOwner;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Gate segment and its ownership. A stand-in owner from the
 * game-test mod ({@link TestGateOwner}) takes the real gate's place and keeps
 * the segments east of it while powered. Layout: stone floor at y = 0; the
 * owner at {@link #OWNER}, its line along x.
 */
public class GateSegmentGameTests implements FabricGameTest {

	private static final Block SEGMENT = ModBlocks.SECRET_GATE_SEGMENT;
	private static final BlockPos OWNER = new BlockPos(1, 1, 3);
	/** Long enough for a segment's first check and one more. */
	private static final int TWO_CHECKS = SecretGateSegmentBlockEntity.CHECK_INTERVAL + 5;

	private static void owner(GameTestHelper h, boolean powered) {
		h.setBlock(OWNER, TestBlocks.TEST_GATE_OWNER.defaultBlockState().setValue(TestGateOwner.POWERED, powered));
	}

	/** A segment at {@code rel} owned by the block at the absolute position {@code ownerAbs}. */
	private static SecretGateSegmentBlockEntity segment(GameTestHelper h, BlockPos rel, BlockPos ownerAbs) {
		h.setBlock(rel, SEGMENT);
		SecretGateSegmentBlockEntity segment = (SecretGateSegmentBlockEntity) h.getBlockEntity(rel);
		segment.setOwner(ownerAbs);
		return segment;
	}

	private static List<ItemEntity> drops(GameTestHelper h) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(0, 0, 0)), h.absolutePos(new BlockPos(8, 4, 8))));
	}

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	private static int command(GameTestHelper h, String command) {
		CommandSourceStack source = h.getLevel().getServer().createCommandSourceStack().withSuppressedOutput().withLevel(h.getLevel());
		return h.getLevel().getServer().getCommands().performPrefixedCommand(source, command);
	}

	private static String at(GameTestHelper h, BlockPos rel) {
		BlockPos abs = h.absolutePos(rel);
		return abs.getX() + " " + abs.getY() + " " + abs.getZ();
	}

	/**
	 * No item, nothing dropped (broken by hand or by a survival player), and
	 * pick-block gives nothing (there is no item; the block also says so).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void noItemNoDropsNoPickBlock(GameTestHelper h) {
		floor(h);
		owner(h, true);
		check(h, SEGMENT.asItem() == Items.AIR, "the segment should have no item, has " + SEGMENT.asItem());
		check(h, !BuiltInRegistries.ITEM.containsKey(new ResourceLocation("secretblocks", "secret_gate_segment")), "no item should be registered for the segment");
		BlockPos first = OWNER.east();
		segment(h, first, h.absolutePos(OWNER));
		check(h, h.getBlockState(first).getBlock().getCloneItemStack(h.getLevel(), h.absolutePos(first), h.getBlockState(first)).isEmpty(),
				"pick-block should give nothing");
		h.getLevel().destroyBlock(h.absolutePos(first), true);
		checkBlock(h, first, Blocks.AIR);
		BlockPos second = OWNER.east(2);
		segment(h, second, h.absolutePos(OWNER));
		ServerPlayer player = player(h, new Vec3(3.5, 1, 6.5), new Vec3(3.5, 1.5, 3.5));
		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
		check(h, player.gameMode.destroyBlock(h.absolutePos(second)), "a survival player should be able to break a segment");
		checkBlock(h, second, Blocks.AIR);
		check(h, drops(h).isEmpty(), "a broken segment should drop nothing, dropped " + drops(h).stream().map(e -> e.getItem().toString()).toList());
		h.succeed();
	}

	/**
	 * What it is made of: wool-like toughness, a conductor, a full block
	 * with no light, no counterpart, and pistons can neither push nor break it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void properties(GameTestHelper h) {
		floor(h);
		owner(h, true);
		BlockPos rel = OWNER.east();
		segment(h, rel, h.absolutePos(OWNER));
		BlockState state = h.getBlockState(rel);
		BlockPos abs = h.absolutePos(rel);
		check(h, state.getDestroySpeed(h.getLevel(), abs) == Blocks.WHITE_WOOL.defaultBlockState().getDestroySpeed(h.getLevel(), abs),
				"it should be as hard as wool (0.8), is " + state.getDestroySpeed(h.getLevel(), abs));
		check(h, !state.isRedstoneConductor(h.getLevel(), abs), "it should not be a conductor");
		check(h, state.isCollisionShapeFullBlock(h.getLevel(), abs) && state.getLightEmission() == 0, "it should be a full block with no light");
		check(h, state.getPistonPushReaction() == PushReaction.BLOCK, "pistons should neither push nor break it, reaction " + state.getPistonPushReaction());
		check(h, Counterparts.hasNone(SEGMENT), "it should have no counterpart");
		h.succeed();
	}

	/**
	 * A piston facing a kept segment does not extend, and the segment stays; the
	 * same piston beside it, facing air, extends. (A vanilla piston never moves
	 * a block with a block entity, so this passes without the segment's own
	 * {@code PushReaction.BLOCK}; {@link #properties} checks that.)
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPistonCannotMoveIt(GameTestHelper h) {
		floor(h);
		owner(h, true);
		BlockPos rel = OWNER.east(3);
		segment(h, rel, h.absolutePos(OWNER));
		BlockPos piston = rel.south();
		BlockPos control = piston.east(2);
		for (BlockPos pos : new BlockPos[] {piston, control}) {
			h.setBlock(pos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH));
			h.setBlock(pos.south(), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(10, () -> {
			check(h, h.getBlockState(control).getValue(PistonBaseBlock.EXTENDED), "control: the piston facing air should extend");
			check(h, !h.getBlockState(piston).getValue(PistonBaseBlock.EXTENDED), "the piston should not extend into the segment");
			checkBlock(h, rel, SEGMENT);
			checkBlock(h, rel.north(), Blocks.AIR);
			h.succeed();
		});
	}

	/** Segments on a powered owner's line stay, check after check. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void keptWhileItsOwnerKeepsIt(GameTestHelper h) {
		floor(h);
		owner(h, true);
		for (int i = 1; i <= 3; i++) {
			segment(h, OWNER.east(i), h.absolutePos(OWNER));
		}
		h.runAfterDelay(2 * TWO_CHECKS, () -> {
			for (int i = 1; i <= 3; i++) {
				checkBlock(h, OWNER.east(i), SEGMENT);
			}
			h.succeed();
		});
	}

	/**
	 * Its owner gone, a segment removes itself on its next check, dropping
	 * nothing; so does a segment loaded from a save whose owner is gone.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void removedWhenItsOwnerIsGone(GameTestHelper h) {
		floor(h);
		owner(h, true);
		BlockPos kept = OWNER.east();
		BlockPos loadedRel = OWNER.east(2);
		segment(h, kept, h.absolutePos(OWNER));
		CompoundTag saved = segment(h, loadedRel, h.absolutePos(OWNER)).saveWithFullMetadata();
		h.runAfterDelay(TWO_CHECKS, () -> {
			checkBlock(h, kept, SEGMENT);
			h.setBlock(OWNER, Blocks.AIR);
			// As if its chunk were loaded now: a block entity from saved data, owner gone.
			BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(loadedRel), h.getBlockState(loadedRel), saved);
			h.getLevel().setBlockEntity(loaded);
		});
		h.runAfterDelay(TWO_CHECKS + 3, () -> checkBlock(h, loadedRel, Blocks.AIR));
		h.runAfterDelay(2 * TWO_CHECKS + 2, () -> {
			checkBlock(h, kept, Blocks.AIR);
			check(h, drops(h).isEmpty(), "a segment removing itself should drop nothing");
			h.succeed();
		});
	}

	/** Its owner unpowered, a segment removes itself; one placed beside an unpowered owner goes at once. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void removedWhenItsOwnerIsUnpowered(GameTestHelper h) {
		floor(h);
		owner(h, true);
		BlockPos rel = OWNER.east();
		segment(h, rel, h.absolutePos(OWNER));
		h.runAfterDelay(TWO_CHECKS, () -> {
			checkBlock(h, rel, SEGMENT);
			owner(h, false);
			segment(h, OWNER.east(2), h.absolutePos(OWNER));
		});
		h.runAfterDelay(TWO_CHECKS + 3, () -> checkBlock(h, OWNER.east(2), Blocks.AIR));
		h.runAfterDelay(2 * TWO_CHECKS + 2, () -> {
			checkBlock(h, rel, Blocks.AIR);
			h.succeed();
		});
	}

	/** A segment whose owner position holds some other block (here stone) removes itself. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void removedWhenItsOwnerIsNotAGate(GameTestHelper h) {
		floor(h);
		h.setBlock(OWNER, Blocks.STONE);
		segment(h, OWNER.east(), h.absolutePos(OWNER));
		h.runAfterDelay(3, () -> {
			checkBlock(h, OWNER.east(), Blocks.AIR);
			h.succeed();
		});
	}

	/** A segment placed by {@code /setblock}, with no owner, removes itself. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSegmentFromSetblockIsRemoved(GameTestHelper h) {
		floor(h);
		owner(h, true);
		BlockPos rel = OWNER.east();
		check(h, command(h, "setblock " + at(h, rel) + " secretblocks:secret_gate_segment") > 0, "control: /setblock should succeed");
		checkBlock(h, rel, SEGMENT);
		check(h, ((SecretGateSegmentBlockEntity) h.getBlockEntity(rel)).getOwner() == null, "a segment from /setblock has no owner");
		h.runAfterDelay(3, () -> {
			checkBlock(h, rel, Blocks.AIR);
			h.succeed();
		});
	}

	/**
	 * A segment copied by {@code /clone} off its owner's line removes itself
	 * (its owner does not keep a segment there); the original stays.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aClonedSegmentIsRemoved(GameTestHelper h) {
		floor(h);
		owner(h, true);
		BlockPos original = OWNER.east();
		BlockPos copy = original.south(2);
		segment(h, original, h.absolutePos(OWNER));
		check(h, command(h, "clone " + at(h, original) + " " + at(h, original) + " " + at(h, copy)) > 0, "control: /clone should succeed");
		checkBlock(h, copy, SEGMENT);
		check(h, h.absolutePos(OWNER).equals(((SecretGateSegmentBlockEntity) h.getBlockEntity(copy)).getOwner()), "control: the copy keeps the owner's position");
		h.runAfterDelay(TWO_CHECKS, () -> {
			checkBlock(h, copy, Blocks.AIR);
			checkBlock(h, original, SEGMENT);
			h.succeed();
		});
	}

	/**
	 * An owner that could never have built it (here 20000 blocks away, so not
	 * loaded either) is not waited for: the segment goes, and the check loads
	 * nothing.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void anOwnerOutOfReachIsNotWaitedFor(GameTestHelper h) {
		floor(h);
		BlockPos far = h.absolutePos(OWNER).offset(20000, 0, 0);
		check(h, !h.getLevel().isLoaded(far), "control: a position 20000 blocks away is not loaded");
		segment(h, OWNER.east(), far);
		h.runAfterDelay(3, () -> {
			checkBlock(h, OWNER.east(), Blocks.AIR);
			check(h, !h.getLevel().isLoaded(far), "the check must not have loaded the owner's chunk");
			h.succeed();
		});
	}

	/**
	 * The decision on its own (a game test cannot unload a chunk next to a
	 * running test): an owner within reach whose chunk is not loaded keeps its
	 * segment and is not asked; a loaded one decides; no owner, or one out of
	 * reach (off its six lines, or more than 9 away), never keeps it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theOwnershipDecision(GameTestHelper h) {
		BlockPos segment = new BlockPos(100, 64, 100);
		java.util.function.Predicate<BlockPos> mustNotAsk = owner -> {
			throw new AssertionError("the owner should not be asked");
		};
		for (Direction dir : Direction.values()) {
			for (int d = 1; d <= 9; d++) {
				BlockPos owner = segment.relative(dir, d);
				check(h, SecretGateSegmentBlockEntity.inReach(owner, segment), dir + " " + d + " should be within reach");
				check(h, SecretGateSegmentBlockEntity.isKept(owner, segment, p -> false, mustNotAsk), "an unloaded owner within reach (" + dir + " " + d + ") should keep its segment");
				check(h, SecretGateSegmentBlockEntity.isKept(owner, segment, p -> true, p -> true), "a loaded owner that keeps it should keep it");
				check(h, !SecretGateSegmentBlockEntity.isKept(owner, segment, p -> true, p -> false), "a loaded owner that does not keep it should not");
			}
			BlockPos tooFar = segment.relative(dir, 10);
			check(h, !SecretGateSegmentBlockEntity.isKept(tooFar, segment, p -> false, mustNotAsk), "an owner 10 away (" + dir + ") is out of reach, even unloaded");
		}
		for (BlockPos owner : new BlockPos[] {segment, segment.offset(1, 1, 0), segment.offset(2, 0, -3), segment.offset(1, 1, 1)}) {
			check(h, !SecretGateSegmentBlockEntity.isKept(owner, segment, p -> false, p -> true), "an owner off the six lines (" + owner + ") never keeps it");
		}
		check(h, !SecretGateSegmentBlockEntity.isKept(null, segment, p -> false, p -> true), "no owner never keeps it");
		h.succeed();
	}

	/** The owner's position and the disguise are saved; clients receive the disguise only. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void ownerIsSavedButNeverSentToClients(GameTestHelper h) {
		floor(h);
		owner(h, true);
		BlockPos rel = OWNER.east();
		SecretGateSegmentBlockEntity segment = segment(h, rel, h.absolutePos(OWNER));
		segment.setDisguise(all(Blocks.BRICKS));
		checkFaces(h, rel, Blocks.BRICKS);
		CompoundTag saved = segment.saveWithFullMetadata();
		check(h, saved.getString("id").equals("secretblocks:secret_gate_segment"), "it should save as secretblocks:secret_gate_segment, got " + saved.getString("id"));
		check(h, saved.contains(SecretGateSegmentBlockEntity.OWNER), "the owner should be saved: " + saved);
		BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(rel), h.getBlockState(rel), saved);
		check(h, loaded instanceof SecretGateSegmentBlockEntity copy && h.absolutePos(OWNER).equals(copy.getOwner())
				&& copy.getState(Direction.NORTH).is(Blocks.BRICKS), "it should load back with its owner and disguise, got " + loaded);
		for (CompoundTag tag : new CompoundTag[] {segment.getUpdateTag(), segment.getUpdatePacket().getTag()}) {
			check(h, tag != null && tag.contains("northState") && !tag.contains(SecretGateSegmentBlockEntity.OWNER),
					"clients should receive the disguise only, never the owner: " + tag);
		}
		h.succeed();
	}
}
