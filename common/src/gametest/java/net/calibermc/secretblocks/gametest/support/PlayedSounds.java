package net.calibermc.secretblocks.gametest.support;

import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;

/** Sounds the server plays at a position, recorded while a test asks for them (server thread only). */
public final class PlayedSounds {

	public record Played(SoundEvent sound, float volume, float pitch) {
	}

	private static List<Played> recording;

	private PlayedSounds() {
	}

	public static void start() {
		recording = new ArrayList<>();
	}

	/** The sounds since {@link #start}; recording stops. */
	public static List<Played> stop() {
		List<Played> played = recording == null ? List.of() : recording;
		recording = null;
		return played;
	}

	public static void record(SoundEvent sound, float volume, float pitch) {
		if (recording != null) {
			recording.add(new Played(sound, volume, pitch));
		}
	}
}
