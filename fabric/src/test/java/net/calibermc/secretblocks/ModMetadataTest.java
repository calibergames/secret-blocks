package net.calibermc.secretblocks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The jar declares exactly the Minecraft versions listed in
 * {@code minecraft_versions} (gradle.properties), each tested on a production
 * server, so the loader refuses it on any other; the list includes the
 * version it is built against. fabric.mod.json lists them itself (Loom reads
 * the source file as JSON); this keeps the two the same.
 */
class ModMetadataTest {

	private static String find(String text, String regex) {
		Matcher m = Pattern.compile(regex, Pattern.MULTILINE).matcher(text);
		assertTrue(m.find(), "no match for " + regex);
		return m.group(1).trim();
	}

	private static List<String> split(String list) {
		return Arrays.stream(list.split(",")).map(s -> s.trim().replace("\"", "")).toList();
	}

	@Test
	void declaresTheClaimedMinecraftVersions() throws IOException {
		String properties = Files.readString(Path.of("../gradle.properties"));
		String built = find(properties, "^minecraft_version\\s*=\\s*(\\S+)");
		List<String> claimed = split(find(properties, "^minecraft_versions\\s*=\\s*(\\S+)"));
		assertTrue(claimed.contains(built), "minecraft_versions should include the built version " + built);

		String metadata;
		try (InputStream in = getClass().getResourceAsStream("/fabric.mod.json")) {
			assertNotNull(in, "the built fabric.mod.json");
			metadata = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		List<String> declared = split(find(metadata, "\"minecraft\"\\s*:\\s*\\[([^\\]]*)\\]"));
		assertEquals(claimed, declared, "fabric.mod.json should depend on exactly " + claimed);
	}
}
