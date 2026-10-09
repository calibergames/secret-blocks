package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Iron Door and Secret Iron Trapdoor behave like vanilla iron
 * ones: redstone only, iron door sounds. Each check runs on the
 * secret block and, beside it, on the vanilla block.
 */
public class IronDoorGameTests implements FabricGameTest {

	private static final BlockPos SECRET = new BlockPos(2, 1, 3);
	private static final BlockPos VANILLA = new BlockPos(5, 1, 3);

	private static void door(GameTestHelper h, BlockPos pos, Block door) {
		BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
		h.setBlock(pos, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		h.setBlock(pos.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	private static boolean open(GameTestHelper h, BlockPos pos) {
		return h.getBlockState(pos).getValue(BlockStateProperties.OPEN);
	}

	private static net.minecraft.world.InteractionResult handUse(GameTestHelper h, BlockPos pos) {
		Player player = h.makeMockPlayer();
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.NORTH, h.absolutePos(pos), false);
		return h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
	}

	/** Using it by hand does nothing, as with a vanilla iron door and iron trapdoor. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void handUseDoesNothing(GameTestHelper h) {
		floor(h);
		door(h, SECRET, ModBlocks.IRON_DOOR_BLOCK);
		door(h, VANILLA, Blocks.IRON_DOOR);
		h.setBlock(SECRET.south(2), ModBlocks.IRON_TRAPDOOR_BLOCK);
		h.setBlock(VANILLA.south(2), Blocks.IRON_TRAPDOOR);
		for (BlockPos pos : new BlockPos[] {SECRET, SECRET.above(), VANILLA, VANILLA.above(), SECRET.south(2), VANILLA.south(2)}) {
			var result = handUse(h, pos);
			check(h, result == net.minecraft.world.InteractionResult.PASS, h.getBlockState(pos).getBlock() + " hand use should PASS, got " + result);
			check(h, !open(h, pos), h.getBlockState(pos).getBlock() + " must not open by hand");
		}
		// Control: the wooden secret door still opens by hand.
		door(h, new BlockPos(0, 1, 6), ModBlocks.DOOR_BLOCK);
		handUse(h, new BlockPos(0, 1, 6));
		check(h, open(h, new BlockPos(0, 1, 6)), "the wooden Secret Door should still open by hand");
		h.succeed();
	}

	/** A redstone signal opens it; removing the signal closes it. Doors keep both halves' disguise. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void redstoneOpensAndCloses(GameTestHelper h) {
		floor(h);
		h.setBlock(SECRET.north(), Blocks.BRICKS);
		place(h, ModBlocks.IRON_DOOR_BLOCK.asItem(), new Vec3(2.5, 1, 6.5), SECRET.north(), Direction.SOUTH, new Vec3(2.5, 1.5, 3.0));
		checkBlock(h, SECRET, ModBlocks.IRON_DOOR_BLOCK);
		checkFaces(h, SECRET, Blocks.BRICKS);
		checkFaces(h, SECRET.above(), Blocks.BRICKS);
		BlockPos trapdoor = new BlockPos(5, 1, 3);
		h.setBlock(trapdoor, ModBlocks.IRON_TRAPDOOR_BLOCK);
		h.setBlock(SECRET.east(), Blocks.REDSTONE_BLOCK);
		h.setBlock(trapdoor.east(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(5, () -> {
			check(h, open(h, SECRET), "the iron door should open with redstone");
			check(h, open(h, trapdoor), "the iron trapdoor should open with redstone");
			checkFaces(h, SECRET, Blocks.BRICKS);
			checkFaces(h, SECRET.above(), Blocks.BRICKS);
			h.setBlock(SECRET.east(), Blocks.AIR);
			h.setBlock(trapdoor.east(), Blocks.AIR);
		});
		h.runAfterDelay(10, () -> {
			check(h, !open(h, SECRET), "the iron door should close without redstone");
			check(h, !open(h, trapdoor), "the iron trapdoor should close without redstone");
			checkFaces(h, SECRET, Blocks.BRICKS);
			checkFaces(h, SECRET.above(), Blocks.BRICKS);
			h.succeed();
		});
	}

	/**
	 * Iron material, as vanilla's (iron open and close sounds, no hand use),
	 * but no tool needed to drop: how long it takes to mine follows its look.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void ironButDropsWithoutATool(GameTestHelper h) {
		for (Block block : new Block[] {ModBlocks.IRON_DOOR_BLOCK, ModBlocks.IRON_TRAPDOOR_BLOCK}) {
			BlockState secret = block.defaultBlockState();
			check(h, !secret.requiresCorrectToolForDrops(), block + " should drop without a tool");
			check(h, !secret.is(BlockTags.MINEABLE_WITH_PICKAXE), block + " should not be tagged for a pickaxe");
			check(h, secret.getMaterial() == Material.METAL, block + " should be metal (iron open/close sounds, no hand use)");
		}
		h.succeed();
	}

	/** Broken with a pickaxe or by hand, it drops itself. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void dropsWithOrWithoutAPickaxe(GameTestHelper h) {
		floor(h);
		ServerPlayer player = player(h, new Vec3(3.5, 1, 6.5), new Vec3(3.5, 1.5, 3.5));
		player.setGameMode(GameType.SURVIVAL);
		for (Block block : new Block[] {ModBlocks.IRON_DOOR_BLOCK, ModBlocks.IRON_TRAPDOOR_BLOCK}) {
			for (boolean pickaxe : new boolean[] {true, false}) {
				if (block == ModBlocks.IRON_DOOR_BLOCK) {
					door(h, SECRET, block);
				} else {
					h.setBlock(SECRET, block);
				}
				player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe ? new ItemStack(Items.IRON_PICKAXE) : ItemStack.EMPTY);
				player.gameMode.destroyBlock(h.absolutePos(SECRET));
				AABB around = new AABB(h.absolutePos(SECRET)).inflate(2);
				boolean dropped = !h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(block.asItem())).isEmpty();
				check(h, dropped, block + (pickaxe ? " should drop with a pickaxe" : " should drop by hand"));
				h.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(ItemEntity::discard);
				h.setBlock(SECRET, Blocks.AIR);
				h.setBlock(SECRET.above(), Blocks.AIR);
			}
		}
		// Breaking the upper door half with a pickaxe drops exactly one door (the
		// loot table drops only from the lower half, which goes too).
		door(h, SECRET, ModBlocks.IRON_DOOR_BLOCK);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
		player.gameMode.destroyBlock(h.absolutePos(SECRET.above()));
		AABB around = new AABB(h.absolutePos(SECRET)).inflate(2);
		int doors = h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(ModBlocks.IRON_DOOR_BLOCK.asItem()))
				.stream().mapToInt(e -> e.getItem().getCount()).sum();
		check(h, doors == 1, "breaking the upper half should drop exactly one door, got " + doors);
		checkBlock(h, SECRET, Blocks.AIR);
		h.succeed();
	}

	/** Dummy Block + iron door / iron trapdoor. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void recipesWork(GameTestHelper h) {
		Optional<ItemStack> door = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.IRON_DOOR);
		Optional<ItemStack> trapdoor = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.IRON_TRAPDOOR);
		check(h, door.isPresent() && door.get().is(ModBlocks.IRON_DOOR_BLOCK.asItem()), "Dummy Block + iron door, got " + door);
		check(h, trapdoor.isPresent() && trapdoor.get().is(ModBlocks.IRON_TRAPDOOR_BLOCK.asItem()), "Dummy Block + iron trapdoor, got " + trapdoor);
		h.succeed();
	}
}
