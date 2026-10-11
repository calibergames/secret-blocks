package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Block behavior that matches 1.2.0 or vanilla: doors and trapdoors open by hand
 * (the wooden ones), redstone parts, the chest, and collision shapes. Behavior
 * that changed on purpose (iron doors, Secret Redstone Dust next to a lamp,
 * probe persistence) is not pinned here.
 */
public class BehaviourGameTests implements FabricGameTest {

	private static final BlockPos A = new BlockPos(3, 1, 3);

	@GameTest(template = EMPTY_STRUCTURE)
	public void doorOpensByHand(GameTestHelper h) {
		floor(h);
		BlockState lower = ModBlocks.DOOR_BLOCK.defaultBlockState();
		h.setBlock(A, lower);
		h.setBlock(A.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		h.useBlock(A);
		check(h, h.getBlockState(A).getValue(DoorBlock.OPEN), "lower half should be open");
		check(h, h.getBlockState(A.above()).getValue(DoorBlock.OPEN), "upper half should be open");
		h.useBlock(A);
		check(h, !h.getBlockState(A).getValue(DoorBlock.OPEN), "door should close again");
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void trapdoorOpensByHand(GameTestHelper h) {
		floor(h);
		h.setBlock(A, ModBlocks.TRAPDOOR_BLOCK.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM));
		h.useBlock(A);
		check(h, h.getBlockState(A).getValue(TrapDoorBlock.OPEN), "trapdoor should open");
		h.useBlock(A);
		check(h, !h.getBlockState(A).getValue(TrapDoorBlock.OPEN), "trapdoor should close again");
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void leverLightsLamp(GameTestHelper h) {
		floor(h);
		BlockPos lamp = A.east();
		h.setBlock(lamp, Blocks.REDSTONE_LAMP);
		h.setBlock(A, ModBlocks.SECRET_LEVER.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
		h.useBlock(A);
		check(h, h.getBlockState(A).getValue(LeverBlock.POWERED), "lever should be on");
		h.succeedWhen(() -> check(h, h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp should be lit"));
	}

	private static void buttonPulses(GameTestHelper h, Block button) {
		floor(h);
		h.setBlock(A, button.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
		h.useBlock(A);
		check(h, h.getBlockState(A).getValue(ButtonBlock.POWERED), "button should be pressed");
		h.succeedWhen(() -> check(h, !h.getBlockState(A).getValue(ButtonBlock.POWERED), "button should release"));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void woodenButtonPulses(GameTestHelper h) { buttonPulses(h, ModBlocks.WOODEN_BUTTON); }

	@GameTest(template = EMPTY_STRUCTURE)
	public void stoneButtonPulses(GameTestHelper h) { buttonPulses(h, ModBlocks.STONE_BUTTON); }

	/** Secret Redstone Dust passes a signal on to vanilla wire, which lights a lamp. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void secretRedstoneFeedsVanillaWire(GameTestHelper h) {
		floor(h);
		BlockPos lamp = new BlockPos(4, 1, 3);
		h.setBlock(lamp, Blocks.REDSTONE_LAMP);
		h.setBlock(new BlockPos(3, 1, 3), Blocks.REDSTONE_WIRE);
		h.setBlock(new BlockPos(2, 1, 3), ModBlocks.SECRET_REDSTONE);
		h.setBlock(new BlockPos(1, 1, 3), Blocks.REDSTONE_BLOCK);
		h.succeedWhen(() -> check(h, h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp should be lit"));
	}

	/** A comparator reads the Secret Chest. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void chestDrivesComparator(GameTestHelper h) {
		floor(h);
		BlockPos comparator = A.north();
		BlockPos lamp = comparator.north();
		h.setBlock(lamp, Blocks.REDSTONE_LAMP);
		h.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.SOUTH));
		h.setBlock(A, ModBlocks.SECRET_CHEST);
		// Insert the way menus and hoppers do: setItem, then setChanged. (setItem
		// alone is enough too, as with a vanilla chest:
		// TrappedChestGameTests.aSetSlotReachesComparators.)
		SecretChestBlockEntity chest = chest(h, A);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
		chest.setChanged();
		h.succeedWhen(() -> check(h, h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp should be lit"));
	}

	/** Chest contents survive a save and load of the block entity. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void chestContentsRoundTrip(GameTestHelper h) {
		h.setBlock(A, ModBlocks.SECRET_CHEST);
		SecretChestBlockEntity chest = chest(h, A);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
		chest.setItem(26, new ItemStack(Items.OAK_LOG, 64));
		CompoundTag saved = chest.saveWithoutMetadata();
		SecretChestBlockEntity copy = new SecretChestBlockEntity(h.absolutePos(A), h.getBlockState(A));
		copy.load(saved);
		check(h, ItemStack.matches(copy.getItem(0), new ItemStack(Items.DIAMOND, 5)), "slot 0 should hold 5 diamonds");
		check(h, ItemStack.matches(copy.getItem(26), new ItemStack(Items.OAK_LOG, 64)), "slot 26 should hold 64 oak logs");
		check(h, copy.getItem(1).isEmpty(), "slot 1 should be empty");
		h.succeed();
	}

	private static VoxelShape collision(GameTestHelper h, BlockState state) {
		h.setBlock(A, state);
		return h.getBlockState(A).getCollisionShape(h.getLevel(), h.absolutePos(A));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void collisionShapes(GameTestHelper h) {
		check(h, collision(h, ModBlocks.GHOST_BLOCK.defaultBlockState()).isEmpty(), "Ghost Block has no collision");
		VoxelShape solid = collision(h, ModBlocks.SOLID_BLOCK.defaultBlockState());
		check(h, solid.max(Direction.Axis.Y) == 1.0 && solid.min(Direction.Axis.Y) == 0.0, "Dummy Block is a full block");
		VoxelShape bottomSlab = collision(h, ModBlocks.SLAB_BLOCK.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
		check(h, bottomSlab.max(Direction.Axis.Y) == 0.5, "bottom Secret Slab is half a block tall");
		VoxelShape stairs = collision(h, ModBlocks.STAIR_BLOCK.defaultBlockState().setValue(StairBlock.HALF, Half.BOTTOM));
		check(h, stairs.max(Direction.Axis.Y) == 1.0 && stairs.toAabbs().size() > 1, "Secret Stairs collide as stairs");
		h.succeed();
	}

	/**
	 * Breaking any secret block removes its disguise block entity. Vanilla
	 * blocks that override {@code onRemove} without calling super (the
	 * observer does) would otherwise leave an orphaned block entity behind.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void breakingRemovesTheBlockEntity(GameTestHelper h) {
		floor(h);
		StringBuilder leftBehind = new StringBuilder();
		for (Block block : ModBlocks.secretBlocksList) {
			h.setBlock(A, block);
			if (!(h.getBlockEntity(A) instanceof DisguisedBlockEntity)) {
				leftBehind.append(block).append(" (no block entity after placing) ");
				continue;
			}
			h.getLevel().destroyBlock(h.absolutePos(A), false);
			if (h.getBlockEntity(A) != null) {
				leftBehind.append(block).append(' ');
				h.getLevel().removeBlockEntity(h.absolutePos(A));
			}
			h.setBlock(A, Blocks.AIR);
		}
		check(h, leftBehind.length() == 0, "block entity left behind after breaking: " + leftBehind);
		h.succeed();
	}

	/**
	 * Changing a secret block's state (a property such as a stair's shape, a
	 * door's open flag or a plate's powered flag) keeps its disguise. The
	 * server calls {@code onRemove} for every state change, not only for
	 * removal, so an {@code onRemove} that ignores which block replaces it
	 * would delete the block entity.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void stateChangesKeepTheDisguise(GameTestHelper h) {
		floor(h);
		StringBuilder lost = new StringBuilder();
		java.util.Map<Direction, BlockState> bricks = new java.util.EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			bricks.put(dir, Blocks.BRICKS.defaultBlockState());
		}
		for (Block block : ModBlocks.secretBlocksList) {
			BlockState state = block.defaultBlockState();
			if (state.getProperties().isEmpty()) {
				continue;
			}
			h.setBlock(A, state);
			secret(h, A).setDisguise(bricks);
			net.minecraft.world.level.block.state.properties.Property<?> property = state.getProperties().iterator().next();
			h.getLevel().setBlock(h.absolutePos(A), state.cycle(property), 2);
			BlockState after = h.getBlockState(A);
			if (!after.is(block) || !(h.getBlockEntity(A) instanceof DisguisedBlockEntity secret)
					|| !secret.getState(Direction.NORTH).is(Blocks.BRICKS)) {
				lost.append(block).append(" (").append(property.getName()).append(") ");
			}
			h.setBlock(A, Blocks.AIR);
		}
		check(h, lost.length() == 0, "disguise lost on a state change: " + lost);
		h.succeed();
	}

	/** A Secret Stairs' shape changes when a stair is placed beside it; its disguise stays. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void stairsKeepDisguiseWhenShapeChanges(GameTestHelper h) {
		floor(h);
		h.setBlock(A, ModBlocks.STAIR_BLOCK.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
		java.util.Map<Direction, BlockState> bricks = new java.util.EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			bricks.put(dir, Blocks.BRICKS.defaultBlockState());
		}
		secret(h, A).setDisguise(bricks);
		h.setBlock(A.north(), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
		check(h, h.getBlockState(A).getValue(StairBlock.SHAPE) != net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT,
				"the stair beside it should change its shape");
		checkFaces(h, A, Blocks.BRICKS);
		h.succeed();
	}

	/**
	 * A survival player breaking each secret block (the real server path,
	 * {@code ServerPlayerGameMode.destroyBlock}) leaves air, no block entity,
	 * and the block's item (nothing at all for a block with no item).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void survivalPlayerBreaksEverySecretBlockCleanly(GameTestHelper h) {
		floor(h);
		net.minecraft.server.level.ServerPlayer player = player(h, new net.minecraft.world.phys.Vec3(3.5, 1, 0.5), new net.minecraft.world.phys.Vec3(3.5, 1.5, 3.5));
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		// A pickaxe: the right tool for every secret block (the Secret Hopper needs one to drop).
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
		StringBuilder problems = new StringBuilder();
		for (Block block : ModBlocks.secretBlocksList) {
			h.setBlock(A, block);
			boolean broken = player.gameMode.destroyBlock(h.absolutePos(A));
			BlockState after = h.getBlockState(A);
			if (!broken || !after.isAir() || h.getBlockEntity(A) != null) {
				problems.append(block).append(" -> broken=").append(broken).append(", left ").append(after)
						.append(", block entity ").append(h.getBlockEntity(A)).append("; ");
			}
			var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(h.absolutePos(A)).inflate(3));
			if (block.asItem() == Items.AIR) {
				// A block with no item (the Secret Gate segment) drops nothing at all.
				if (!drops.isEmpty()) {
					problems.append(block).append(" has no item but dropped ").append(drops.stream().map(e -> e.getItem().toString()).toList()).append("; ");
				}
			} else if (drops.stream().noneMatch(e -> e.getItem().is(block.asItem()))) {
				problems.append(block).append(" dropped no ").append(block.asItem()).append("; ");
			}
			drops.forEach(net.minecraft.world.entity.Entity::discard);
			h.setBlock(A, Blocks.AIR);
		}
		check(h, problems.length() == 0, "survival break problems: " + problems);
		h.succeed();
	}

	/**
	 * A secret block saved without a block entity (an older build's block, as
	 * found in an old test world) gets a fresh disguise block
	 * entity the first time server code asks for it (vanilla creates missing
	 * block entities on access), so probes and placement still work on it.
	 * The client draws such a block as stone (SecretBlockBakedModel).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void missingBlockEntityIsRecreatedOnAccess(GameTestHelper h) {
		floor(h);
		StringBuilder problems = new StringBuilder();
		for (Block block : ModBlocks.secretBlocksList) {
			h.setBlock(A, block);
			h.getLevel().removeBlockEntity(h.absolutePos(A));
			if (h.getLevel().getChunkAt(h.absolutePos(A)).getBlockEntity(h.absolutePos(A),
					net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) != null) {
				problems.append(block).append(" (block entity not removed, test invalid); ");
				continue;
			}
			BlockEntity again = h.getLevel().getBlockEntity(h.absolutePos(A));
			if (!(again instanceof DisguisedBlockEntity)) {
				problems.append(block).append(" -> ").append(again).append("; ");
			}
			h.setBlock(A, Blocks.AIR);
		}
		check(h, problems.length() == 0, "no disguise block entity after access: " + problems);
		h.succeed();
	}

	/** A double Secret Slab drops two slabs, as a double oak slab does; a single one drops one. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aDoubleSlabDropsTwo(GameTestHelper h) {
		floor(h);
		BlockPos slab = new BlockPos(1, 1, 1);
		for (SlabType type : SlabType.values()) {
			for (Block block : new Block[] {ModBlocks.SLAB_BLOCK, Blocks.OAK_SLAB}) {
				h.setBlock(slab, block.defaultBlockState().setValue(SlabBlock.TYPE, type));
				int count = Block.getDrops(h.getBlockState(slab), h.getLevel(), h.absolutePos(slab),
						h.getLevel().getBlockEntity(h.absolutePos(slab))).stream().mapToInt(ItemStack::getCount).sum();
				int expected = type == SlabType.DOUBLE ? 2 : 1;
				check(h, count == expected, block + " " + type + " should drop " + expected + ", drops " + count);
			}
		}
		h.succeed();
	}

	/**
	 * A Secret Slab takes the half a vanilla slab would: clicking the floor's
	 * top or the lower half of a wall's side gives a bottom slab, the upper
	 * half of a side a top slab, and a second slab makes a double. Each
	 * collides as that half (the disguise is drawn as a full block).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void slabsTakeTheHalfYouClick(GameTestHelper h) {
		floor(h);
		h.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
		Vec3 feet = new Vec3(2.5, 1, 0.5);
		// On the floor: bottom.
		place(h, ModBlocks.SLAB_BLOCK.asItem(), feet, new BlockPos(1, 0, 2), Direction.UP, new Vec3(1.5, 1.0, 2.5));
		// Against the stone's west side, lower and upper half.
		place(h, ModBlocks.SLAB_BLOCK.asItem(), feet, new BlockPos(4, 1, 2), Direction.WEST, new Vec3(4.0, 1.25, 2.5));
		h.setBlock(new BlockPos(4, 2, 2), Blocks.STONE);
		place(h, ModBlocks.SLAB_BLOCK.asItem(), feet, new BlockPos(4, 2, 2), Direction.WEST, new Vec3(4.0, 2.75, 2.5));
		check(h, h.getBlockState(new BlockPos(1, 1, 2)).getValue(SlabBlock.TYPE) == SlabType.BOTTOM, "on the floor: bottom, is " + h.getBlockState(new BlockPos(1, 1, 2)));
		check(h, h.getBlockState(new BlockPos(3, 1, 2)).getValue(SlabBlock.TYPE) == SlabType.BOTTOM, "lower half of a side: bottom, is " + h.getBlockState(new BlockPos(3, 1, 2)));
		check(h, h.getBlockState(new BlockPos(3, 2, 2)).getValue(SlabBlock.TYPE) == SlabType.TOP, "upper half of a side: top, is " + h.getBlockState(new BlockPos(3, 2, 2)));
		// A second slab on the bottom one makes a double.
		place(h, ModBlocks.SLAB_BLOCK.asItem(), feet, new BlockPos(1, 1, 2), Direction.UP, new Vec3(1.5, 1.5, 2.5));
		check(h, h.getBlockState(new BlockPos(1, 1, 2)).getValue(SlabBlock.TYPE) == SlabType.DOUBLE, "a second slab makes a double, is " + h.getBlockState(new BlockPos(1, 1, 2)));
		// Collision per half.
		VoxelShape top = h.getBlockState(new BlockPos(3, 2, 2)).getCollisionShape(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 2)));
		VoxelShape bottom = h.getBlockState(new BlockPos(3, 1, 2)).getCollisionShape(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 2)));
		VoxelShape both = h.getBlockState(new BlockPos(1, 1, 2)).getCollisionShape(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 2)));
		check(h, top.min(Direction.Axis.Y) == 0.5 && top.max(Direction.Axis.Y) == 1.0, "a top slab collides in the upper half, is " + top);
		check(h, bottom.min(Direction.Axis.Y) == 0.0 && bottom.max(Direction.Axis.Y) == 0.5, "a bottom slab collides in the lower half, is " + bottom);
		check(h, both.min(Direction.Axis.Y) == 0.0 && both.max(Direction.Axis.Y) == 1.0, "a double slab collides as a full block, is " + both);
		h.succeed();
	}
}
