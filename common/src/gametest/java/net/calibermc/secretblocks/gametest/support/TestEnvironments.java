package net.calibermc.secretblocks.gametest.support;

import java.util.Map;

/**
 * The environments the shared game tests run in. Each is a {@link SkyEnvironment}:
 * the time of day and the weather while its batch runs, set back to a clear noon
 * afterwards. Tests run in batches by environment, so the isolation environments
 * keep tests that would disturb each other (sculk vibrations, droppers firing into
 * one another) apart; they are a clear noon too.
 */
public final class TestEnvironments {

	public static final String DEFAULT = "default";
	public static final String DAYLIGHT_NOON = "daylight_noon";
	public static final String DAYLIGHT_MIDNIGHT = "daylight_midnight";
	public static final String DAYLIGHT_RAIN = "daylight_rain";
	public static final String DAYLIGHT_MORNING = "daylight_morning";
	public static final String DAYLIGHT_DUSK = "daylight_dusk";
	public static final String DAYLIGHT_BEFORE_DAWN = "daylight_before_dawn";

	/** A clear noon: where every batch starts and what each sets back. */
	static final SkyEnvironment NOON = new SkyEnvironment(6000, false);

	/** The environments that change the sky; any other name is a clear noon. */
	private static final Map<String, SkyEnvironment> SKIES = Map.of(
			DAYLIGHT_NOON, NOON,
			DAYLIGHT_MIDNIGHT, new SkyEnvironment(18000, false),
			DAYLIGHT_RAIN, new SkyEnvironment(6000, true),
			DAYLIGHT_MORNING, new SkyEnvironment(1000, false),
			DAYLIGHT_DUSK, new SkyEnvironment(12500, false),
			DAYLIGHT_BEFORE_DAWN, new SkyEnvironment(23000, false));

	private TestEnvironments() {
	}

	/** A new environment for {@code name}: each registered environment is a separate entry. */
	static SkyEnvironment sky(String name) {
		SkyEnvironment sky = SKIES.getOrDefault(name, NOON);
		return new SkyEnvironment(sky.time(), sky.rain());
	}
}
