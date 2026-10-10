package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Clamber, a disguised, walk-through, climbable block like the
 * original mod's. A Ghost Block column is the control: same block class, not
 * climbable.
 */
public class ClamberGameTests implements FabricGameTest {

	private static final int COLUMN_HEIGHT = 6;

	private static void column(GameTestHelper h, int x, Block block) {
		for (int y = 1; y <= COLUMN_HEIGHT; y++) {
			h.setBlock(x, y, 3, block);
		}
	}

	/** A jumping player at the foot of the column; returns it so its height can be read. */
	private static Player jumperIn(GameTestHelper h, int x) {
		Player player = h.makeMockPlayer();
		Vec3 feet = abs(h, new Vec3(x + 0.5, 1, 3.5));
		player.moveTo(feet.x, feet.y, feet.z, 0, 0);
		player.setJumping(true);
		h.getLevel().addFreshEntity(player);
		return player;
	}

	private static double heightAboveFloor(GameTestHelper h, Player player) {
		return player.getY() - h.absolutePos(BlockPos.ZERO).getY() - 1;
	}

	/**
	 * Holding jump, a player climbs a Clamber column like a ladder; in a Ghost
	 * Block column the same player only jumps (at most about 1.25 blocks).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void playerClimbsAClamberColumn(GameTestHelper h) {
		floor(h);
		column(h, 2, ModBlocks.SECRET_CLAMBER);
		column(h, 5, ModBlocks.GHOST_BLOCK);
		Player climber = jumperIn(h, 2);
		Player jumper = jumperIn(h, 5);
		double[] jumperMax = {0};
		h.onEachTick(() -> jumperMax[0] = Math.max(jumperMax[0], heightAboveFloor(h, jumper)));
		h.runAfterDelay(40, () -> {
			double climbed = heightAboveFloor(h, climber);
			climber.discard();
			jumper.discard();
			check(h, climbed > 3.0, "the player should climb the Clamber column, reached " + climbed);
			check(h, jumperMax[0] < 1.5, "in a Ghost Block column the player should only jump, reached " + jumperMax[0]);
			h.succeed();
		});
	}

	/** Climbable through the vanilla tag, so it works the same on every loader. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void climbableThroughTheVanillaTag(GameTestHelper h) {
		check(h, ModBlocks.SECRET_CLAMBER.defaultBlockState().is(BlockTags.CLIMBABLE), "Secret Clamber is in #minecraft:climbable");
		check(h, !ModBlocks.GHOST_BLOCK.defaultBlockState().is(BlockTags.CLIMBABLE), "Ghost Block is not climbable");
		h.succeed();
	}

	/** Placed through the item: copies the clicked block, walk-through, drawn with its disguise. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void placedDisguisedAndWalkThrough(GameTestHelper h) {
		floor(h);
		BlockPos target = new BlockPos(3, 1, 2);
		BlockPos placed = target.south();
		h.setBlock(target, Blocks.BRICKS);
		place(h, ModBlocks.SECRET_CLAMBER.asItem(), new Vec3(3.5, 1, 6.5), target, Direction.SOUTH, new Vec3(3.5, 1.5, 3.0));
		checkBlock(h, placed, ModBlocks.SECRET_CLAMBER);
		checkFaces(h, placed, Blocks.BRICKS);
		check(h, h.getBlockState(placed).getCollisionShape(h.getLevel(), h.absolutePos(placed)).isEmpty(), "no collision");
		check(h, !h.getBlockState(placed).isSuffocating(h.getLevel(), h.absolutePos(placed)), "standing inside does not suffocate");
		h.succeed();
	}

	/** Ghost Block + ladder. */
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
		grid.setItem(0, new ItemStack(ModBlocks.GHOST_BLOCK));
		grid.setItem(1, new ItemStack(Items.LADDER));
		Optional<ItemStack> result = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel())
				.map((CraftingRecipe recipe) -> recipe.assemble(grid, h.getLevel().registryAccess()));
		check(h, result.isPresent() && result.get().is(ModBlocks.SECRET_CLAMBER.asItem()),
				"Ghost Block + ladder should craft a Secret Clamber, got " + result);
		h.succeed();
	}
}
