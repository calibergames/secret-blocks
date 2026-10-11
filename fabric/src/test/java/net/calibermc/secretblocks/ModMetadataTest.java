package net.calibermc.secretblocks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mod declares exactly the Minecraft version it is built and tested
 * against, so the loader refuses it on any other, and the version the
 * release uploads list is the one players can run it on.
 */
class ModMetadataTest {

	private static String find(Path file, String regex) throws IOException {
		Matcher m = Pattern.compile(regex, Pattern.MULTILINE).matcher(Files.readString(file));
		assertTrue(m.find(), "no match for " + regex + " in " + file);
		return m.group(1).trim();
	}

	@Test
	void declaresTheMinecraftVersionItIsBuiltFor() throws IOException {
		String built = find(Path.of("../gradle.properties"), "^minecraft_version\\s*=\\s*(\\S+)");
		String declared = find(Path.of("src/main/resources/fabric.mod.json"), "\"minecraft\"\\s*:\\s*\"([^\"]+)\"");
		assertEquals(built, declared, "fabric.mod.json should depend on exactly Minecraft " + built);
	}
}
