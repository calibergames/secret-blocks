package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.WeightedPressurePlate;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Secret Light and Heavy Weighted Pressure Plates against vanilla's weighted
 * plates: the same signal for the same number of entities on them. Stone
 * floor at y = 0; the plates at y = 1.
 */
public class WeightedPlateGameTests implements FabricGameTest {

	private static final BlockPos PLATE = new BlockPos(3, 1, 3);

	/** Twenty different items, so the dropped ones never merge into fewer entities. */
	private static final Item[] ITEMS = {
			Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT, Items.COAL, Items.STICK, Items.APPLE,
			Items.BONE, Items.FEATHER, Items.FLINT, Items.STRING, Items.PAPER, Items.BOOK, Items.BRICK, Items.CLAY_BALL,
			Items.WHEAT, Items.SUGAR, Items.EGG, Items.ARROW, Items.BOWL};

	private static int power(GameTestHelper h, BlockPos pos) {
		return h.getBlockState(pos).getValue(BlockStateProperties.POWER);
	}

	/** Vanilla's formula: min(n, max) / max × 15, rounded up; 0 for nobody. */
	private static int expected(int count, int maxWeight) {
		int n = Math.min(count, maxWeight);
		return n > 0 ? (int) Math.ceil((float) n / maxWeight * 15.0F) : 0;
	}

