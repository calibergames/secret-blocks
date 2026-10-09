package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.SecretPushing;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Pistons push and pull the full disguised blocks, which keep their
 * disguise and any items stuck in them; the containers, the sensors and the
 * gate stay unpushable; doors, buttons and the like break as their vanilla
 * twins do. Layout: stone floor at y = 0; each piston is powered by a
 * redstone block under it.
 */
public class PushableGameTests implements FabricGameTest {

	/** Every pushable secret block, from the mod's own list, in a fixed order. */
	private static final Block[] PUSHABLE = SecretPushing.pushableBlocks().stream()
			.sorted(java.util.Comparator.comparing(block -> net.minecraft.core.Registry.BLOCK.getKey(block).toString())).toArray(Block[]::new);

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	/** The i-th of up to 16 slots: a piston facing east at x = 0 or 4, the block in front at x + 1. */
	private static BlockPos slot(int i) {
		return new BlockPos((i % 2) * 4, 1, i / 2);
	}

	private static void pistonAt(GameTestHelper h, BlockPos pos, Block piston, Direction facing) {
		h.setBlock(pos, piston.defaultBlockState().setValue(PistonBaseBlock.FACING, facing));
	}

	private static boolean noBlockEntity(GameTestHelper h, BlockPos rel) {
		BlockPos abs = h.absolutePos(rel);
		return h.getLevel().getChunkAt(abs).getBlockEntity(abs, LevelChunk.EntityCreationType.CHECK) == null;
	}

