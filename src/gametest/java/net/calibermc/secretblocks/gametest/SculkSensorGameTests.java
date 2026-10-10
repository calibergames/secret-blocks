package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.SecretSculkSensorBlockEntity;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Sculk Sensor is a vanilla sculk sensor in a full disguised
 * block. Most tests set off a Secret Sculk Sensor and a vanilla one at the
 * same distance from the same vibration and compare them.
 *
 * A sensor hears 8 blocks, further than a test area, so it would hear the
 * tests running beside it (a block placed, an item landing). Every test that
 * waits for a vibration runs in a batch of its own; the rest share one batch
 * in which nothing listens for long.
 */
public class SculkSensorGameTests implements FabricGameTest {

	/** Vanilla's frequency for each game event, the one a sensor reports. */
	private static final java.util.function.ToIntFunction<GameEvent> FREQUENCY =
			net.minecraft.world.level.gameevent.vibrations.VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT;

	private static final Block SECRET = ModBlocks.SECRET_SCULK_SENSOR;

	private static SculkSensorPhase phase(GameTestHelper h, BlockPos rel) {
		return SculkSensorBlock.getPhase(h.getBlockState(rel));
	}

	private static int power(GameTestHelper h, BlockPos rel) {
		return h.getBlockState(rel).getValue(SculkSensorBlock.POWER);
	}

