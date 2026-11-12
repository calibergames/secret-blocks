package net.calibermc.secretblocks.gametest.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;

/**
 * A test environment that sets the time of day and the weather for its batch,
 * the rain at full strength at once (not over the 100 ticks the weather takes
 * to change), and holds the time there: the day clock is paused for the
 * batch, since a batch's tests may start long after it does (the first one
 * waits for its area to load, while the test server runs ticks as fast as it
 * can). It sets back a clear noon, the clock running again, when the batch
 * ends, so that no batch
 * starts in the last one's sky or fading rain. Each environment is its own
 * entry, so environments with the same sky (the isolation ones) stay apart.
 */
public final class SkyEnvironment implements TestEnvironmentDefinition<Unit> {

	private final int time;
	private final boolean rain;

	public SkyEnvironment(int time, boolean rain) {
		this.time = time;
		this.rain = rain;
	}

	public int time() {
		return time;
	}

	public boolean rain() {
		return rain;
	}

	public static final MapCodec<SkyEnvironment> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.fieldOf("time").forGetter(SkyEnvironment::time),
			Codec.BOOL.fieldOf("rain").forGetter(SkyEnvironment::rain)
	).apply(instance, SkyEnvironment::new));

	@Override
	public Unit setup(ServerLevel level) {
		apply(level, time, rain, true);
		return Unit.INSTANCE;
	}

	@Override
	public void teardown(ServerLevel level, Unit unused) {
		apply(level, TestEnvironments.NOON.time(), TestEnvironments.NOON.rain(), false);
	}

	@Override
	public MapCodec<SkyEnvironment> codec() {
		return CODEC;
	}

	private static void apply(ServerLevel level, long dayTime, boolean rain, boolean hold) {
		var clock = level.dimensionType().defaultClock().orElseThrow();
		level.clockManager().setTotalTicks(clock, dayTime);
		level.clockManager().setPaused(clock, hold);
		level.getServer().setWeatherParameters(rain ? 0 : 100000, rain ? 100000 : 0, rain, false);
		level.setRainLevel(rain ? 1.0F : 0.0F);
	}
}