	/**
	 * Every pushable secret block, pushed by a vanilla piston, moves one block,
	 * keeps its disguise, and leaves no block entity behind (the wire and diode
	 * blocks skip vanilla's removal code when moved).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyPushableSecretBlockMovesWithItsDisguise(GameTestHelper h) {
		floor(h);
		for (int i = 0; i < PUSHABLE.length; i++) {
			BlockPos piston = slot(i);
			pistonAt(h, piston, Blocks.PISTON, Direction.EAST);
			h.setBlock(piston.east(), PUSHABLE[i]);
			secret(h, piston.east()).setDisguise(all(Blocks.BRICKS));
		}
		h.runAfterDelay(2, () -> {
			for (int i = 0; i < PUSHABLE.length; i++) {
				h.setBlock(slot(i).below(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(8, () -> {
			List<String> problems = new ArrayList<>();
			for (int i = 0; i < PUSHABLE.length; i++) {
				BlockPos from = slot(i).east();
				BlockPos to = from.east();
				if (!h.getBlockState(to).is(PUSHABLE[i])) {
					problems.add(PUSHABLE[i] + " not moved (at the target: " + h.getBlockState(to).getBlock() + ")");
					continue;
				}
				if (!(h.getBlockEntity(to) instanceof DisguisedBlockEntity moved) || !moved.getState(Direction.NORTH).is(Blocks.BRICKS)) {
					problems.add(PUSHABLE[i] + " lost its disguise");
				}
				if (!noBlockEntity(h, from)) {
					problems.add(PUSHABLE[i] + " left a block entity behind");
				}
			}
			check(h, problems.isEmpty(), "pushed secret blocks: " + problems);
			for (int i = 0; i < PUSHABLE.length; i++) {
				h.setBlock(slot(i).below(), Blocks.STONE);
			}
			h.succeed();
		});
	}

	/**
	 * Pushed out and pulled back by a sticky piston, every pushable secret block
	 * leaves no block entity at the far spot it left (vanilla's wire and diode
	 * blocks skip the removal code when moved; in a plain push the piston head
	 * takes the old spot, so only a pull shows it).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPulledBackBlockLeavesNoBlockEntityBehind(GameTestHelper h) {
		floor(h);
		for (int i = 0; i < PUSHABLE.length; i++) {
			BlockPos piston = slot(i);
			pistonAt(h, piston, Blocks.STICKY_PISTON, Direction.EAST);
			h.setBlock(piston.east(), PUSHABLE[i]);
			secret(h, piston.east()).setDisguise(all(Blocks.BRICKS));
		}
		h.runAfterDelay(2, () -> {
			for (int i = 0; i < PUSHABLE.length; i++) {
				h.setBlock(slot(i).below(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(8, () -> {
			for (int i = 0; i < PUSHABLE.length; i++) {
				h.setBlock(slot(i).below(), Blocks.STONE);
			}
		});
		h.runAfterDelay(14, () -> {
			List<String> problems = new ArrayList<>();
			for (int i = 0; i < PUSHABLE.length; i++) {
				BlockPos home = slot(i).east();
				BlockPos far = home.east();
				if (!h.getBlockState(home).is(PUSHABLE[i])) {
					problems.add(PUSHABLE[i] + " not pulled back");
				} else if (!(h.getBlockEntity(home) instanceof DisguisedBlockEntity back) || !back.getState(Direction.NORTH).is(Blocks.BRICKS)) {
					problems.add(PUSHABLE[i] + " lost its disguise");
				}
				if (!noBlockEntity(h, far)) {
					problems.add(PUSHABLE[i] + " left a block entity at " + far);
				}
			}
			check(h, problems.isEmpty(), "pulled back: " + problems);
			h.succeed();
		});
	}

	/**
	 * Moved blocks keep their state and their own data: an upside-down stair
	 * its facing, a double slab, an open top trapdoor; a line of three Dummy Blocks
	 * each its own disguise (vanilla moves the front block first); a Dummy Block
	 * dragged along by a slime block beside the one pushed.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void statesLinesAndSlimeMoveIntact(GameTestHelper h) {
		floor(h);
		BlockState[] states = {
				ModBlocks.STAIR_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH)
						.setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP),
				ModBlocks.SLAB_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.DOUBLE),
				ModBlocks.TRAPDOOR_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true)
						.setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP)};
		for (int i = 0; i < states.length; i++) {
			pistonAt(h, slot(i), Blocks.PISTON, Direction.EAST);
			h.setBlock(slot(i).east(), states[i]);
			secret(h, slot(i).east()).setDisguise(all(Blocks.BRICKS));
		}
		// A line of three at slot 4, each with its own disguise.
		Block[] line = {Blocks.BRICKS, Blocks.OAK_PLANKS, Blocks.GOLD_BLOCK};
		pistonAt(h, slot(4), Blocks.PISTON, Direction.EAST);
		for (int k = 0; k < 3; k++) {
			h.setBlock(slot(4).east(k + 1), ModBlocks.SOLID_BLOCK);
			secret(h, slot(4).east(k + 1)).setDisguise(all(line[k]));
		}
		// Slot 6: the piston pushes a slime block; a Dummy Block on top of the slime is dragged along.
		pistonAt(h, slot(6), Blocks.PISTON, Direction.EAST);
		h.setBlock(slot(6).east(), Blocks.SLIME_BLOCK);
		h.setBlock(slot(6).east().above(), ModBlocks.SOLID_BLOCK);
		secret(h, slot(6).east().above()).setDisguise(all(Blocks.SAND));
		int[] used = {0, 1, 2, 4, 6};
		h.runAfterDelay(2, () -> {
			for (int i : used) {
				h.setBlock(slot(i).below(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(8, () -> {
			for (int i = 0; i < states.length; i++) {
				BlockPos to = slot(i).east(2);
				check(h, h.getBlockState(to).equals(states[i]), "moved, " + states[i] + " should be the same state, is " + h.getBlockState(to));
				checkFaces(h, to, Blocks.BRICKS);
			}
			for (int k = 0; k < 3; k++) {
				BlockPos to = slot(4).east(k + 2);
				checkBlock(h, to, ModBlocks.SOLID_BLOCK);
				checkFaces(h, to, line[k]);
			}
			checkBlock(h, slot(6).east(2), Blocks.SLIME_BLOCK);
			checkBlock(h, slot(6).east(2).above(), ModBlocks.SOLID_BLOCK);
			checkFaces(h, slot(6).east(2).above(), Blocks.SAND);
			for (int i : used) {
				h.setBlock(slot(i).below(), Blocks.STONE);
			}
			h.succeed();
		});
	}

	/**
	 * A Dummy Block with a different disguise, direction and rotation on each
	 * face, pushed and pulled back by a Secret Sticky Piston, has every face as
	 * before.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void pushedAndPulledADummyBlockKeepsEveryFace(GameTestHelper h) {
		floor(h);
		BlockPos piston = new BlockPos(1, 1, 3);
		BlockPos block = piston.east();
		pistonAt(h, piston, ModBlocks.SECRET_STICKY_PISTON, Direction.EAST);
		h.setBlock(block, ModBlocks.SOLID_BLOCK);
		DisguisedBlockEntity entity = secret(h, block);
		Block[] faces = {Blocks.BRICKS, Blocks.OAK_PLANKS, Blocks.STONE, Blocks.GOLD_BLOCK, Blocks.DIRT, Blocks.SAND};
		for (Direction dir : Direction.values()) {
			entity.setState(dir, faces[dir.ordinal()].defaultBlockState());
			entity.setRotation(dir, 90 * (dir.ordinal() % 4));
		}
		entity.setDirection(Direction.NORTH, Direction.UP);
		CompoundTag before = entity.getDisguise().save(new CompoundTag());
		h.runAfterDelay(2, () -> h.setBlock(piston.below(), Blocks.REDSTONE_BLOCK));
		h.runAfterDelay(7, () -> {
			checkBlock(h, block.east(), ModBlocks.SOLID_BLOCK);
			CompoundTag out = secret(h, block.east()).getDisguise().save(new CompoundTag());
			out.remove("age");
			CompoundTag want = before.copy();
			want.remove("age");
			check(h, out.equals(want), "pushed, every face should be as before:\n " + want + "\n " + out);
			h.setBlock(piston.below(), Blocks.STONE);
		});
		h.runAfterDelay(12, () -> {
			checkBlock(h, block, ModBlocks.SOLID_BLOCK);
			CompoundTag back = secret(h, block).getDisguise().save(new CompoundTag());
			back.remove("age");
			CompoundTag want = before.copy();
			want.remove("age");
			check(h, back.equals(want), "pulled back, every face should be as before:\n " + want + "\n " + back);
			h.succeed();
		});
	}

	/**
	 * Items stuck in a Dummy Block before 2.0.0 travel with it (clients are sent
	 * only its disguise during the move), and still drop when it is broken.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void storedItemsTravelWithTheBlock(GameTestHelper h) throws Exception {
		floor(h);
		BlockPos piston = new BlockPos(1, 1, 3);
		BlockPos block = piston.east();
		pistonAt(h, piston, Blocks.PISTON, Direction.EAST);
		h.setBlock(block, ModBlocks.SOLID_BLOCK);
		plain(h, block).load(TagParser.parseTag("{Items:[{Slot:0b,id:\"minecraft:diamond\",Count:4b}]}"));
		boolean[] seen = {false};
		h.onEachTick(() -> {
			if (!seen[0] && h.getBlockEntity(block.east()) instanceof net.minecraft.world.level.block.piston.PistonMovingBlockEntity moving) {
				seen[0] = true;
				CompoundTag saved = moving.saveWithoutMetadata().getCompound("SecretBlocksDisguise");
				CompoundTag sent = moving.getUpdateTag().getCompound("SecretBlocksDisguise");
				check(h, saved.contains("Items"), "the move should carry the stuck items: " + saved);
				check(h, sent.contains("northState") && !sent.contains("Items"), "clients should get the disguise only, never the items: " + sent);
			}
		});
		h.runAfterDelay(2, () -> h.setBlock(piston.below(), Blocks.REDSTONE_BLOCK));
		h.runAfterDelay(7, () -> {
			check(h, seen[0], "control: the move was caught");
			checkBlock(h, block.east(), ModBlocks.SOLID_BLOCK);
			check(h, h.getBlockEntity(block.east()) instanceof SecretBlockEntity moved
					&& moved.saveWithoutMetadata().getList("Items", 10).size() == 1, "the stuck diamonds should have moved with the block");
			h.setBlock(block.east(), Blocks.AIR);
			int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(block.east())).inflate(2), e -> e.getItem().is(Items.DIAMOND))
					.stream().mapToInt(e -> e.getItem().getCount()).sum();
			check(h, dropped == 4, "broken after the move, it should drop the 4 diamonds, dropped " + dropped);
			h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(block.east())).inflate(2)).forEach(ItemEntity::discard);
			h.setBlock(piston.below(), Blocks.STONE);
			h.succeed();
		});
	}

	/**
	 * The containers, the sensors, the comparator and the gate stay unpushable:
	 * the piston does not extend and the block stays.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void containersSensorsAndTheGateAreRefused(GameTestHelper h) {
		floor(h);
		Block[] refused = {ModBlocks.SECRET_CHEST, ModBlocks.SECRET_TRAPPED_CHEST, ModBlocks.SECRET_DISPENSER, ModBlocks.SECRET_DROPPER,
				ModBlocks.SECRET_HOPPER, ModBlocks.SECRET_SCULK_SENSOR, ModBlocks.SECRET_DAYLIGHT_DETECTOR,
				ModBlocks.SECRET_GATE};
		for (int i = 0; i < refused.length; i++) {
			pistonAt(h, slot(i), Blocks.PISTON, Direction.EAST);
			h.setBlock(slot(i).east(), refused[i]);
		}
		h.runAfterDelay(2, () -> {
			for (int i = 0; i < refused.length; i++) {
				h.setBlock(slot(i).below(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(8, () -> {
			for (int i = 0; i < refused.length; i++) {
				check(h, !h.getBlockState(slot(i)).getValue(PistonBaseBlock.EXTENDED), "a piston should not push " + refused[i]);
				checkBlock(h, slot(i).east(), refused[i]);
				h.setBlock(slot(i).below(), Blocks.STONE);
			}
			h.succeed();
		});
	}

	/** What a piston does to a block: refused, moved, or broken. */
	private static String outcome(GameTestHelper h, BlockPos at, Block block) {
		if (h.getBlockState(at).is(block)) {
			return "refused";
		}
		return h.getBlockState(at.east()).is(block) ? "moved" : "broken";
	}

