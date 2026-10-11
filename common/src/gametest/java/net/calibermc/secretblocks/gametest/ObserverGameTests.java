package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Observer behaves like a vanilla observer and keeps its
 * disguise. Layout: stone floor at y = 0; the observer at {@link #OBSERVER}
 * faces north, so it watches {@link #FRONT} and outputs into {@link #BACK}.
 */
public class ObserverGameTests {

	private static final BlockPos OBSERVER = new BlockPos(3, 1, 3);
	private static final BlockPos FRONT = OBSERVER.north();
	private static final BlockPos BACK = OBSERVER.south();

	private static BlockState observerFacingNorth() {
		return ModBlocks.SECRET_OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.NORTH);
	}

	private static boolean powered(GameTestHelper h) {
		return h.getBlockState(OBSERVER).getValue(ObserverBlock.POWERED);
	}

	/** Counts rising edges of the observer's POWERED property, every tick. */
	private static int[] countPulses(GameTestHelper h) {
		int[] pulses = {0};
		boolean[] last = {false};
		h.onEachTick(() -> {
			boolean now = h.getBlockState(OBSERVER).is(ModBlocks.SECRET_OBSERVER) && powered(h);
			if (now && !last[0]) {
				pulses[0]++;
			}
			last[0] = now;
		});
		return pulses;
	}

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	/** Placed through the item, it faces the same way a vanilla observer would. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void facingMatchesVanilla(GameTestHelper h) {
		floor(h);
		// Level look (horizontal facing) and a steep look (facing down).
		place(h, ModBlocks.SECRET_OBSERVER.asItem(), new Vec3(1.5, 1, 6.5), new BlockPos(1, 0, 3), Direction.UP, new Vec3(1.5, 1.0, 3.5));
		place(h, Items.OBSERVER, new Vec3(5.5, 1, 6.5), new BlockPos(5, 0, 3), Direction.UP, new Vec3(5.5, 1.0, 3.5));
		place(h, ModBlocks.SECRET_OBSERVER.asItem(), new Vec3(1.5, 1, 1.9), new BlockPos(1, 0, 1), Direction.UP, new Vec3(1.5, 1.0, 1.5));
		place(h, Items.OBSERVER, new Vec3(5.5, 1, 1.9), new BlockPos(5, 0, 1), Direction.UP, new Vec3(5.5, 1.0, 1.5));
		checkBlock(h, new BlockPos(1, 1, 3), ModBlocks.SECRET_OBSERVER);
		checkBlock(h, new BlockPos(1, 1, 1), ModBlocks.SECRET_OBSERVER);
		for (int z : new int[] {3, 1}) {
			Direction secret = h.getBlockState(new BlockPos(1, 1, z)).getValue(ObserverBlock.FACING);
			Direction vanilla = h.getBlockState(new BlockPos(5, 1, z)).getValue(ObserverBlock.FACING);
			check(h, secret == vanilla, "secret observer faces " + secret + ", vanilla faces " + vanilla);
		}
		check(h, h.getBlockState(new BlockPos(1, 1, 1)).getValue(ObserverBlock.FACING) == Direction.DOWN,
				"a steep look should give a downward-facing observer");
		checkFaces(h, new BlockPos(1, 1, 3), Blocks.STONE);
		h.succeed();
	}

	/** A block change in front gives exactly one short pulse out of the back. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void changeInFrontPulsesOnceOutOfTheBack(GameTestHelper h) {
		floor(h);
		h.setBlock(FRONT, Blocks.STONE);
		h.setBlock(BACK, Blocks.REDSTONE_LAMP);
		h.setBlock(OBSERVER, observerFacingNorth());
		int[] pulses = countPulses(h);
		boolean[] lampLit = {false};
		h.onEachTick(() -> lampLit[0] |= h.getBlockState(BACK).getValue(RedstoneLampBlock.LIT));
		h.runAfterDelay(5, () -> h.setBlock(FRONT, Blocks.DIRT));
		h.runAfterDelay(30, () -> {
			check(h, pulses[0] == 1, "expected one pulse, got " + pulses[0]);
			check(h, lampLit[0], "the lamp behind the observer should have lit");
			check(h, !powered(h), "the pulse should have ended");
			h.succeed();
		});
	}

	/** Changing a disguise (the watched block's or the observer's own) is not a block change. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void disguiseChangesDoNotTrigger(GameTestHelper h) {
		floor(h);
		h.setBlock(FRONT, ModBlocks.SOLID_BLOCK);
		h.setBlock(OBSERVER, observerFacingNorth());
		int[] pulses = countPulses(h);
		h.runAfterDelay(5, () -> {
			secret(h, FRONT).setDisguise(all(Blocks.BRICKS));
			secret(h, OBSERVER).setDisguise(all(Blocks.BRICKS));
		});
		h.runAfterDelay(30, () -> {
			check(h, pulses[0] == 0, "disguise changes must not pulse the observer, got " + pulses[0]);
			h.succeed();
		});
	}

	/** Pulsing changes the block state; the disguise must survive it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void keepsDisguiseThroughAPulse(GameTestHelper h) {
		floor(h);
		h.setBlock(FRONT, Blocks.STONE);
		h.setBlock(OBSERVER, observerFacingNorth());
		secret(h, OBSERVER).setDisguise(all(Blocks.BRICKS));
		int[] pulses = countPulses(h);
		h.runAfterDelay(5, () -> h.setBlock(FRONT, Blocks.DIRT));
		h.runAfterDelay(30, () -> {
			check(h, pulses[0] == 1, "expected one pulse, got " + pulses[0]);
			checkFaces(h, OBSERVER, Blocks.BRICKS);
			h.succeed();
		});
	}

	/** Breaking it drops it and removes its block entity. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void dropsItselfAndLeavesNoBlockEntity(GameTestHelper h) {
		floor(h);
		h.setBlock(OBSERVER, observerFacingNorth());
		h.getLevel().destroyBlock(h.absolutePos(OBSERVER), true);
		checkBlock(h, OBSERVER, Blocks.AIR);
		check(h, h.getBlockEntity(OBSERVER) == null, "a block entity was left behind: " + h.getBlockEntity(OBSERVER));
		boolean dropped = !h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(OBSERVER)).inflate(1.5),
				e -> e.getItem().is(ModBlocks.SECRET_OBSERVER.asItem())).isEmpty();
		check(h, dropped, "the observer should drop itself");
		h.succeed();
	}

	/**
	 * A piston pushes it as it pushes a vanilla observer, and it keeps its
	 * disguise.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPistonPushesItAsAVanillaObserver(GameTestHelper h) {
		floor(h);
		h.setBlock(OBSERVER, observerFacingNorth());
		secret(h, OBSERVER).setDisguise(java.util.Map.of(Direction.NORTH, Blocks.BRICKS.defaultBlockState(), Direction.SOUTH, Blocks.BRICKS.defaultBlockState()));
		BlockPos piston = BACK;
		h.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH));
		BlockPos vanilla = OBSERVER.east(3);
		BlockPos vanillaPiston = piston.east(3);
		h.setBlock(vanilla, Blocks.OBSERVER.defaultBlockState().setValue(net.minecraft.world.level.block.ObserverBlock.FACING, Direction.NORTH));
		h.setBlock(vanillaPiston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH));
		h.setBlock(piston.below(), Blocks.REDSTONE_BLOCK);
		h.setBlock(vanillaPiston.below(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(10, () -> {
			checkBlock(h, vanilla.north(), Blocks.OBSERVER);
			checkBlock(h, OBSERVER.north(), ModBlocks.SECRET_OBSERVER);
			check(h, secret(h, OBSERVER.north()).getState(Direction.NORTH).is(Blocks.BRICKS), "it should keep its disguise");
			h.setBlock(piston.below(), Blocks.STONE);
			h.setBlock(vanillaPiston.below(), Blocks.STONE);
			h.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void recipeWorks(GameTestHelper h) {
		AbstractContainerMenu menu = new AbstractContainerMenu(null, 0) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player player) {
				return true;
			}
		};
		CraftingContainer grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
		grid.setItem(0, new ItemStack(ModBlocks.SOLID_BLOCK));
		grid.setItem(1, new ItemStack(Items.OBSERVER));
		Optional<ItemStack> result = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel())
				.map((CraftingRecipe recipe) -> recipe.assemble(grid, h.getLevel().registryAccess()));
		Item expected = ModBlocks.SECRET_OBSERVER.asItem();
		check(h, result.isPresent() && result.get().is(expected), "Dummy Block + observer should craft a Secret Observer, got " + result);
		h.succeed();
	}
}
