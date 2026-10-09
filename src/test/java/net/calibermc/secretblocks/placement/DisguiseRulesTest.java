package net.calibermc.secretblocks.placement;

import net.calibermc.secretblocks.placement.DisguiseRules.Face;
import net.calibermc.secretblocks.placement.DisguiseRules.FullCube;
import net.calibermc.secretblocks.placement.DisguiseRules.Other;
import net.calibermc.secretblocks.placement.DisguiseRules.SecretNeighbor;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The placement rules. States are plain strings here. */
class DisguiseRulesTest {

	private static final String STONE = "stone";
	private static final String GLASS = "glass";

	private static Map<Face, String> select(DisguiseRules.Neighbor<String> neighbor, Face touching, Face glassFace) {
		return DisguiseRules.select(neighbor, touching, glassFace, STONE, GLASS);
	}

	private static void assertAll(Map<Face, String> faces, String expected) {
		for (Face face : Face.values()) {
			assertEquals(expected, faces.get(face), face.name());
		}
	}

	private static Map<Face, String> sixFaces() {
		Map<Face, String> faces = new EnumMap<>(Face.class);
		faces.put(Face.UP, "gold");
		faces.put(Face.DOWN, "diamond");
		faces.put(Face.NORTH, "emerald");
		faces.put(Face.SOUTH, "lapis");
		faces.put(Face.WEST, "amethyst");
		faces.put(Face.EAST, "copper");
		return faces;
	}

	@Test
	void fullCubeIsCopiedToEveryFace() {
		assertAll(select(new FullCube<>("bricks"), Face.NORTH, null), "bricks");
	}

	@Test
	void anythingElseGivesStone() {
		assertAll(select(new Other<>(), Face.NORTH, null), STONE);
	}

	@Test
	void secretNeighborGivesItsTouchingFace() {
		// P6: the clicked (touching) face, for every face, not the down face.
		for (Face touching : Face.values()) {
			assertAll(select(new SecretNeighbor<>(sixFaces()), touching, null), sixFaces().get(touching));
		}
	}

	@Test
	void secretNeighborWithMissingFaceGivesStone() {
		assertAll(select(new SecretNeighbor<>(new EnumMap<>(Face.class)), Face.EAST, null), STONE);
	}

	@Test
	void oneWayGlassPutsGlassOnOneFace() {
		Map<Face, String> faces = select(new FullCube<>("quartz"), Face.NORTH, Face.NORTH);
		for (Face face : Face.values()) {
			assertEquals(face == Face.NORTH ? GLASS : "quartz", faces.get(face), face.name());
		}
	}

	@Test
	void glassFacesTheViewerWhenAPlayerPlaces() {
		// P4: a player facing south stands on the north side, so the north face is glass.
		assertEquals(Face.NORTH, DisguiseRules.glassFace(Face.SOUTH, Face.NORTH));
		assertEquals(Face.WEST, DisguiseRules.glassFace(Face.EAST, Face.WEST));
		// The player's facing wins over the clicked face.
		assertEquals(Face.EAST, DisguiseRules.glassFace(Face.WEST, Face.UP));
	}

	@Test
	void glassFacesTheViewerAboveOrBelow() {
		// Sneak-placing looking down (the player faces down): glass on top; looking up: underneath.
		assertEquals(Face.UP, DisguiseRules.glassFace(Face.DOWN, Face.UP));
		assertEquals(Face.DOWN, DisguiseRules.glassFace(Face.UP, Face.DOWN));
	}

	@Test
	void glassFacesAwayFromTheClickedBlockWithoutAPlayer() {
		// P3b: automation places without a player.
		for (Face touching : Face.values()) {
			assertEquals(touching, DisguiseRules.glassFace(null, touching));
		}
	}

	@Test
	void notGlassMeansNoGlassFace() {
		Map<Face, String> faces = select(new FullCube<>("bricks"), Face.UP, null);
		assertAll(faces, "bricks");
	}

	@Test
	void oppositeFaces() {
		for (Face face : Face.values()) {
			assertEquals(face, face.opposite().opposite());
		}
		assertEquals(Face.SOUTH, Face.NORTH.opposite());
		assertEquals(Face.UP, Face.DOWN.opposite());
	}
}