	/**
	 * Doors, trapdoors, buttons, levers and pressure plates meet a piston as
	 * their vanilla twins do, each beside its twin: doors, buttons, levers and
	 * plates break, trapdoors move.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void doorsAndSwitchesBehaveAsTheirVanillaTwins(GameTestHelper h) {
		floor(h);
		Block[][] pairs = {
				{ModBlocks.DOOR_BLOCK, Blocks.OAK_DOOR}, {ModBlocks.TRAPDOOR_BLOCK, Blocks.OAK_TRAPDOOR},
				{ModBlocks.WOODEN_BUTTON, Blocks.OAK_BUTTON}, {ModBlocks.SECRET_LEVER, Blocks.LEVER},
				{ModBlocks.PRESSURE_PLATE, Blocks.OAK_PRESSURE_PLATE}, {ModBlocks.IRON_TRAPDOOR_BLOCK, Blocks.IRON_TRAPDOOR},
				{ModBlocks.STONE_BUTTON, Blocks.STONE_BUTTON}, {ModBlocks.IRON_DOOR_BLOCK, Blocks.IRON_DOOR}};
		for (int p = 0; p < pairs.length; p++) {
			for (int side = 0; side < 2; side++) {
				int i = p * 2 + side;
				BlockPos at = slot(i).east();
				pistonAt(h, slot(i), Blocks.PISTON, Direction.EAST);
				Block block = pairs[p][side];
				BlockState state = block.defaultBlockState();
				if (state.hasProperty(FaceAttachedHorizontalDirectionalBlock.FACE)) {
					state = state.setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR);
				}
				h.setBlock(at, state);
				if (block instanceof DoorBlock) {
					h.setBlock(at.above(), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
				}
			}
		}
		h.runAfterDelay(2, () -> {
			for (int i = 0; i < pairs.length * 2; i++) {
				h.setBlock(slot(i).below(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(8, () -> {
			for (int p = 0; p < pairs.length; p++) {
				String secret = outcome(h, slot(p * 2).east(), pairs[p][0]);
				String vanilla = outcome(h, slot(p * 2 + 1).east(), pairs[p][1]);
				check(h, secret.equals(vanilla), pairs[p][0] + " was " + secret + ", its vanilla twin " + pairs[p][1] + " was " + vanilla);
			}
			h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(8, 4, 8))).inflate(2)).forEach(ItemEntity::discard);
			for (int i = 0; i < pairs.length * 2; i++) {
				h.setBlock(slot(i).below(), Blocks.STONE);
			}
			h.succeed();
		});
	}

	/**
	 * The Secret Dust, Wire, Repeater and Comparator meet a piston as vanilla's
	 * dust, repeater and comparator do, each beside its twin: the piston breaks
	 * them, and each secret one drops itself.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void redstoneBreaksAsItsVanillaTwins(GameTestHelper h) {
		floor(h);
		Block[][] pairs = {
				{ModBlocks.SECRET_REDSTONE, Blocks.REDSTONE_WIRE}, {ModBlocks.SECRET_REDSTONE_WIRE, Blocks.REDSTONE_WIRE},
				{ModBlocks.SECRET_REPEATER, Blocks.REPEATER}, {ModBlocks.SECRET_COMPARATOR, Blocks.COMPARATOR}};
		h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(8, 4, 8))).inflate(2)).forEach(ItemEntity::discard);
		for (int p = 0; p < pairs.length; p++) {
			for (int side = 0; side < 2; side++) {
				int i = p * 2 + side;
				pistonAt(h, slot(i), Blocks.PISTON, Direction.EAST);
				h.setBlock(slot(i).east(), pairs[p][side]);
			}
		}
		h.runAfterDelay(2, () -> {
			for (int i = 0; i < pairs.length * 2; i++) {
				h.setBlock(slot(i).below(), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(8, () -> {
			List<String> problems = new ArrayList<>();
			for (int p = 0; p < pairs.length; p++) {
				String secret = outcome(h, slot(p * 2).east(), pairs[p][0]);
				String vanilla = outcome(h, slot(p * 2 + 1).east(), pairs[p][1]);
				if (!secret.equals("broken") || !vanilla.equals("broken")) {
					problems.add(pairs[p][0] + " was " + secret + ", its vanilla twin " + pairs[p][1] + " was " + vanilla);
				}
				Item item = pairs[p][0].asItem();
				AABB around = new AABB(h.absolutePos(slot(p * 2).east())).inflate(1.5);
				if (h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(item)).isEmpty()) {
					problems.add(pairs[p][0] + " dropped no " + item);
				}
			}
			check(h, problems.isEmpty(), "pushed redstone: " + problems);
			h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(8, 4, 8))).inflate(2)).forEach(ItemEntity::discard);
			for (int i = 0; i < pairs.length * 2; i++) {
				h.setBlock(slot(i).below(), Blocks.STONE);
			}
			h.succeed();
		});
	}

	/**
	 * A hidden 2×2 piston door: four Secret Sticky Pistons push four Dummy
	 * Blocks into an opening and pull them back out, twice; the door blocks
	 * keep their disguise throughout.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void aHiddenTwoByTwoDoor(GameTestHelper h) {
		floor(h);
		// The opening: x = 3, 4; y = 1, 2; z = 3. Door blocks wait at z = 4 in front of pistons at z = 5 facing north.
		List<BlockPos> openings = new ArrayList<>();
		for (int x = 3; x <= 4; x++) {
			for (int y = 1; y <= 2; y++) {
				BlockPos opening = new BlockPos(x, y, 3);
				openings.add(opening);
				pistonAt(h, opening.south(2), ModBlocks.SECRET_STICKY_PISTON, Direction.NORTH);
				secret(h, opening.south(2)).setDisguise(all(Blocks.STONE_BRICKS));
				h.setBlock(opening.south(), ModBlocks.SOLID_BLOCK);
				secret(h, opening.south()).setDisguise(all(Blocks.BRICKS));
			}
		}
		for (int cycle = 0; cycle < 2; cycle++) {
			int t = 2 + cycle * 20;
			h.runAfterDelay(t, () -> {
				for (BlockPos opening : openings) {
					h.setBlock(opening.south(3), Blocks.REDSTONE_BLOCK);
				}
			});
			h.runAfterDelay(t + 8, () -> {
				for (BlockPos opening : openings) {
					checkBlock(h, opening, ModBlocks.SOLID_BLOCK);
					checkFaces(h, opening, Blocks.BRICKS);
					checkBlock(h, opening.south(), ModBlocks.SECRET_PISTON_HEAD);
					h.setBlock(opening.south(3), Blocks.AIR);
				}
			});
			h.runAfterDelay(t + 16, () -> {
				for (BlockPos opening : openings) {
					checkBlock(h, opening, Blocks.AIR);
					checkBlock(h, opening.south(), ModBlocks.SOLID_BLOCK);
					checkFaces(h, opening.south(), Blocks.BRICKS);
					checkFaces(h, opening.south(2), Blocks.STONE_BRICKS);
				}
			});
		}
		h.runAfterDelay(44, h::succeed);
	}
}
