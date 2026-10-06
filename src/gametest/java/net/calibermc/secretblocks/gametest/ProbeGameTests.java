package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Switch Probe and Rotator Probe. Probe edits flag the chunk for saving, so
 * they survive a save and reload.
 */
public class ProbeGameTests implements FabricGameTest {

	private static final BlockPos DUMMY = new BlockPos(3, 1, 3);
	private static final Vec3 DUMMY_NORTH = new Vec3(3.5, 1.5, 3.0);
	private static final Vec3 FEET_NORTH_OF_DUMMY = new Vec3(3.5, 1, 0.5);

	@GameTest(template = EMPTY_STRUCTURE)
	public void probeCopiesThenApplies(GameTestHelper h) {
		floor(h);
		BlockPos granite = new BlockPos(5, 1, 3);
		h.setBlock(granite, Blocks.POLISHED_GRANITE);
		h.setBlock(DUMMY, ModBlocks.SOLID_BLOCK);
		ItemStack probe = new ItemStack(ModItems.SWITCH_PROBE);

		// A fresh probe on a normal block stores the block and the clicked face.
		Vec3 graniteNorth = new Vec3(5.5, 1.5, 3.0);
		useOn(h, player(h, new Vec3(5.5, 1, 0.5), graniteNorth), probe, granite, Direction.NORTH, graniteNorth);
		CompoundTag tag = probe.getTag();
		check(h, tag != null && "minecraft:polished_granite".equals(tag.getCompound("setState").getString("Name")),
				"probe should store polished granite, got " + tag);
		check(h, "north".equals(tag.getString("setDirection")), "probe should store the north face, got " + tag);

		// The loaded probe sets the clicked face only, and stays loaded.
		useOn(h, player(h, FEET_NORTH_OF_DUMMY, DUMMY_NORTH), probe, DUMMY, Direction.NORTH, DUMMY_NORTH);
		checkFaces(h, DUMMY, Blocks.AIR, only(Direction.NORTH, Blocks.POLISHED_GRANITE));
		check(h, secret(h, DUMMY).getDir(Direction.NORTH) == Direction.NORTH, "north face should be drawn from the stored north side");
		check(h, probe.hasTag(), "probe should stay loaded");
		h.succeed();
	}

