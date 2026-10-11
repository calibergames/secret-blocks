package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.EnumMap;
import java.util.Map;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Clients get disguises only through the vanilla block-entity sync (no
 * custom packets). These tests check the server side of that path:
 * the update packet and the initial chunk data carry the disguise, and a
 * placement write marks the chunk for saving. Client rendering itself is
 * checked by hand in game.
 */
public class SyncGameTests {

	private static final BlockPos A = new BlockPos(3, 1, 3);

	private static Map<Direction, BlockState> sixFaces() {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		faces.put(Direction.UP, Blocks.GOLD_BLOCK.defaultBlockState());
		faces.put(Direction.DOWN, Blocks.DIAMOND_BLOCK.defaultBlockState());
		faces.put(Direction.NORTH, Blocks.EMERALD_BLOCK.defaultBlockState());
		faces.put(Direction.SOUTH, Blocks.LAPIS_BLOCK.defaultBlockState());
		faces.put(Direction.WEST, Blocks.AMETHYST_BLOCK.defaultBlockState());
		faces.put(Direction.EAST, Blocks.COPPER_BLOCK.defaultBlockState());
		return faces;
	}

	private static void checkCarriesDisguise(GameTestHelper h, CompoundTag tag, Map<Direction, BlockState> faces, String what) {
		for (Map.Entry<Direction, BlockState> face : faces.entrySet()) {
			String key = face.getKey().getName() + "State";
			String expected = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(face.getValue().getBlock()).toString();
			check(h, tag != null && expected.equals(tag.getCompound(key).getString("Name")),
					what + " should carry " + key + "=" + expected + ", got " + (tag == null ? null : tag.get(key)));
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void updatePacketCarriesDisguise(GameTestHelper h) {
		h.setBlock(A, ModBlocks.SOLID_BLOCK);
		SecretBlockEntity be = plain(h, A);
		be.setDisguise(sixFaces());
		checkCarriesDisguise(h, be.getUpdatePacket().getTag(), sixFaces(), "block-entity update packet");
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void chunkDataCarriesDisguise(GameTestHelper h) {
		h.setBlock(A, ModBlocks.SOLID_BLOCK);
		SecretBlockEntity be = plain(h, A);
		be.setDisguise(sixFaces());
		checkCarriesDisguise(h, be.getUpdateTag(), sixFaces(), "initial chunk data");
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void placementWriteMarksChunkForSaving(GameTestHelper h) {
		h.setBlock(A, ModBlocks.SOLID_BLOCK);
		SecretBlockEntity be = plain(h, A);
		LevelChunk chunk = h.getLevel().getChunkAt(h.absolutePos(A));
		chunk.setUnsaved(false);
		be.setDisguise(sixFaces());
		check(h, chunk.isUnsaved(), "setDisguise should mark the chunk for saving");
		h.succeed();
	}

	/**
	 * A Secret Chest's contents are not sent to every client in range (a
	 * privacy fix): the update tag carries the disguise only. Players still see
	 * the contents through the chest menu when they open it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void chestContentsAreNotSentToClients(GameTestHelper h) {
		h.setBlock(A, ModBlocks.SECRET_CHEST);
		SecretChestBlockEntity chest = chest(h, A);
		chest.setDisguise(sixFaces());
		chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
		CompoundTag update = chest.getUpdateTag();
		checkCarriesDisguise(h, update, sixFaces(), "initial chunk data");
		check(h, !update.contains("Items"), "the update tag must not carry the chest contents: " + update.get("Items"));
		h.succeed();
	}
}
