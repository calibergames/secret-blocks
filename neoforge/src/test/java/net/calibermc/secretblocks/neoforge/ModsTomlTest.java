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
 * The built mods.toml claims exactly the Minecraft version the jar is built
 * against, and its Forge floor is the oldest Forge the release is tested on
 * (47.1.0) and admits the NeoForge 1.20.1 version it is built against
 * (NeoForge 1.20.1 keeps Forge's mod id and version numbers).
 */
class ModsTomlTest {

	private static String find(String text, String regex) {
		Matcher m = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL).matcher(text);
		assertTrue(m.find(), "no match for " + regex);
		return m.group(1).trim();
	}

	private static String modsToml() throws IOException {
		try (InputStream in = ModsTomlTest.class.getResourceAsStream("/META-INF/mods.toml")) {
			assertNotNull(in, "the built mods.toml");
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
	void claimsTheBuiltMinecraftVersion() throws IOException {
		String properties = Files.readString(Path.of("../gradle.properties"));
		String built = find(properties, "^minecraft_version\\s*=\\s*(\\S+)");
		String range = find(modsToml(), "modId = \"minecraft\".*?versionRange = \"([^\"]*)\"");
		assertEquals("[" + built + "]", range, "mods.toml should claim exactly Minecraft " + built);
	}

	@Test
	void forgeFloorAdmitsTheBuiltVersion() throws IOException {
		String properties = Files.readString(Path.of("../gradle.properties"));
		String neoforge = find(properties, "^neoforge_version\\s*=\\s*\\S+-(\\S+)");
		String range = find(modsToml(), "modId = \"forge\".*?versionRange = \"\\[([^,\\]]+),\\)\"");
		assertEquals("47.1", range, "the Forge floor should stay the oldest tested Forge, 47.1.0");
		assertTrue(atLeast(version(neoforge), version(range)),
				"the Forge floor " + range + " should admit the NeoForge build version " + neoforge);
	}
}
