package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.PressurePlate;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Secret Pressure Plate (any entity) and Secret Player Pressure Plate
 * (players only), full disguised floor blocks. Layout: stone floor at y = 0,
 * the plate at {@link #PLATE} on top of it, entities arrive at y = 2.
 */
public class PressurePlateGameTests {

	private static final BlockPos PLATE = new BlockPos(3, 1, 3);
	private static final BlockPos LAMP_BESIDE = PLATE.east();
	private static final BlockPos LAMP_BELOW = PLATE.below();

	private static void plate(GameTestHelper h, Block plate) {
		floor(h);
		h.setBlock(PLATE, plate);
		h.setBlock(LAMP_BESIDE, Blocks.REDSTONE_LAMP);
		h.setBlock(LAMP_BELOW, Blocks.REDSTONE_LAMP);
	}

	private static boolean powered(GameTestHelper h) {
		return h.getBlockState(PLATE).getValue(PressurePlate.POWERED);
	}

	private static boolean lit(GameTestHelper h, BlockPos lamp) {
		return h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT);
	}

	/** A mob landing on it presses it; it powers the lamps beside and below it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void mobPressesPlate(GameTestHelper h) {
		plate(h, ModBlocks.PRESSURE_PLATE);
		h.spawnWithNoFreeWill(EntityType.PIG, PLATE.above());
		h.succeedWhen(() -> {
			check(h, powered(h), "plate should be pressed");
			check(h, lit(h, LAMP_BESIDE), "lamp beside the plate should be lit");
			check(h, lit(h, LAMP_BELOW), "lamp below the plate should be lit");
		});
	}

	/** Like the original's Secret Pressure Plate, any entity counts, dropped items too. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void droppedItemPressesPlate(GameTestHelper h) {
		plate(h, ModBlocks.PRESSURE_PLATE);
		h.spawnItem(Items.DIAMOND, 3.5f, 2.2f, 3.5f);
		h.succeedWhen(() -> check(h, powered(h), "a dropped item should press the plate"));
	}

	/** Released within about a second of the last entity leaving; lamps go off. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void releasesAfterMobLeaves(GameTestHelper h) {
		plate(h, ModBlocks.PRESSURE_PLATE);
		Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, PLATE.above());
		h.runAfterDelay(30, () -> {
			check(h, powered(h), "plate should be pressed while the pig stands on it");
			pig.discard();
			h.succeedWhen(() -> {
				check(h, !powered(h), "plate should release after the pig is gone");
				check(h, !lit(h, LAMP_BESIDE), "lamp beside should turn off");
			});
		});
	}

	/**
	 * Weak power only: the stone under the plate is not powered through. The
	 * lamp beside that stone is placed after the plate is pressed and then
	 * given a neighbor update, so it reads the stone's power directly (a
	 * powered plate does not update the stone's neighbors itself).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void doesNotPowerThroughTheBlockBelow(GameTestHelper h) {
		plate(h, ModBlocks.PRESSURE_PLATE);
		h.setBlock(LAMP_BELOW, Blocks.STONE);
		BlockPos lampBesideStone = LAMP_BELOW.west();
		h.spawnWithNoFreeWill(EntityType.PIG, PLATE.above());
		h.runAfterDelay(30, () -> {
			check(h, powered(h), "plate should be pressed");
			h.setBlock(lampBesideStone, Blocks.REDSTONE_LAMP);
			h.getLevel().updateNeighborsAt(h.absolutePos(LAMP_BELOW), Blocks.STONE);
			check(h, !lit(h, lampBesideStone), "the block under the plate must not be strongly powered");
			h.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void playerPlateIgnoresMobsAndItems(GameTestHelper h) {
		plate(h, ModBlocks.PLAYER_PRESSURE_PLATE);
		h.spawnWithNoFreeWill(EntityType.PIG, PLATE.above());
		h.spawnItem(Items.DIAMOND, 3.5f, 2.2f, 3.5f);
		h.runAfterDelay(40, () -> {
			check(h, !powered(h), "the player plate must ignore mobs and items");
			h.succeed();
		});
	}

	/**
	 * A player landing on either plate presses it; it releases after they
	 * leave. Uses the game-test mock player (a plain {@code Player} with no
	 * network connection), added to the level so it falls and steps on the
	 * plate like any entity.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void playerPressesPlayerPlate(GameTestHelper h) {
		playerSteps(h, ModBlocks.PLAYER_PRESSURE_PLATE, false);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void playerPressesPlate(GameTestHelper h) {
		playerSteps(h, ModBlocks.PRESSURE_PLATE, false);
	}

	/** Like the original, there is no sneak check: sneaking players press it too. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void sneakingPlayerPressesPlate(GameTestHelper h) {
		playerSteps(h, ModBlocks.PRESSURE_PLATE, true);
	}

	private static void playerSteps(GameTestHelper h, Block block, boolean sneaking) {
		plate(h, block);
		Player player = h.makeMockPlayer();
		player.setShiftKeyDown(sneaking);
		Vec3 feet = abs(h, new Vec3(3.5, 2.5, 3.5));
		player.moveTo(feet.x, feet.y, feet.z, 0, 0);
		h.getLevel().addFreshEntity(player);
		h.runAfterDelay(30, () -> {
			check(h, powered(h), "a player standing on the plate should press it");
			player.discard();
			h.succeedWhen(() -> check(h, !powered(h), "plate should release after the player is gone"));
		});
	}

	/** A full, solid, drawn block, placed through the item with the floor's disguise. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void placedAsDisguisedFloor(GameTestHelper h) {
		floor(h);
		h.setBlock(PLATE.north(), Blocks.BRICKS);
		place(h, ModBlocks.PRESSURE_PLATE.asItem(), new Vec3(3.5, 1, 6.5), PLATE.north(), Direction.SOUTH, new Vec3(3.5, 1.5, 3.0));
		checkBlock(h, PLATE, ModBlocks.PRESSURE_PLATE);
		checkFaces(h, PLATE, Blocks.BRICKS);
		BlockState state = h.getBlockState(PLATE);
		check(h, state.getRenderShape() == RenderShape.MODEL, "plate should be drawn with its disguise");
		check(h, state.isCollisionShapeFullBlock(h.getLevel(), h.absolutePos(PLATE)), "plate should be a full solid block");
		h.succeed();
	}

	/** Pressing changes the block state; the disguise must survive it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void keepsDisguiseWhenPressed(GameTestHelper h) {
		floor(h);
		h.setBlock(PLATE.north(), Blocks.BRICKS);
		place(h, ModBlocks.PRESSURE_PLATE.asItem(), new Vec3(3.5, 1, 6.5), PLATE.north(), Direction.SOUTH, new Vec3(3.5, 1.5, 3.0));
		h.spawnWithNoFreeWill(EntityType.PIG, PLATE.above());
		h.succeedWhen(() -> {
			check(h, powered(h), "plate should be pressed");
			checkFaces(h, PLATE, Blocks.BRICKS);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void dropsItselfWhenBroken(GameTestHelper h) {
		floor(h);
		for (Block block : new Block[] {ModBlocks.PRESSURE_PLATE, ModBlocks.PLAYER_PRESSURE_PLATE}) {
			h.setBlock(PLATE, block);
			h.getLevel().destroyBlock(h.absolutePos(PLATE), true);
			checkBlock(h, PLATE, Blocks.AIR);
			boolean dropped = !h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(PLATE)).inflate(1.5),
					e -> e.getItem().is(block.asItem())).isEmpty();
			check(h, dropped, block + " should drop itself");
		}
		h.succeed();
	}

	private static void checkCrafts(GameTestHelper h, Item expected, Item... items) {
		Optional<ItemStack> result = craft(h, items);
		check(h, result.isPresent() && result.get().is(expected),
				java.util.Arrays.toString(items) + " should craft " + expected + ", got " + result);
	}

	/** Dummy Block + any wooden plate → Secret Pressure Plate; + stone plate → Secret Player Pressure Plate. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void recipesWork(GameTestHelper h) {
		Item dummy = ModBlocks.SOLID_BLOCK.asItem();
		checkCrafts(h, ModBlocks.PRESSURE_PLATE.asItem(), dummy, Items.OAK_PRESSURE_PLATE);
		checkCrafts(h, ModBlocks.PRESSURE_PLATE.asItem(), dummy, Items.CRIMSON_PRESSURE_PLATE);
		checkCrafts(h, ModBlocks.PLAYER_PRESSURE_PLATE.asItem(), dummy, Items.STONE_PRESSURE_PLATE);
		h.succeed();
	}
}
