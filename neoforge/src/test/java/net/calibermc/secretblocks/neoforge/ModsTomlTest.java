package net.calibermc.secretblocks.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The built neoforge.mods.toml claims the family's Minecraft versions, from
 * the oldest the NeoForge jar claims up to the next family, and its NeoForge
 * floor admits the NeoForge version it is built against.
 */
class ModsTomlTest {

	private static String find(String text, String regex) {
		Matcher m = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL).matcher(text);
		assertTrue(m.find(), "no match for " + regex);
		return m.group(1).trim();
	}

	private static String modsToml() throws IOException {
		try (InputStream in = ModsTomlTest.class.getResourceAsStream("/META-INF/neoforge.mods.toml")) {
			assertNotNull(in, "the built neoforge.mods.toml");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static int[] version(String v) {
		String[] parts = v.split("\\.");
		int[] out = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			out[i] = Integer.parseInt(parts[i]);
		}
		return out;
	}

	private static boolean atLeast(int[] v, int[] floor) {
		for (int i = 0; i < floor.length; i++) {
			int part = i < v.length ? v[i] : 0;
			if (part != floor[i]) {
				return part > floor[i];
			}
		}
		return true;
	}

	@Test
	void claimsTheFamilysMinecraftVersions() throws IOException {
		String properties = Files.readString(Path.of("../gradle.properties"));
		String oldest = find(properties, "^neoforge_minecraft_min\\s*=\\s*(\\S+)");
		String next = find(properties, "^next_minecraft_version\\s*=\\s*(\\S+)");
		String range = find(modsToml(), "modId = \"minecraft\".*?versionRange = \"([^\"]*)\"");
		assertEquals("[" + oldest + "," + next + ")", range, "neoforge.mods.toml should claim Minecraft " + oldest + " up to " + next);
	}

	@Test
	void neoForgeFloorAdmitsTheBuiltVersion() throws IOException {
		String properties = Files.readString(Path.of("../gradle.properties"));
		String built = find(properties, "^neoforge_version\\s*=\\s*(\\S+)");
		String floor = find(modsToml(), "modId = \"neoforge\".*?versionRange = \"\\[([^,\\]]+),\\)\"");
		assertTrue(atLeast(version(built), version(floor)), "the NeoForge floor " + floor + " should admit the NeoForge build " + built);
	}
}