	/** A fresh probe used on a secret block copies that face. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void probeCopiesFromSecretBlock(GameTestHelper h) {
		floor(h);
		h.setBlock(DUMMY, ModBlocks.SOLID_BLOCK);
		secret(h, DUMMY).setState(Direction.NORTH, Blocks.EMERALD_BLOCK.defaultBlockState());
		ItemStack probe = new ItemStack(ModItems.SWITCH_PROBE);
		useOn(h, player(h, FEET_NORTH_OF_DUMMY, DUMMY_NORTH), probe, DUMMY, Direction.NORTH, DUMMY_NORTH);
		CompoundTag tag = probe.getTag();
		check(h, tag != null && "minecraft:emerald_block".equals(tag.getCompound("setState").getString("Name")),
				"probe should copy the emerald face, got " + tag);
		h.succeed();
	}

	/** The Rotator Probe turns the clicked face by 90 degrees per use. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void rotatorTurnsFace(GameTestHelper h) {
		floor(h);
		h.setBlock(DUMMY, ModBlocks.SOLID_BLOCK);
		ServerPlayer player = player(h, FEET_NORTH_OF_DUMMY, DUMMY_NORTH);
		ItemStack rotator = new ItemStack(ModItems.SWITCH_PROBE_ROTATION_MODE);
		useOn(h, player, rotator, DUMMY, Direction.NORTH, DUMMY_NORTH);
		check(h, secret(h, DUMMY).getRotation(Direction.NORTH) == 90, "first use should rotate to 90");
		useOn(h, player, rotator, DUMMY, Direction.NORTH, DUMMY_NORTH);
		check(h, secret(h, DUMMY).getRotation(Direction.NORTH) == 180, "second use should rotate to 180");
		check(h, secret(h, DUMMY).getRotation(Direction.SOUTH) == 0, "other faces should not rotate");
		h.succeed();
	}

	/** Sneak + use in the air swaps the probe mode and clears the stored block. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void sneakUseTogglesMode(GameTestHelper h) {
		ServerPlayer player = player(h, new Vec3(3.5, 1, 3.5), new Vec3(3.5, 8, 3.6));
		player.setShiftKeyDown(true);
		ItemStack probe = new ItemStack(ModItems.SWITCH_PROBE);
		probe.getOrCreateTag().putString("setDirection", "north");
		player.setItemInHand(InteractionHand.MAIN_HAND, probe);
		probe.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
		ItemStack swapped = player.getItemInHand(InteractionHand.MAIN_HAND);
		check(h, swapped.is(ModItems.SWITCH_PROBE_ROTATION_MODE), "should switch to the Rotator Probe, got " + swapped);
		check(h, !swapped.hasTag(), "the new probe should carry no stored block");
		swapped.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
		check(h, player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.SWITCH_PROBE), "should switch back to the Switch Probe");
		h.succeed();
	}

	private static LevelChunk chunk(GameTestHelper h, BlockPos pos) {
		return h.getLevel().getChunkAt(h.absolutePos(pos));
	}

	/** A probe apply and a rotator turn each flag the chunk for saving. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void probeEditsFlagTheChunkForSaving(GameTestHelper h) {
		floor(h);
		h.setBlock(DUMMY, ModBlocks.SOLID_BLOCK);
		ServerPlayer player = player(h, FEET_NORTH_OF_DUMMY, DUMMY_NORTH);
		ItemStack probe = new ItemStack(ModItems.SWITCH_PROBE);
		probe.getOrCreateTag().put("setState", NbtUtils.writeBlockState(Blocks.POLISHED_GRANITE.defaultBlockState()));
		probe.getOrCreateTag().putString("setDirection", "north");
		chunk(h, DUMMY).setUnsaved(false);
		useOn(h, player, probe, DUMMY, Direction.NORTH, DUMMY_NORTH);
		check(h, secret(h, DUMMY).getState(Direction.NORTH).is(Blocks.POLISHED_GRANITE), "the probe should have applied granite");
		check(h, chunk(h, DUMMY).isUnsaved(), "a probe apply must flag the chunk for saving");
		chunk(h, DUMMY).setUnsaved(false);
		useOn(h, player, new ItemStack(ModItems.SWITCH_PROBE_ROTATION_MODE), DUMMY, Direction.NORTH, DUMMY_NORTH);
		check(h, secret(h, DUMMY).getRotation(Direction.NORTH) == 90, "the rotator should have turned the face");
		check(h, chunk(h, DUMMY).isUnsaved(), "a rotator turn must flag the chunk for saving");
		h.succeed();
	}

	/** Every disguise setter flags the chunk for saving, on a plain secret block and on the dispenser. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyDisguiseSetterFlagsTheChunk(GameTestHelper h) {
		floor(h);
		StringBuilder problems = new StringBuilder();
		for (Block block : new Block[] {ModBlocks.SOLID_BLOCK, ModBlocks.SECRET_DISPENSER}) {
			h.setBlock(DUMMY, block);
			var secret = secret(h, DUMMY);
			Map<String, Runnable> setters = new LinkedHashMap<>();
			setters.put("setState(face)", () -> secret.setState(Direction.UP, Blocks.GOLD_BLOCK.defaultBlockState()));
			setters.put("setState(all)", () -> secret.setState(Blocks.DIAMOND_BLOCK.defaultBlockState()));
			setters.put("setDirection(face)", () -> secret.setDirection(Direction.UP, Direction.EAST));
			setters.put("setDirection(all)", () -> secret.setDirection(Direction.WEST));
			setters.put("setRotation(face)", () -> secret.setRotation(Direction.UP, 90));
			setters.put("setRotation(all)", () -> secret.setRotation(180));
			for (var setter : setters.entrySet()) {
				chunk(h, DUMMY).setUnsaved(false);
				setter.getValue().run();
				if (!chunk(h, DUMMY).isUnsaved()) {
					problems.append(block).append(' ').append(setter.getKey()).append("; ");
				}
			}
		}
		check(h, problems.length() == 0, "setters that do not flag the chunk for saving: " + problems);
		h.succeed();
	}

	/** Probe edits are in the saved data: a save and load of the block entity keeps them. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void probeEditsSurviveSaveAndLoad(GameTestHelper h) {
		floor(h);
		h.setBlock(DUMMY, ModBlocks.SOLID_BLOCK);
		var secret = secret(h, DUMMY);
		secret.setState(Direction.NORTH, Blocks.POLISHED_GRANITE.defaultBlockState());
		secret.setDirection(Direction.NORTH, Direction.UP);
		secret.setRotation(Direction.NORTH, 270);
		var saved = plain(h, DUMMY).saveWithFullMetadata();
		var loaded = (DisguisedBlockEntity)
				BlockEntity.loadStatic(h.absolutePos(DUMMY), h.getBlockState(DUMMY), saved);
		check(h, loaded.getState(Direction.NORTH).is(Blocks.POLISHED_GRANITE), "the face disguise should survive");
		check(h, loaded.getDir(Direction.NORTH) == Direction.UP, "the drawn side should survive");
		check(h, loaded.getRotation(Direction.NORTH) == 270, "the rotation should survive");
		h.succeed();
	}
}
