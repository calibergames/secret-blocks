package net.calibermc.secretblocks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fabric runs only the game test classes its test mod lists, while the other
 * loaders find them all: every shared class with game tests must be listed, so
 * none is silently skipped on Fabric.
 */
class GameTestListTest {

	private static final Path SHARED = Path.of("../common/src/gametest/java/net/calibermc/secretblocks/gametest");
	private static final Path FABRIC_MOD = Path.of("src/gametest/resources/fabric.mod.json");

	@Test
	void fabricListsEverySharedGameTestClass() throws IOException {
		Set<String> withTests = new TreeSet<>();
		try (Stream<Path> files = Files.list(SHARED)) {
			for (Path file : (Iterable<Path>) files::iterator) {
				if (file.toString().endsWith(".java") && Files.readString(file).contains("@GameTest(")) {
					String name = file.getFileName().toString();
					withTests.add("net.calibermc.secretblocks.gametest." + name.substring(0, name.length() - ".java".length()));
				}
			}
		}
		Set<String> listed = new TreeSet<>();
		String json = Files.readString(FABRIC_MOD);
		Matcher block = Pattern.compile("\"fabric-gametest\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
		if (block.find()) {
			Matcher entry = Pattern.compile("\"([^\"]+)\"").matcher(block.group(1));
			while (entry.find()) {
				listed.add(entry.group(1));
			}
		}
		assertEquals(withTests, listed, "fabric.mod.json's fabric-gametest list should name every shared game test class");
	}
}
