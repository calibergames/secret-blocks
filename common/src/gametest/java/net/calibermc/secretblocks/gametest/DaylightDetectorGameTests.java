package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.SecretDaylightDetector;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Daylight Detector gives a vanilla daylight detector's signal
 * from the brightest sky light around it. Each check has a vanilla detector
 * beside it under the same sky. The time of day and the weather belong to the
 * whole test world, so the tests that need noon, midnight or rain run in
 * batches of their own, which set them first and put noon and clear weather
 * back afterwards.
 *
 * Detectors update once a second, and the light engine needs a tick or two
 * after blocks are placed, so every check waits 45 ticks or more.
 */
public class DaylightDetectorGameTests {

	private static final Block SECRET = ModBlocks.SECRET_DAYLIGHT_DETECTOR;
	private static final String NOON = "secretblocks_daylight_noon";
	private static final String MIDNIGHT = "secretblocks_daylight_midnight";
	private static final String RAIN = "secretblocks_daylight_rain";
	private static final String MORNING = "secretblocks_daylight_morning";
	private static final String DUSK = "secretblocks_daylight_dusk";
	private static final String BEFORE_DAWN = "secretblocks_daylight_before_dawn";

	private static void sky(ServerLevel level, long dayTime, boolean rain) {
		level.setDayTime(dayTime);
		level.setWeatherParameters(rain ? 0 : 100000, rain ? 100000 : 0, rain, false);
		// At once, not over the 100 ticks the weather takes to change: a batch must not start in the last one's fading rain.
		level.setRainLevel(rain ? 1.0F : 0.0F);
	}

	@BeforeBatch(batch = NOON)
	public static void noon(ServerLevel level) {
		sky(level, 6000, false);
	}

	@BeforeBatch(batch = MIDNIGHT)
	public static void midnight(ServerLevel level) {
		sky(level, 18000, false);
	}

	@BeforeBatch(batch = RAIN)
	public static void rain(ServerLevel level) {
		sky(level, 6000, true);
	}

	@BeforeBatch(batch = MORNING)
	public static void morning(ServerLevel level) {
		sky(level, 1000, false);
	}

	@BeforeBatch(batch = DUSK)
	public static void dusk(ServerLevel level) {
		sky(level, 12500, false);
	}

	@BeforeBatch(batch = BEFORE_DAWN)
	public static void beforeDawn(ServerLevel level) {
		sky(level, 23000, false);
	}

	@AfterBatch(batch = MORNING)
	public static void afterMorning(ServerLevel level) {
		sky(level, 6000, false);
	}

	@AfterBatch(batch = DUSK)
	public static void afterDusk(ServerLevel level) {
		sky(level, 6000, false);
	}

	@AfterBatch(batch = BEFORE_DAWN)
	public static void afterBeforeDawn(ServerLevel level) {
		sky(level, 6000, false);
	}

	@AfterBatch(batch = MIDNIGHT)
	public static void afterMidnight(ServerLevel level) {
		sky(level, 6000, false);
	}

	@AfterBatch(batch = RAIN)
	public static void afterRain(ServerLevel level) {
		sky(level, 6000, false);
	}

	private static int power(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(DaylightDetectorBlock.POWER);
	}

	private static BlockState inverted(Block block) {
		return block.defaultBlockState().setValue(DaylightDetectorBlock.INVERTED, true);
	}

	private static void both(GameTestHelper h, String when, int expected, int secret, int vanilla) {
		check(h, vanilla == expected, "control, " + when + ": the vanilla detector should give " + expected + ", gives " + vanilla);
		check(h, secret == expected, when + ": the Secret Daylight Detector should give " + expected + ", gives " + secret);
	}

	/** Bury the block at (x, y, z): stone on the five sides the floor does not already cover. */
	private static void bury(GameTestHelper h, int x, int y, int z) {
		for (Direction direction : Direction.values()) {
			if (direction != Direction.DOWN) {
				h.setBlock(new BlockPos(x, y, z).relative(direction), Blocks.STONE);
			}
		}
	}

