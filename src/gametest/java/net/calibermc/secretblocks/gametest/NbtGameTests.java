package net.calibermc.secretblocks.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Block-entity NBT written by Secret Blocks 1.19.2 (1.2.0) loads unchanged
 * and saves back identically, so existing worlds keep their disguises. The
 * samples are copied from a 1.19.2 reference world (one item's custom name
 * is changed).
 */
public class NbtGameTests implements FabricGameTest {

	private static final BlockPos A = new BlockPos(3, 1, 3);

	/** B0: six different faces and every rotation. */
	static final String B0_SIX_FACES = "{upState:{Name:\"minecraft:gold_block\"},upDirection:\"up\",upRotation:0,downState:{Name:\"minecraft:diamond_block\"},downDirection:\"down\",downRotation:90,northState:{Name:\"minecraft:emerald_block\"},northDirection:\"north\",northRotation:180,southState:{Name:\"minecraft:lapis_block\"},southDirection:\"south\",southRotation:270,eastState:{Name:\"minecraft:copper_block\"},eastDirection:\"east\",eastRotation:0,westState:{Name:\"minecraft:amethyst_block\"},westDirection:\"west\",westRotation:90,age:0}";

	/** B1: property-bearing state, a face drawn from another side, a rotated top. */
	static final String B1_LOG_AXIS_X = "{upState:{Name:\"minecraft:oak_log\",Properties:{axis:\"x\"}},upDirection:\"up\",upRotation:90,downState:{Name:\"minecraft:oak_log\",Properties:{axis:\"x\"}},downDirection:\"down\",downRotation:0,northState:{Name:\"minecraft:oak_log\",Properties:{axis:\"x\"}},northDirection:\"east\",northRotation:0,southState:{Name:\"minecraft:oak_log\",Properties:{axis:\"x\"}},southDirection:\"south\",southRotation:0,eastState:{Name:\"minecraft:oak_log\",Properties:{axis:\"x\"}},eastDirection:\"east\",eastRotation:0,westState:{Name:\"minecraft:oak_log\",Properties:{axis:\"x\"}},westDirection:\"west\",westRotation:0,age:0}";

	/** B2: the furnace front drawn on four sides with four rotations. */
	static final String B2_FURNACE_ROTATIONS = "{upState:{Name:\"minecraft:furnace\",Properties:{facing:\"north\",lit:\"false\"}},upDirection:\"up\",upRotation:0,downState:{Name:\"minecraft:furnace\",Properties:{facing:\"north\",lit:\"false\"}},downDirection:\"down\",downRotation:0,northState:{Name:\"minecraft:furnace\",Properties:{facing:\"north\",lit:\"false\"}},northDirection:\"north\",northRotation:0,southState:{Name:\"minecraft:furnace\",Properties:{facing:\"north\",lit:\"false\"}},southDirection:\"north\",southRotation:180,eastState:{Name:\"minecraft:furnace\",Properties:{facing:\"north\",lit:\"false\"}},eastDirection:\"north\",eastRotation:90,westState:{Name:\"minecraft:furnace\",Properties:{facing:\"north\",lit:\"false\"}},westDirection:\"north\",westRotation:270,age:0}";

	/** B3: tinted and cutout disguises. */
	static final String B3_TINTED_CUTOUT = "{upState:{Name:\"minecraft:grass_block\",Properties:{snowy:\"false\"}},upDirection:\"up\",upRotation:0,downState:{Name:\"minecraft:dirt\"},downDirection:\"down\",downRotation:0,northState:{Name:\"minecraft:oak_leaves\",Properties:{distance:\"7\",persistent:\"true\",waterlogged:\"false\"}},northDirection:\"north\",northRotation:0,southState:{Name:\"minecraft:glass\"},southDirection:\"south\",southRotation:0,eastState:{Name:\"minecraft:dirt\"},eastDirection:\"east\",eastRotation:0,westState:{Name:\"minecraft:dirt\"},westDirection:\"west\",westRotation:0,age:0}";

	/** Row A One Way Glass placed by a player (glass toward the placer). */
	static final String ROW_A_ONE_WAY_GLASS = "{upState:{Name:\"minecraft:quartz_block\"},upDirection:\"up\",upRotation:0,downState:{Name:\"minecraft:quartz_block\"},downDirection:\"down\",downRotation:0,northState:{Name:\"minecraft:glass\"},northDirection:\"north\",northRotation:0,southState:{Name:\"minecraft:quartz_block\"},southDirection:\"south\",southRotation:0,eastState:{Name:\"minecraft:quartz_block\"},eastDirection:\"east\",eastRotation:0,westState:{Name:\"minecraft:quartz_block\"},westDirection:\"west\",westRotation:0,age:0}";