	private static SculkSensorBlockEntity sensor(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof SculkSensorBlockEntity sensor) {
			return sensor;
		}
		h.fail("expected a sculk sensor block entity, found " + h.getBlockEntity(rel), rel);
		throw new AssertionError("unreachable");
	}

	private static String name(GameTestHelper h, BlockPos rel) {
		return h.getBlockState(rel).is(SECRET) ? "the Secret Sculk Sensor" : "the vanilla sculk sensor";
	}

	/** A vibration at the centre of the block {@code rel}, from no entity. */
	private static void vibration(GameTestHelper h, GameEvent event, BlockPos rel) {
		h.getLevel().gameEvent(null, event, h.absolutePos(rel));
	}

	/** Checks a pair heard the same thing: both active, the same power, the expected frequency. */
	private static void bothActive(GameTestHelper h, BlockPos secret, BlockPos vanilla, int frequency) {
		for (BlockPos pos : new BlockPos[] {secret, vanilla}) {
			check(h, phase(h, pos) == SculkSensorPhase.ACTIVE, name(h, pos) + " should be active, is " + phase(h, pos));
			check(h, sensor(h, pos).getLastVibrationFrequency() == frequency,
					name(h, pos) + " should have heard frequency " + frequency + ", heard " + sensor(h, pos).getLastVibrationFrequency());
		}
		check(h, power(h, secret) == power(h, vanilla), "at the same distance the Secret Sculk Sensor should give " + power(h, vanilla) + ", gives " + power(h, secret));
	}

	/** All the experience an orb holds: equal orbs that land together merge into one that counts several times. */
	private static int experienceIn(ExperienceOrb orb) {
		CompoundTag saved = orb.saveWithoutId(new CompoundTag());
		return saved.getShort("Value") * Math.max(1, saved.getInt("Count"));
	}

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	/**
	 * A block placed 3 blocks away sets both off: power 10 (15 less 15/8 per
	 * block, rounded down), the block-place frequency, which the comparator
	 * behind each passes to a lamp.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_place")
	public void aPlacedBlockSetsItOff(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(1, 1, 2);
		BlockPos vanilla = new BlockPos(7, 1, 2);
		for (BlockPos pos : new BlockPos[] {secret, vanilla}) {
			h.setBlock(pos.south(2), Blocks.REDSTONE_LAMP);
			h.setBlock(pos.south(), Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
		}
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.SCULK_SENSOR);
		h.runAfterDelay(5, () -> place(h, Items.STONE, new Vec3(4.5, 1, 5.5), new BlockPos(4, 0, 2), Direction.UP, new Vec3(4.5, 1.0, 2.5)));
		h.runAfterDelay(20, () -> {
			checkBlock(h, new BlockPos(4, 1, 2), Blocks.STONE);
			int placed = FREQUENCY.applyAsInt(GameEvent.BLOCK_PLACE);
			check(h, placed == 13, "control: Minecraft 1.20.1's block-place frequency is 13, the table gives " + placed);
			bothActive(h, secret, vanilla, placed);
			check(h, power(h, vanilla) == 10, "control: a vanilla sensor 3 blocks away gives 10, gives " + power(h, vanilla));
			for (BlockPos pos : new BlockPos[] {secret, vanilla}) {
				int out = ((ComparatorBlockEntity) h.getBlockEntity(pos.south())).getOutputSignal();
				check(h, out == placed, "the comparator behind " + name(h, pos) + " should give " + placed + " (the frequency), gives " + out);
				check(h, h.getBlockState(pos.south(2)).getValue(RedstoneLampBlock.LIT), "the lamp behind " + name(h, pos) + "'s comparator should light");
			}
			h.succeed();
		});
	}

	/**
	 * An arrow landing between them sets both off alike, with the
	 * projectile-land frequency. It starts 12 blocks up, out of their hearing:
	 * an arrow's first tick makes a "shot" vibration where it is.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_arrow")
	public void anArrowLandingSetsItOff(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(1, 1, 2);
		BlockPos vanilla = new BlockPos(7, 1, 2);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.SCULK_SENSOR);
		List<Arrow> arrows = new ArrayList<>();
		h.runAfterDelay(5, () -> {
			Vec3 from = abs(h, new Vec3(4.5, 13.5, 2.5));
			Arrow arrow = new Arrow(h.getLevel(), from.x, from.y, from.z);
			arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
			arrow.setDeltaMovement(0, -1, 0);
			h.getLevel().addFreshEntity(arrow);
			arrows.add(arrow);
		});
		h.runAfterDelay(40, () -> {
			double floorTop = abs(h, new Vec3(0, 1, 0)).y;
			// An arrow stuck in the ground keeps its last motion, so only its height tells.
			check(h, arrows.get(0).getY() < floorTop + 0.5, "control: the arrow should have landed on the floor, it is at " + arrows.get(0).position());
			check(h, FREQUENCY.applyAsInt(GameEvent.PROJECTILE_LAND) == 2, "control: Minecraft 1.20.1's projectile-land frequency is 2");
			bothActive(h, secret, vanilla, FREQUENCY.applyAsInt(GameEvent.PROJECTILE_LAND));
			arrows.forEach(Arrow::discard);
			h.succeed();
		});
	}

	/**
	 * Wool between the vibration and a sensor blocks it, for both kinds; the
	 * open sensors at the same distance hear it.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_wool")
	public void woolBlocksIt(GameTestHelper h) {
		floor(h);
		BlockPos event = new BlockPos(4, 1, 4);
		BlockPos secretWalled = new BlockPos(2, 1, 4);
		BlockPos vanillaWalled = new BlockPos(6, 1, 4);
		BlockPos secretOpen = new BlockPos(4, 1, 7);
		BlockPos vanillaOpen = new BlockPos(4, 1, 1);
		for (BlockPos pos : new BlockPos[] {secretWalled, vanillaWalled}) {
			for (Direction side : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP}) {
				h.setBlock(pos.relative(side), Blocks.WHITE_WOOL);
			}
		}
		h.setBlock(secretWalled, SECRET);
		h.setBlock(vanillaWalled, Blocks.SCULK_SENSOR);
		h.setBlock(secretOpen, SECRET);
		h.setBlock(vanillaOpen, Blocks.SCULK_SENSOR);
		h.runAfterDelay(5, () -> vibration(h, GameEvent.BLOCK_PLACE, event));
		h.runAfterDelay(20, () -> {
			bothActive(h, secretOpen, vanillaOpen, FREQUENCY.applyAsInt(GameEvent.BLOCK_PLACE));
			for (BlockPos pos : new BlockPos[] {secretWalled, vanillaWalled}) {
				check(h, phase(h, pos) == SculkSensorPhase.INACTIVE, name(h, pos) + " behind wool should not have heard it, is " + phase(h, pos));
			}
			h.succeed();
		});
	}

	/**
	 * Tick by tick the two keep the same phase and power: active for 40 ticks,
	 * a one-tick cooldown, then listening again. A vibration while active is
	 * ignored; one after the cooldown sets them off again.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_timing", timeoutTicks = 200)
	public void timingAndCooldownMatchVanilla(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(1, 1, 2);
		BlockPos vanilla = new BlockPos(7, 1, 2);
		BlockPos event = new BlockPos(4, 1, 2);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.SCULK_SENSOR);
		List<String> secretLog = new ArrayList<>();
		List<String> vanillaLog = new ArrayList<>();
		int[] activeTicks = new int[1];
		for (int t = 1; t <= 130; t++) {
			h.runAfterDelay(t, () -> {
				secretLog.add(phase(h, secret) + "/" + power(h, secret));
				vanillaLog.add(phase(h, vanilla) + "/" + power(h, vanilla));
				if (phase(h, vanilla) == SculkSensorPhase.ACTIVE && secretLog.size() < 60) {
					activeTicks[0]++;
				}
			});
		}
		h.runAfterDelay(5, () -> vibration(h, GameEvent.BLOCK_PLACE, event));
		h.runAfterDelay(20, () -> {
			bothActive(h, secret, vanilla, FREQUENCY.applyAsInt(GameEvent.BLOCK_PLACE));
			vibration(h, GameEvent.BLOCK_ACTIVATE, event);
		});
		h.runAfterDelay(30, () -> bothActive(h, secret, vanilla, FREQUENCY.applyAsInt(GameEvent.BLOCK_PLACE)));
		h.runAfterDelay(60, () -> {
			for (BlockPos pos : new BlockPos[] {secret, vanilla}) {
				check(h, phase(h, pos) == SculkSensorPhase.INACTIVE && power(h, pos) == 0, name(h, pos) + " should be listening again with no power, is " + phase(h, pos) + "/" + power(h, pos));
			}
			vibration(h, GameEvent.BLOCK_DESTROY, event);
		});
		h.runAfterDelay(75, () -> bothActive(h, secret, vanilla, FREQUENCY.applyAsInt(GameEvent.BLOCK_DESTROY)));
		h.runAfterDelay(131, () -> {
			check(h, activeTicks[0] == SculkSensorBlock.ACTIVE_TICKS, "control: the vanilla sensor should stay active " + SculkSensorBlock.ACTIVE_TICKS + " ticks, stayed " + activeTicks[0]);
			check(h, secretLog.equals(vanillaLog), "tick by tick, phase/power should match the vanilla sensor:\n secret  " + secretLog + "\n vanilla " + vanillaLog);
			check(h, secretLog.contains(SculkSensorPhase.COOLDOWN + "/0"), "the cooldown should have been seen");
			h.succeed();
		});
	}

	/**
	 * A vibration still on its way survives saving and loading the block
	 * entity: both sensors, reloaded mid-flight, go off when it arrives. The
	 * disguise survives too. The block entity is replaced in place, not
	 * through a real chunk unload (the old one, removed, stops listening).
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_reload")
	public void aVibrationInFlightSurvivesSaveAndLoad(GameTestHelper h) {
		floor(h);
		BlockPos event = new BlockPos(1, 1, 1);
		BlockPos secret = new BlockPos(7, 1, 1);
		BlockPos vanilla = new BlockPos(1, 1, 7);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.SCULK_SENSOR);
		((SecretSculkSensorBlockEntity) sensor(h, secret)).setDisguise(all(Blocks.BRICKS));
		h.runAfterDelay(5, () -> vibration(h, GameEvent.BLOCK_PLACE, event));
		List<BlockEntity> reloaded = new ArrayList<>();
		h.runAfterDelay(8, () -> {
			for (BlockPos pos : new BlockPos[] {secret, vanilla}) {
				check(h, phase(h, pos) == SculkSensorPhase.INACTIVE, name(h, pos) + " should still be waiting for the vibration (6 blocks: 6 ticks), is " + phase(h, pos));
				CompoundTag saved = sensor(h, pos).saveWithFullMetadata();
				check(h, saved.getCompound("listener").contains("event"), name(h, pos) + " should save the vibration in flight: " + saved.get("listener"));
				BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(pos), h.getBlockState(pos), saved);
				check(h, loaded != null, name(h, pos) + " should load back");
				h.getLevel().setBlockEntity(loaded);
				reloaded.add(loaded);
			}
		});
		h.runAfterDelay(25, () -> {
			check(h, h.getBlockEntity(secret) == reloaded.get(0) && h.getBlockEntity(vanilla) == reloaded.get(1), "the loaded block entities should be the ones in the world");
			check(h, reloaded.get(0) instanceof SecretSculkSensorBlockEntity, "it should load as a Secret Sculk Sensor, got " + reloaded.get(0));
			bothActive(h, secret, vanilla, FREQUENCY.applyAsInt(GameEvent.BLOCK_PLACE));
			check(h, power(h, vanilla) == 4, "control: a vanilla sensor 6 blocks away gives 4, gives " + power(h, vanilla));
			checkFaces(h, secret, Blocks.BRICKS);
			h.succeed();
		});
	}

	/**
	 * A pig dropped onto each lands on it and sets it off at full power (the
	 * block's {@code stepOn}, reached through the pig's own movement, hears it
	 * from where it stands). Its step and its landing reach the sensor at
	 * once from the same distance, and the higher frequency, the landing's,
	 * is the one heard. A warden stepping on either does nothing.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_step")
	public void landingOnItSetsItOff(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(1, 1, 2);
		BlockPos vanilla = new BlockPos(7, 1, 2);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.SCULK_SENSOR);
		// Wool between them, so each hears only the pig on it: the first
		// vibration a sensor hears wins, and the pig on the taller Secret
		// Sculk Sensor lands a tick sooner.
		for (int y = 1; y <= 4; y++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(4, y, z, Blocks.WHITE_WOOL);
			}
		}
		// A warden first: vanilla's step ignores it.
		Warden warden = EntityType.WARDEN.create(h.getLevel());
		for (BlockPos pos : new BlockPos[] {secret, vanilla}) {
			BlockState state = h.getBlockState(pos);
			state.getBlock().stepOn(h.getLevel(), h.absolutePos(pos), state, warden);
			check(h, phase(h, pos) == SculkSensorPhase.INACTIVE, "a warden stepping on " + name(h, pos) + " should not set it off");
		}
		List<Pig> pigs = new ArrayList<>();
		h.runAfterDelay(5, () -> {
			for (BlockPos pos : new BlockPos[] {secret, vanilla}) {
				Pig pig = EntityType.PIG.create(h.getLevel());
				Vec3 above = Vec3.atBottomCenterOf(h.absolutePos(pos.above(2)));
				pig.moveTo(above.x, above.y, above.z);
				h.getLevel().addFreshEntity(pig);
				pigs.add(pig);
			}
		});
		h.runAfterDelay(25, () -> {
			check(h, FREQUENCY.applyAsInt(GameEvent.HIT_GROUND) == 2, "control: Minecraft 1.20.1's hit-ground frequency is 2");
			bothActive(h, secret, vanilla, FREQUENCY.applyAsInt(GameEvent.HIT_GROUND));
			check(h, power(h, secret) == 15 && power(h, vanilla) == 15, "landed on, both should give 15 (stepOn), give " + power(h, secret) + " and " + power(h, vanilla));
			pigs.forEach(Pig::discard);
			h.succeed();
		});
	}

	/**
	 * A block placed on top of each, by a player standing beside them, is one
	 * block away for both: power 14 and the block-place frequency. Only a mob
	 * standing on the Secret Sculk Sensor counts as at it.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_on_top")
	public void aBlockPlacedOnTopIsABlockAway(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(1, 1, 2);
		BlockPos vanilla = new BlockPos(7, 1, 2);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.SCULK_SENSOR);
		h.runAfterDelay(5, () -> {
			place(h, Items.STONE, new Vec3(1.5, 1, 4.5), secret, Direction.UP, new Vec3(1.5, 2.0, 2.5));
			place(h, Items.STONE, new Vec3(7.5, 1, 4.5), vanilla, Direction.UP, new Vec3(7.5, 1.5, 2.5));
		});
		h.runAfterDelay(20, () -> {
			checkBlock(h, secret.above(), Blocks.STONE);
			checkBlock(h, vanilla.above(), Blocks.STONE);
			bothActive(h, secret, vanilla, FREQUENCY.applyAsInt(GameEvent.BLOCK_PLACE));
			check(h, power(h, vanilla) == 14, "control: a block placed on a vanilla sensor gives 14, gives " + power(h, vanilla));
			h.succeed();
		});
	}

	/**
	 * Placed in water it is not waterlogged (vanilla's is) and holds no water;
	 * a bucket cannot fill it.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_misc")
	public void neverWaterlogged(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(2, 1, 3);
		BlockPos vanilla = new BlockPos(5, 1, 3);
		h.setBlock(secret, Blocks.WATER);
		h.setBlock(vanilla, Blocks.WATER);
		place(h, SECRET.asItem(), new Vec3(2.5, 1, 6.5), new BlockPos(2, 0, 3), Direction.UP, new Vec3(2.5, 1.0, 3.5));
		place(h, Items.SCULK_SENSOR, new Vec3(5.5, 1, 6.5), new BlockPos(5, 0, 3), Direction.UP, new Vec3(5.5, 1.0, 3.5));
		checkBlock(h, secret, SECRET);
		checkBlock(h, vanilla, Blocks.SCULK_SENSOR);
		check(h, h.getBlockState(vanilla).getValue(SculkSensorBlock.WATERLOGGED), "control: a vanilla sensor placed in water is waterlogged");
		check(h, !h.getBlockState(secret).getValue(SculkSensorBlock.WATERLOGGED), "the Secret Sculk Sensor placed in water should not be waterlogged");
		check(h, h.getBlockState(secret).getFluidState().isEmpty(), "the Secret Sculk Sensor should hold no water, holds " + h.getBlockState(secret).getFluidState());
		LiquidBlockContainer container = (LiquidBlockContainer) SECRET;
		check(h, !container.canPlaceLiquid(h.getLevel(), h.absolutePos(secret), h.getBlockState(secret), Fluids.WATER), "a bucket should not be able to fill it");
		// A state that has the property anyway (a command, a structure) holds no water and drops it at the next update.
		BlockPos forced = new BlockPos(2, 1, 5);
		h.setBlock(forced, SECRET.defaultBlockState().setValue(SculkSensorBlock.WATERLOGGED, true));
		check(h, h.getBlockState(forced).getFluidState().isEmpty(), "a Secret Sculk Sensor set waterlogged should still hold no water");
		h.setBlock(forced.east(), Blocks.STONE);
		check(h, !h.getBlockState(forced).getValue(SculkSensorBlock.WATERLOGGED), "a neighbour update should clear its waterlogged property");
		check(h, h.getBlockState(forced.north()).isAir() && h.getBlockState(forced.south()).isAir(), "no water should have flowed out of it");
		// A waterlogged vanilla sensor turned secret (as the coming Camouflage Wand will do) holds no water.
		BlockState converted = Counterparts.secretStateOf(Blocks.SCULK_SENSOR.defaultBlockState().setValue(SculkSensorBlock.WATERLOGGED, true)).orElseThrow();
		check(h, converted.is(SECRET) && converted.getFluidState().isEmpty(), "a waterlogged vanilla sensor turned secret should hold no water: " + converted);
		BlockState dry = Blocks.SCULK_SENSOR.defaultBlockState();
		check(h, ((LiquidBlockContainer) Blocks.SCULK_SENSOR).canPlaceLiquid(h.getLevel(), h.absolutePos(vanilla), dry, Fluids.WATER), "control: a bucket can fill a dry vanilla sensor");
		h.succeed();
	}

	/**
	 * Broken, it drops itself and no experience; a vanilla sensor broken without
	 * Silk Touch drops only 5 experience. With Silk Touch both drop themselves.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_misc")
	public void breakingDropsItselfAndNoExperience(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(1, 1, 2);
		BlockPos vanilla = new BlockPos(6, 1, 2);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.SCULK_SENSOR);
		h.getLevel().destroyBlock(h.absolutePos(secret), true);
		h.getLevel().destroyBlock(h.absolutePos(vanilla), true);
		// With Silk Touch both drop themselves and no experience.
		BlockPos secretSilk = new BlockPos(1, 1, 6);
		BlockPos vanillaSilk = new BlockPos(6, 1, 6);
		ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
		silk.enchant(Enchantments.SILK_TOUCH, 1);
		for (BlockPos pos : new BlockPos[] {secretSilk, vanillaSilk}) {
			h.setBlock(pos, pos == secretSilk ? SECRET : Blocks.SCULK_SENSOR);
			BlockState state = h.getBlockState(pos);
			Block.dropResources(state, h.getLevel(), h.absolutePos(pos), h.getBlockEntity(pos), null, silk);
			h.setBlock(pos, Blocks.AIR);
		}
		for (BlockPos pos : new BlockPos[] {secretSilk, vanillaSilk}) {
			AABB around = new AABB(h.absolutePos(pos)).inflate(1.5);
			Item item = pos == secretSilk ? SECRET.asItem() : Items.SCULK_SENSOR;
			List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(item));
			List<ExperienceOrb> orbs = h.getLevel().getEntitiesOfClass(ExperienceOrb.class, around);
			String what = pos == secretSilk ? "the Secret Sculk Sensor" : "control: the vanilla sensor";
			check(h, drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, what + " broken with Silk Touch should drop itself once, dropped " + drops);
			check(h, orbs.isEmpty(), what + " broken with Silk Touch should drop no experience, dropped " + orbs.size() + " orbs");
			drops.forEach(ItemEntity::discard);
			orbs.forEach(ExperienceOrb::discard);
		}
		Item[] items = {SECRET.asItem(), Items.SCULK_SENSOR};
		int[] expectedItems = {1, 0};
		int[] expectedExperience = {0, 5};
		BlockPos[] both = {secret, vanilla};
		for (int i = 0; i < 2; i++) {
			AABB around = new AABB(h.absolutePos(both[i])).inflate(2);
			Item item = items[i];
			List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(item));
			int dropped = drops.stream().mapToInt(e -> e.getItem().getCount()).sum();
			List<ExperienceOrb> orbs = h.getLevel().getEntitiesOfClass(ExperienceOrb.class, around);
			int experience = orbs.stream().mapToInt(SculkSensorGameTests::experienceIn).sum();
			String what = i == 0 ? "the Secret Sculk Sensor" : "control: the vanilla sensor";
			check(h, dropped == expectedItems[i], what + " should drop " + expectedItems[i] + " of itself, dropped " + dropped);
			check(h, experience == expectedExperience[i], what + " should drop " + expectedExperience[i] + " experience, dropped " + experience);
			drops.forEach(ItemEntity::discard);
			orbs.forEach(ExperienceOrb::discard);
		}
		h.succeed();
	}

	/**
	 * What it is made of: no light (vanilla's gives 1, which would give it
	 * away), wool-like toughness,
	 * not a conductor, a full block with a block entity (pistons cannot move
	 * it), a signal source that comparators read, and the server ticker for
	 * its own block entity type only.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_misc")
	public void properties(GameTestHelper h) {
		floor(h);
		BlockPos rel = new BlockPos(2, 1, 2);
		h.setBlock(rel, SECRET);
		BlockState secret = h.getBlockState(rel);
		BlockState vanilla = Blocks.SCULK_SENSOR.defaultBlockState();
		BlockPos abs = h.absolutePos(rel);
		check(h, vanilla.getLightEmission() == 1 && secret.getLightEmission() == 0, "it should give no light (a vanilla sensor gives 1), gives " + secret.getLightEmission());
		check(h, secret.getDestroySpeed(h.getLevel(), abs) == Blocks.WHITE_WOOL.defaultBlockState().getDestroySpeed(h.getLevel(), abs),
				"it should be as hard as wool (0.8), is " + secret.getDestroySpeed(h.getLevel(), abs));
		check(h, !secret.requiresCorrectToolForDrops(), "it should drop without a tool");
		check(h, !secret.isRedstoneConductor(h.getLevel(), abs), "it should not be a conductor");
		check(h, secret.isCollisionShapeFullBlock(h.getLevel(), abs), "it should be a full block");
		check(h, !secret.useShapeForLightOcclusion() && secret.canOcclude() == ModBlocks.SOLID_BLOCK.defaultBlockState().canOcclude(),
				"it should block light as the Dummy Block does, not by vanilla's half-block shape");
		check(h, secret.isSignalSource() && secret.hasAnalogOutputSignal(), "it should be a signal source that comparators read");
		check(h, secret.hasBlockEntity(), "it should have a block entity (so pistons cannot move it)");
		check(h, secret.getTicker(h.getLevel(), ModEntities.SECRET_SCULK_SENSOR) != null, "it should tick its own block entity type");
		check(h, secret.getTicker(h.getLevel(), BlockEntityType.SCULK_SENSOR) == null, "it should not tick the vanilla type");
		check(h, h.getBlockEntity(rel) instanceof SecretSculkSensorBlockEntity sensor && sensor.getType() == ModEntities.SECRET_SCULK_SENSOR
				&& sensor.getType().isValid(secret), "its block entity should be a Secret Sculk Sensor's, of its own type, got " + h.getBlockEntity(rel));
		h.succeed();
	}

	/**
	 * The disguise survives going active (a state change), and saving and
	 * loading with the last frequency; clients receive the disguise only.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_misc")
	public void keepsItsDisguiseAndSendsOnlyThat(GameTestHelper h) {
		floor(h);
		BlockPos rel = new BlockPos(2, 1, 5);
		h.setBlock(rel, SECRET);
		SecretSculkSensorBlockEntity sensor = (SecretSculkSensorBlockEntity) sensor(h, rel);
		sensor.setDisguise(all(Blocks.BRICKS));
		sensor.setLastVibrationFrequency(9);
		((SculkSensorBlock) h.getBlockState(rel).getBlock()).activate(null, h.getLevel(), h.absolutePos(rel), h.getBlockState(rel), 7, 9);
		check(h, phase(h, rel) == SculkSensorPhase.ACTIVE && power(h, rel) == 7, "it should be active at 7, is " + phase(h, rel) + "/" + power(h, rel));
		check(h, h.getBlockEntity(rel) == sensor, "going active should keep the block entity");
		checkFaces(h, rel, Blocks.BRICKS);
		CompoundTag saved = sensor.saveWithFullMetadata();
		check(h, saved.getString("id").equals("secretblocks:secret_sculk_sensor"), "it should save as secretblocks:secret_sculk_sensor, got " + saved.getString("id"));
		BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(rel), h.getBlockState(rel), saved);
		check(h, loaded instanceof SecretSculkSensorBlockEntity copy && copy.getLastVibrationFrequency() == 9
				&& copy.getState(Direction.UP).is(Blocks.BRICKS), "it should load back with frequency 9 and bricks, got " + loaded);
		CompoundTag update = sensor.getUpdateTag();
		CompoundTag packet = sensor.getUpdatePacket().getTag();
		for (CompoundTag tag : new CompoundTag[] {update, packet}) {
			check(h, tag != null && tag.contains("northState") && !tag.contains("listener") && !tag.contains("last_vibration_frequency"),
					"clients should receive the disguise only: " + tag);
		}
		h.succeed();
	}

	/** Dummy Block + sculk sensor makes one. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sculk_misc")
	public void recipeWorks(GameTestHelper h) {
		Optional<ItemStack> result = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.SCULK_SENSOR);
		check(h, result.isPresent() && result.get().is(SECRET.asItem()) && result.get().getCount() == 1,
				"Dummy Block + sculk sensor should craft one Secret Sculk Sensor, got " + result);
		h.succeed();
	}
}