	/** Noon under an open sky: 15, and a lamp beside it lights; inverted: 0. As a vanilla detector. */
	@GameTest(template = EMPTY_STRUCTURE, batch = NOON, timeoutTicks = 150)
	public void noonUnderAnOpenSky(GameTestHelper h) {
		floor(h);
		// Plain: Secret (1, 1, 1), vanilla (5, 1, 1). Inverted: Secret (1, 1, 5), vanilla (5, 1, 5). A lamp beside each plain one.
		h.setBlock(2, 1, 1, Blocks.REDSTONE_LAMP);
		h.setBlock(6, 1, 1, Blocks.REDSTONE_LAMP);
		h.setBlock(1, 1, 1, SECRET);
		h.setBlock(5, 1, 1, Blocks.DAYLIGHT_DETECTOR);
		h.setBlock(1, 1, 5, inverted(SECRET));
		h.setBlock(5, 1, 5, inverted(Blocks.DAYLIGHT_DETECTOR));
		h.runAfterDelay(45, () -> {
			both(h, "noon, open sky", 15, power(h, 1, 1, 1), power(h, 5, 1, 1));
			both(h, "noon, open sky, inverted", 0, power(h, 1, 1, 5), power(h, 5, 1, 5));
			check(h, h.getBlockState(new BlockPos(6, 1, 1)).getValue(RedstoneLampBlock.LIT), "control: the lamp beside the vanilla detector should be lit");
			check(h, h.getBlockState(new BlockPos(2, 1, 1)).getValue(RedstoneLampBlock.LIT), "the lamp beside the Secret Daylight Detector should be lit");
			h.succeed();
		});
	}