	/** Row A Secret Chest: bookshelf disguise plus items whose NBT format changes in later versions. */
	static final String ROW_A_CHEST = "{upState:{Name:\"minecraft:bookshelf\"},upDirection:\"up\",upRotation:0,downState:{Name:\"minecraft:bookshelf\"},downDirection:\"down\",downRotation:0,northState:{Name:\"minecraft:bookshelf\"},northDirection:\"north\",northRotation:0,southState:{Name:\"minecraft:bookshelf\"},southDirection:\"south\",southRotation:0,eastState:{Name:\"minecraft:bookshelf\"},eastDirection:\"east\",eastRotation:0,westState:{Name:\"minecraft:bookshelf\"},westDirection:\"west\",westRotation:0,age:0,"
			+ "Items:[{Slot:0b,id:\"minecraft:diamond\",Count:5b},{Slot:1b,id:\"minecraft:diamond_sword\",Count:1b,tag:{Damage:0,display:{Name:'{\"text\":\"Named ref\"}'},Enchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}},{Slot:2b,id:\"secretblocks:solid_block\",Count:64b},{Slot:26b,id:\"minecraft:oak_log\",Count:64b}]}";

	private static CompoundTag parse(GameTestHelper h, String snbt) {
		try {
			return TagParser.parseTag(snbt);
		} catch (CommandSyntaxException e) {
			h.fail("bad test SNBT: " + e.getMessage());
			throw new AssertionError("unreachable");
		}
	}

	/** Load {@code snbt} into a fresh block entity, check the API view, save, and require identical NBT. */
	private static SecretBlockEntity roundTrip(GameTestHelper h, Block block, String snbt) {
		CompoundTag input = parse(h, snbt);
		h.setBlock(A, block);
		SecretBlockEntity be = plain(h, A);
		be.load(input);
		for (Direction dir : Direction.values()) {
			String name = dir.getName();
			check(h, be.getState(dir) == NbtUtils.readBlockState(input.getCompound(name + "State")), name + "State read back wrong: " + be.getState(dir));
			check(h, be.getDir(dir).getName().equals(input.getString(name + "Direction")), name + "Direction read back wrong: " + be.getDir(dir));
			check(h, be.getRotation(dir) == input.getInt(name + "Rotation"), name + "Rotation read back wrong: " + be.getRotation(dir));
		}
		CompoundTag saved = be.saveWithoutMetadata();
		for (String key : input.getAllKeys()) {
			Tag before = input.get(key);
			Tag after = saved.get(key);
			check(h, before.equals(after), "key " + key + " changed on save: " + before + " -> " + after);
		}
		return be;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sixFacesAndRotations(GameTestHelper h) {
		SecretBlockEntity be = roundTrip(h, ModBlocks.SOLID_BLOCK, B0_SIX_FACES);
		check(h, be.getState(Direction.DOWN).is(Blocks.DIAMOND_BLOCK), "down face should be diamond");
		check(h, be.getRotation(Direction.SOUTH) == 270, "south rotation should be 270");
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void propertyStatesAndDrawnSides(GameTestHelper h) {
		SecretBlockEntity be = roundTrip(h, ModBlocks.SOLID_BLOCK, B1_LOG_AXIS_X);
		check(h, be.getState(Direction.UP).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "log axis should be x");
		check(h, be.getDir(Direction.NORTH) == Direction.EAST, "north face should be drawn from the east side");
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void furnaceRotations(GameTestHelper h) {
		roundTrip(h, ModBlocks.SOLID_BLOCK, B2_FURNACE_ROTATIONS);
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tintedAndCutoutStates(GameTestHelper h) {
		roundTrip(h, ModBlocks.SOLID_BLOCK, B3_TINTED_CUTOUT);
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oneWayGlassState(GameTestHelper h) {
		roundTrip(h, ModBlocks.ONE_WAY_GLASS, ROW_A_ONE_WAY_GLASS);
		h.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void chestWithItems(GameTestHelper h) {
		roundTrip(h, ModBlocks.SECRET_CHEST, ROW_A_CHEST);
		SecretChestBlockEntity be = chest(h, A);
		check(h, be.getItem(1).getHoverName().getString().equals("Named ref"), "named sword should keep its name");
		h.succeed();
	}
}
