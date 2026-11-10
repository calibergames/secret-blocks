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
 * Only the classes {@code SharedGameTests} lists have their game tests
 * registered: every shared class with game tests must be listed, so none is
 * silently skipped.
 */
class GameTestListTest {

	private static final Path SHARED = Path.of("src/gametest/java/net/calibermc/secretblocks/gametest");
	private static final Path REGISTRY = SHARED.resolve("support/SharedGameTests.java");
	private static final Pattern TEST = Pattern.compile("^\\s*@GameTest\\b", Pattern.MULTILINE);

	@Test
	void everySharedGameTestClassIsListed() throws IOException {
		Set<String> withTests = new TreeSet<>();
		try (Stream<Path> files = Files.list(SHARED)) {
			for (Path file : (Iterable<Path>) files::iterator) {
				if (file.toString().endsWith(".java") && TEST.matcher(Files.readString(file)).find()) {
					String name = file.getFileName().toString();
					withTests.add(name.substring(0, name.length() - ".java".length()));
				}
			}
		}
		String registry = Files.readString(REGISTRY);
		Set<String> listed = new TreeSet<>();
		Matcher list = Pattern.compile("CLASSES = List\\.of\\(([^;]*)\\);").matcher(registry);
		if (list.find()) {
			Matcher entry = Pattern.compile("(\\w+)\\.class").matcher(list.group(1));
			while (entry.find()) {
				listed.add(entry.group(1));
			}
		}
		assertEquals(withTests, listed, "SharedGameTests.CLASSES should name every shared game test class");
	}
}