	/**
	 * Buried on every side it reads as in the dark: 0, and 15 inverted, like a
	 * buried vanilla detector. Buried after it was lit, it follows within two
	 * updates.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = NOON, timeoutTicks = 200)
	public void buriedReadsDark(GameTestHelper h) {
		floor(h);
		// Plain: Secret (1, 1, 1), vanilla (5, 1, 1). Inverted: Secret (1, 1, 5), vanilla (5, 1, 5).
		h.setBlock(1, 1, 1, SECRET);
		h.setBlock(5, 1, 1, Blocks.DAYLIGHT_DETECTOR);
		h.setBlock(1, 1, 5, inverted(SECRET));
		h.setBlock(5, 1, 5, inverted(Blocks.DAYLIGHT_DETECTOR));
		h.runAfterDelay(45, () -> {
			both(h, "before burying", 15, power(h, 1, 1, 1), power(h, 5, 1, 1));
			for (int x : new int[] {1, 5}) {
				bury(h, x, 1, 1);
				bury(h, x, 1, 5);
			}
		});
		h.runAfterDelay(100, () -> {
			both(h, "buried", 0, power(h, 1, 1, 1), power(h, 5, 1, 1));
			both(h, "buried, inverted", 15, power(h, 1, 1, 5), power(h, 5, 1, 5));
			h.succeed();
		});
	}

	/**
	 * With only one face open to the sky-lit air it still reads daylight,
	 * whichever of its six faces that is: 15 from the top or any side, and
	 * from below what reaches the space under it. A vanilla detector under a
	 * block reads one less than full, the light that reaches its own space.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = NOON, timeoutTicks = 150)
	public void anyOneOpenFaceIsEnough(GameTestHelper h) {
		floor(h);
		// One detector per face, each buried on its five other sides. The one open below sits high and to one
		// side, so that neither it nor its stone stands over another detector's open face.
		BlockPos[] spots = {new BlockPos(1, 2, 1), new BlockPos(5, 2, 1), new BlockPos(1, 2, 5), new BlockPos(5, 2, 5), new BlockPos(3, 2, 3), new BlockPos(3, 5, 6)};
		Direction[] open = {Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.DOWN};
		for (int i = 0; i < spots.length; i++) {
			h.setBlock(spots[i], SECRET);
			for (Direction direction : Direction.values()) {
				if (direction != open[i]) {
					h.setBlock(spots[i].relative(direction), Blocks.STONE);
				}
			}
		}
		h.setBlock(7, 1, 3, Blocks.DAYLIGHT_DETECTOR);
		h.setBlock(7, 2, 3, Blocks.STONE);
		h.runAfterDelay(45, () -> {
			for (int i = 0; i < spots.length; i++) {
				int got = h.getBlockState(spots[i]).getValue(DaylightDetectorBlock.POWER);
				if (open[i] == Direction.DOWN) {
					check(h, got >= 13, "open only below, it should read the light under it (13 or 14 at noon), gives " + got);
				} else {
					check(h, got == 15, "open only to the " + open[i] + ", the Secret Daylight Detector should give 15 at noon, gives " + got);
				}
			}
			check(h, power(h, 7, 1, 3) == 14, "for comparison: a vanilla detector under a block gives 14 at noon, gives " + power(h, 7, 1, 3));
			h.succeed();
		});
	}

	/**
	 * Use toggles inverted and the signal follows at once, as with a vanilla
	 * detector; the disguise is kept through it; a player who may not build
	 * changes nothing.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = NOON, timeoutTicks = 150)
	public void useTogglesInverted(GameTestHelper h) {
		floor(h);
		BlockPos secret = new BlockPos(1, 1, 1);
		BlockPos vanilla = new BlockPos(5, 1, 1);
		h.setBlock(secret, SECRET);
		h.setBlock(vanilla, Blocks.DAYLIGHT_DETECTOR);
		secret(h, secret).setState(Blocks.BRICKS.defaultBlockState());
		h.runAfterDelay(45, () -> {
			both(h, "noon", 15, power(h, 1, 1, 1), power(h, 5, 1, 1));
			Player player = h.makeMockPlayer();
			for (BlockPos pos : new BlockPos[] {secret, vanilla}) {
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
				InteractionResult result = h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
				check(h, result.consumesAction(), "use should toggle the detector at " + pos + ", got " + result);
				check(h, h.getBlockState(pos).getValue(DaylightDetectorBlock.INVERTED), "used once, the detector at " + pos + " should be inverted");
			}
			both(h, "the moment it is inverted at noon", 0, power(h, 1, 1, 1), power(h, 5, 1, 1));
			checkBlock(h, secret, SECRET);
			checkFaces(h, secret, Blocks.BRICKS);
			player.getAbilities().mayBuild = false;
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(secret)), Direction.UP, h.absolutePos(secret), false);
			InteractionResult refused = h.getBlockState(secret).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
			check(h, refused == InteractionResult.PASS && h.getBlockState(secret).getValue(DaylightDetectorBlock.INVERTED),
					"a player who may not build must not toggle it, got " + refused);
		});
		h.runAfterDelay(90, () -> {
			both(h, "inverted at noon, two updates on", 0, power(h, 1, 1, 1), power(h, 5, 1, 1));
			checkFaces(h, secret, Blocks.BRICKS);
			h.succeed();
		});
	}

	/**
	 * Weak power on every side equal to its level, no strong power, and not a
	 * conductor: a lamp behind a block next to it stays off, where a strongly
	 * powered block would light it.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = NOON, timeoutTicks = 150)
	public void weakPowerOnEverySide(GameTestHelper h) {
		floor(h);
		BlockPos pos = new BlockPos(2, 1, 2);
		h.setBlock(4, 1, 2, Blocks.REDSTONE_LAMP);
		h.setBlock(3, 1, 2, Blocks.STONE);
		h.setBlock(pos, SECRET);
		h.runAfterDelay(45, () -> {
			BlockState state = h.getBlockState(pos);
			check(h, state.getValue(DaylightDetectorBlock.POWER) == 15, "at noon it should be at 15");
			for (Direction direction : Direction.values()) {
				check(h, state.getSignal(h.getLevel(), h.absolutePos(pos), direction) == 15, "weak power asked from " + direction + " should be 15");
				check(h, state.getDirectSignal(h.getLevel(), h.absolutePos(pos), direction) == 0, "it must give no strong power (asked from " + direction + ")");
			}
			// Nudge the lamp: nothing else tells it to look.
			h.getLevel().updateNeighborsAt(h.absolutePos(new BlockPos(3, 1, 2)), Blocks.STONE);
			check(h, !h.getBlockState(new BlockPos(4, 1, 2)).getValue(RedstoneLampBlock.LIT), "the lamp behind the stone beside it must stay off");
			check(h, !state.isRedstoneConductor(h.getLevel(), h.absolutePos(pos)), "the Secret Daylight Detector must not be a redstone conductor");
			check(h, state.isCollisionShapeFullBlock(h.getLevel(), h.absolutePos(pos)), "it should be a full block");
			check(h, state.isSignalSource(), "it should be a signal source");
			boolean pushable = net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(state, h.getLevel(), h.absolutePos(pos), Direction.NORTH, true, Direction.NORTH);
			boolean vanillaPushable = net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(Blocks.DAYLIGHT_DETECTOR.defaultBlockState(), h.getLevel(), h.absolutePos(pos), Direction.NORTH, true, Direction.NORTH);
			boolean stonePushable = net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(Blocks.STONE.defaultBlockState(), h.getLevel(), h.absolutePos(pos), Direction.NORTH, true, Direction.NORTH);
			check(h, stonePushable, "control: a piston can push stone at this position");
			check(h, !pushable && !vanillaPushable, "a piston must not move it, as it cannot move a vanilla detector (ours " + pushable + ", vanilla " + vanillaPushable + ")");
			h.succeed();
		});
	}

	/** Midnight under an open sky: 0, and inverted the same value as a vanilla detector (11). */
	@GameTest(template = EMPTY_STRUCTURE, batch = MIDNIGHT, timeoutTicks = 150)
	public void midnightUnderAnOpenSky(GameTestHelper h) {
		floor(h);
		h.setBlock(1, 1, 1, SECRET);
		h.setBlock(5, 1, 1, Blocks.DAYLIGHT_DETECTOR);
		h.setBlock(1, 1, 5, inverted(SECRET));
		h.setBlock(5, 1, 5, inverted(Blocks.DAYLIGHT_DETECTOR));
		h.runAfterDelay(45, () -> {
			both(h, "midnight, open sky", 0, power(h, 1, 1, 1), power(h, 5, 1, 1));
			both(h, "midnight, open sky, inverted", 11, power(h, 1, 1, 5), power(h, 5, 1, 5));
			h.succeed();
		});
	}