	private static List<ItemEntity> drop(GameTestHelper h, BlockPos onTopOf, int count) {
		List<ItemEntity> items = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			ItemEntity item = h.spawnItem(ITEMS[i], onTopOf.getX() + 0.5f, onTopOf.getY() + 1.2f, onTopOf.getZ() + 0.5f);
			item.setDeltaMovement(Vec3.ZERO);
			items.add(item);
		}
		return items;
	}

	/**
	 * 1, 5, 15 and 20 dropped items on each secret plate and on its vanilla
	 * twin: the same signal, and vanilla's value. Stations are two blocks
	 * apart, so items on different plates never merge either.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void droppedItemsGiveTheVanillaSignal(GameTestHelper h) {
		floor(h);
		int[] counts = {1, 5, 15, 20};
		Object[][] rows = {
				{ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, 15, 0},
				{ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, 150, 4}};
		List<Runnable> checks = new ArrayList<>();
		for (Object[] row : rows) {
			Block secret = (Block) row[0];
			Block vanilla = (Block) row[1];
			int maxWeight = (int) row[2];
			int z = (int) row[3];
			for (int i = 0; i < counts.length; i++) {
				int count = counts[i];
				BlockPos secretPos = new BlockPos(i * 2, 1, z);
				BlockPos vanillaPos = new BlockPos(i * 2, 1, z + 2);
				h.setBlock(secretPos, secret);
				h.setBlock(vanillaPos, vanilla);
				drop(h, secretPos, count);
				drop(h, vanillaPos.below(), count);
				String label = secret.getName().getString() + " x" + count;
				checks.add(() -> {
					check(h, power(h, vanillaPos) == expected(count, maxWeight),
							label + ": vanilla gave " + power(h, vanillaPos) + ", the formula " + expected(count, maxWeight));
					check(h, power(h, secretPos) == power(h, vanillaPos),
							label + ": secret " + power(h, secretPos) + ", vanilla " + power(h, vanillaPos));
				});
			}
		}
		h.succeedWhen(() -> checks.forEach(Runnable::run));
	}

	/** Mobs and players count; it releases once they are gone. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void mobsAndPlayersCountAndItReleases(GameTestHelper h) {
		floor(h);
		BlockPos pigPlate = PLATE.west(2);
		BlockPos playerPlate = PLATE.east(2);
		h.setBlock(pigPlate, ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
		h.setBlock(playerPlate, ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
		var pig = h.spawnWithNoFreeWill(EntityType.PIG, pigPlate.above());
		Player player = h.makeMockPlayer();
		Vec3 feet = abs(h, Vec3.atBottomCenterOf(playerPlate.above()).add(0, 0.5, 0));
		player.moveTo(feet.x, feet.y, feet.z, 0, 0);
		h.getLevel().addFreshEntity(player);
		h.runAfterDelay(30, () -> {
			check(h, power(h, pigPlate) == 1, "a pig on the light plate gives 1, got " + power(h, pigPlate));
			check(h, power(h, playerPlate) == 1, "a player on the heavy plate gives 1, got " + power(h, playerPlate));
			pig.discard();
			player.discard();
			h.succeedWhen(() -> {
				check(h, power(h, pigPlate) == 0, "the light plate should release");
				check(h, power(h, playerPlate) == 0, "the heavy plate should release");
			});
		});
	}

	/**
	 * Weak power only: it lights the lamps beside and directly under it, but
	 * the block under it is not powered through (a lamp beside that block,
	 * placed after the press and given an update, stays off).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void weakPowerOnly(GameTestHelper h) {
		floor(h);
		BlockPos plate = PLATE.above();
		h.setBlock(plate, ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
		h.setBlock(plate.east(), Blocks.REDSTONE_LAMP);
		h.setBlock(PLATE, Blocks.REDSTONE_LAMP);
		BlockPos stoneBelow = PLATE.east(2).above();
		drop(h, plate, 3);
		h.runAfterDelay(30, () -> {
			check(h, power(h, plate) == 3, "three items give 3, got " + power(h, plate));
			check(h, h.getBlockState(plate.east()).getValue(RedstoneLampBlock.LIT), "the lamp beside should be lit");
			check(h, h.getBlockState(PLATE).getValue(RedstoneLampBlock.LIT), "the lamp under it should be lit");
			// The block under the plate: a stone, with a lamp beside it.
			h.setBlock(PLATE, Blocks.STONE);
			h.setBlock(PLATE.west(), Blocks.REDSTONE_LAMP);
			h.getLevel().updateNeighborsAt(h.absolutePos(PLATE), Blocks.STONE);
			check(h, !h.getBlockState(PLATE.west()).getValue(RedstoneLampBlock.LIT), "the block under the plate must not be powered through");
			h.succeed();
		});
	}

	/** Pressing changes the block state; the disguise placed with it stays. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void keepsItsDisguiseWhenPressed(GameTestHelper h) {
		floor(h);
		h.setBlock(PLATE.north(), Blocks.BRICKS);
		place(h, ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE.asItem(), new Vec3(3.5, 1, 6.5), PLATE.north(), Direction.SOUTH, new Vec3(3.5, 1.5, 3.0));
		checkBlock(h, PLATE, ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
		drop(h, PLATE, 11);
		h.succeedWhen(() -> {
			check(h, power(h, PLATE) == 2, "eleven items on the heavy plate give 2, got " + power(h, PLATE));
			checkFaces(h, PLATE, Blocks.BRICKS);
		});
	}

	/** While pressed it counts again every 10 ticks: more items raise the signal. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 160)
	public void countsAgainWhilePressed(GameTestHelper h) {
		floor(h);
		h.setBlock(PLATE, ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
		List<ItemEntity> first = drop(h, PLATE, 2);
		h.runAfterDelay(30, () -> {
			check(h, power(h, PLATE) == 2, "two items give 2, got " + power(h, PLATE));
			for (int i = 2; i < 5; i++) {
				ItemEntity item = h.spawnItem(ITEMS[i], PLATE.getX() + 0.5f, PLATE.getY() + 1.2f, PLATE.getZ() + 0.5f);
				item.setDeltaMovement(Vec3.ZERO);
			}
			h.succeedWhen(() -> check(h, power(h, PLATE) == 5, "five items give 5, got " + power(h, PLATE) + " (" + first.size() + " first)"));
		});
	}

	/**
	 * No secret plate is a redstone conductor, like the vanilla plates: strong
	 * power does not pass through it (the Secret Pressure Plate and Player
	 * Pressure Plate too, since 2.0.0).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void notARedstoneConductor(GameTestHelper h) {
		floor(h);
		for (Block block : new Block[] {ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE, ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
				ModBlocks.PRESSURE_PLATE, ModBlocks.PLAYER_PRESSURE_PLATE,
				Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE}) {
			h.setBlock(PLATE, block);
			check(h, !h.getBlockState(PLATE).isRedstoneConductor(h.getLevel(), h.absolutePos(PLATE)), block + " must not be a conductor");
		}
		// A repeater feeding a plate does not light a lamp on its far side.
		BlockPos second = PLATE.south(2);
		h.setBlock(PLATE, ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
		h.setBlock(second, ModBlocks.PRESSURE_PLATE);
		for (BlockPos plate : new BlockPos[] {PLATE, second}) {
			h.setBlock(plate.west(2), Blocks.REDSTONE_BLOCK);
			h.setBlock(plate.west(), Blocks.REPEATER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
			h.setBlock(plate.east(), Blocks.REDSTONE_LAMP);
		}
		h.runAfterDelay(6, () -> {
			check(h, !h.getBlockState(PLATE.east()).getValue(RedstoneLampBlock.LIT), "a repeater must not power through the weighted plate");
			check(h, !h.getBlockState(second.east()).getValue(RedstoneLampBlock.LIT), "a repeater must not power through the Secret Pressure Plate");
			h.succeed();
		});
	}

	/** A piston breaks it, as it breaks the vanilla plate beside it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPistonBreaksItAsVanilla(GameTestHelper h) {
		floor(h);
		Block[] plates = {ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
				ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE};
		for (int i = 0; i < plates.length; i++) {
			BlockPos piston = new BlockPos(0, 1, i * 2);
			h.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
			h.setBlock(piston.east(), plates[i]);
		}
		// Powered from the floor block under each piston.
		h.runAfterDelay(2, () -> {
			for (int i = 0; i < plates.length; i++) {
				h.setBlock(new BlockPos(0, 0, i * 2), Blocks.REDSTONE_BLOCK);
			}
		});
		h.runAfterDelay(8, () -> {
			for (int i = 0; i < plates.length; i++) {
				BlockPos at = new BlockPos(1, 1, i * 2);
				check(h, !h.getBlockState(at).is(plates[i]) && !h.getBlockState(at.east()).is(plates[i]), plates[i] + " should be broken");
			}
			h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO),
					h.absolutePos(new BlockPos(8, 4, 8))).inflate(2)).forEach(ItemEntity::discard);
			h.succeed();
		});
	}

	/** Dummy Block + the vanilla plate. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void craftedFromADummyBlockAndTheVanillaPlate(GameTestHelper h) {
		check(h, craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
						.map(s -> s.is(ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE.asItem())).orElse(false),
				"Dummy Block + light weighted plate should make the secret one");
		check(h, craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
						.map(s -> s.is(ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE.asItem())).orElse(false),
				"Dummy Block + heavy weighted plate should make the secret one");
		check(h, ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE instanceof WeightedPressurePlate, "the light plate is a weighted plate");
		h.succeed();
	}
}
