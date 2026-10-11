package net.calibermc.secretblocks.gametest.driver;

import java.util.ArrayList;
import java.util.List;

/**
 * What the client asked to play and show while recording ({@code record on}):
 * sounds as they reach the sound manager (so they are seen even with no
 * audio device) and block particles as they are added, by sprite. Read and
 * cleared by the driver's {@code recorded} command.
 */
public final class Recorder {

	private static volatile boolean on;
	private static final List<String> SOUNDS = new ArrayList<>();
	private static final List<String> PARTICLES = new ArrayList<>();

	private Recorder() {
	}

	public static void setOn(boolean recording) {
		on = recording;
	}

	public static boolean isOn() {
		return on;
	}

	public static synchronized void sound(String line) {
		SOUNDS.add(line);
	}

	public static synchronized void particle(String sprite) {
		PARTICLES.add(sprite);
	}

	/** Everything recorded since the last call, then cleared. */
	public static synchronized List<String> drain() {
		List<String> lines = new ArrayList<>();
		SOUNDS.forEach(s -> lines.add("sound " + s));
		PARTICLES.stream().distinct().forEach(p -> lines.add("particle " + p + " x" + PARTICLES.stream().filter(p::equals).count()));
		SOUNDS.clear();
		PARTICLES.clear();
		return lines;
	}
}