	/**
	 * Between noon and night the sun's angle shapes the signal. At three such
	 * times the Secret detector gives what the vanilla one beside it gives, plain
	 * and inverted, and the plain value is strictly between 0 and 15, so the
	 * scaling is really at work.
	 */
	private static void matchesVanillaNow(GameTestHelper h, String when) {
		floor(h);
		h.setBlock(1, 1, 1, SECRET);
		h.setBlock(5, 1, 1, Blocks.DAYLIGHT_DETECTOR);
		h.setBlock(1, 1, 5, inverted(SECRET));
		h.setBlock(5, 1, 5, inverted(Blocks.DAYLIGHT_DETECTOR));
		h.runAfterDelay(45, () -> {
			int vanilla = power(h, 5, 1, 1);
			check(h, vanilla > 0 && vanilla < 15, "control: " + when + " a vanilla detector should be between 1 and 14, gives " + vanilla);
			check(h, power(h, 1, 1, 1) == vanilla, when + ": the Secret Daylight Detector gives " + power(h, 1, 1, 1) + ", the vanilla one " + vanilla);
			check(h, power(h, 1, 1, 5) == power(h, 5, 1, 5), when + ", inverted: the Secret Daylight Detector gives " + power(h, 1, 1, 5) + ", the vanilla one " + power(h, 5, 1, 5));
			h.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = MORNING, timeoutTicks = 150)
	public void morningMatchesVanilla(GameTestHelper h) {
		matchesVanillaNow(h, "in the morning");
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = DUSK, timeoutTicks = 150)
	public void duskMatchesVanilla(GameTestHelper h) {
		matchesVanillaNow(h, "at dusk");
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = BEFORE_DAWN, timeoutTicks = 150)
	public void beforeDawnMatchesVanilla(GameTestHelper h) {
		matchesVanillaNow(h, "before dawn");
	}

	/** Rain at noon dims the sky: both give the same value, less than 15 (12 in full rain). */
	@GameTest(template = EMPTY_STRUCTURE, batch = RAIN, timeoutTicks = 400)
	public void rainAtNoon(GameTestHelper h) {
		floor(h);
		h.setBlock(1, 1, 1, SECRET);
		h.setBlock(5, 1, 1, Blocks.DAYLIGHT_DETECTOR);
		h.runAfterDelay(60, () -> {
			check(h, h.getLevel().isRaining() && h.getLevel().getRainLevel(1.0F) > 0.9F, "control: it should be raining fully, rain level " + h.getLevel().getRainLevel(1.0F));
			both(h, "noon in the rain", 12, power(h, 1, 1, 1), power(h, 5, 1, 1));
			h.succeed();
		});
	}

	/**
	 * The formula, on vanilla's numbers: full sky at noon 15; at midnight (sky
	 * darkened by 11, sun angle pi) 0 and 11 inverted; no sky light 0 and 15
	 * inverted; rain at noon (darkened by 3) 12; one less light, one less
	 * signal.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void formulaOnKnownValues(GameTestHelper h) {
		float pi = (float) Math.PI;
		int[][] cases = {{15, 0, 0, 0, 15}, {15, 11, 1, 0, 0}, {15, 11, 1, 1, 11}, {0, 0, 0, 0, 0}, {0, 0, 0, 1, 15}, {15, 3, 0, 0, 12}, {14, 0, 0, 0, 14}, {0, 11, 1, 1, 15}};
		for (int[] c : cases) {
			int got = SecretDaylightDetector.signalStrength(c[0], c[1], c[2] == 1 ? pi : 0.0F, c[3] == 1);
			check(h, got == c[4], "sky light " + c[0] + ", darkened by " + c[1] + ", sun angle " + (c[2] == 1 ? "pi" : "0") + (c[3] == 1 ? ", inverted" : "") + " should give " + c[4] + ", gives " + got);
		}
		// Between the extremes, where the blend toward 0 or 2 pi matters: angle 1.0 becomes 0.8 (cos 0.697, 15 gives 10);
		// angle 5.0 becomes 5.257 (cos 0.518, 15 gives 8).
		int afternoon = SecretDaylightDetector.signalStrength(15, 0, 1.0F, false);
		check(h, afternoon == 10, "full sky at sun angle 1.0 should give 10, gives " + afternoon);
		int beforeNoon = SecretDaylightDetector.signalStrength(15, 0, 5.0F, false);
		check(h, beforeNoon == 8, "full sky at sun angle 5.0 should give 8, gives " + beforeNoon);
		h.succeed();
	}

	/**
	 * Where there is no sky it never updates, as vanilla: the block gives no
	 * ticker in the Nether, and gives one in the Overworld.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void noUpdatesWithoutASky(GameTestHelper h) {
		EntityBlock secret = (EntityBlock) SECRET;
		EntityBlock vanilla = (EntityBlock) Blocks.DAYLIGHT_DETECTOR;
		ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
		check(h, nether != null && !nether.dimensionType().hasSkyLight(), "control: the test server should have a Nether without sky light");
		check(h, vanilla.getTicker(nether, Blocks.DAYLIGHT_DETECTOR.defaultBlockState(), net.minecraft.world.level.block.entity.BlockEntityType.DAYLIGHT_DETECTOR) == null,
				"control: a vanilla detector has no ticker in the Nether");
		check(h, secret.getTicker(nether, SECRET.defaultBlockState(), ModEntities.SECRET_BLOCK_ENTITY) == null, "the Secret Daylight Detector must have no ticker in the Nether");
		check(h, secret.getTicker(h.getLevel(), SECRET.defaultBlockState(), ModEntities.SECRET_BLOCK_ENTITY) != null, "the Secret Daylight Detector should have a ticker in the Overworld");
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void recipe(GameTestHelper h) {
		Optional<ItemStack> made = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.DAYLIGHT_DETECTOR);
		check(h, made.isPresent() && made.get().is(SECRET.asItem()), "Dummy Block + daylight detector should make the Secret Daylight Detector, got " + made);
		h.succeed();
	}
}
