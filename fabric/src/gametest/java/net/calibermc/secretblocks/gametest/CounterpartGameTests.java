package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The counterpart table (secret block ↔ vanilla block) that the
 * Builder's Goggles and the Camouflage Wand use, and the rule that clients
 * receive a secret block's disguise and nothing else.
 */
public class CounterpartGameTests implements FabricGameTest {

	/** Secret blocks whose vanilla block leads back to another secret block, by design. */
	private static final Set<Block> SHARED = Set.of(ModBlocks.SECRET_REDSTONE_WIRE);

	private static List<Block> modBlocks() {
		List<Block> blocks = new ArrayList<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(SecretBlocks.MOD_ID)) {
				blocks.add(block);
			}
		}
		return blocks;
	}

	/**
	 * Every registered block that is a secret block is in {@code secretBlocksList}:
	 * that list gives it its render layer and its place in the every-block tests,
	 * so one left out would lose both without any other test noticing.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everySecretBlockIsInTheBlockList(GameTestHelper h) {
		List<Block> listed = List.of(ModBlocks.secretBlocksList);
		int secretBlocks = 0;
		for (Block block : modBlocks()) {
			if (block instanceof net.calibermc.secretblocks.blocks.SecretBlock) {
				secretBlocks++;
				check(h, listed.contains(block), BuiltInRegistries.BLOCK.getKey(block) + " is a secret block but is not in secretBlocksList");
			}
		}
		check(h, secretBlocks >= 20, "expected the mod's secret blocks, found " + secretBlocks);
		h.succeed();
	}

	/** Every registered secret block has a counterpart row or is listed as having none. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everySecretBlockIsListed(GameTestHelper h) {
		List<Block> blocks = modBlocks();
		check(h, blocks.size() >= 20, "expected the mod's blocks, found " + blocks.size());
		for (Block block : blocks) {
			check(h, Counterparts.isListed(block), BuiltInRegistries.BLOCK.getKey(block) + " has no row in Counterparts");
			Counterparts.vanillaOf(block).ifPresent(vanilla -> check(h, vanilla != Blocks.AIR
					&& !BuiltInRegistries.BLOCK.getKey(vanilla).getNamespace().equals(SecretBlocks.MOD_ID),
					BuiltInRegistries.BLOCK.getKey(block) + " must map to a vanilla block, maps to " + vanilla));
			check(h, !(Counterparts.hasNone(block) && Counterparts.vanillaOf(block).isPresent()),
					BuiltInRegistries.BLOCK.getKey(block) + " is listed both with and without a counterpart");
		}
		for (Block block : ModBlocks.secretBlocksList) {
			check(h, Counterparts.isListed(block), BuiltInRegistries.BLOCK.getKey(block) + " (in secretBlocksList) is not listed");
		}
		h.succeed();
	}

	/** States convert with their shared properties: facing, half, hinge, open, power, connections, shape. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void statesKeepTheirProperties(GameTestHelper h) {
		BlockState door = ModBlocks.IRON_DOOR_BLOCK.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST)
				.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER).setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT)
				.setValue(DoorBlock.OPEN, true);
		BlockState wire = ModBlocks.SECRET_REDSTONE.defaultBlockState().setValue(RedStoneWireBlock.POWER, 7)
				.setValue(RedStoneWireBlock.EAST, RedstoneSide.UP).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE);
		BlockState stairs = ModBlocks.STAIR_BLOCK.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH)
				.setValue(StairBlock.HALF, Half.TOP).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT);
		BlockState slab = ModBlocks.SLAB_BLOCK.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
		BlockState lever = ModBlocks.SECRET_LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
				.setValue(LeverBlock.FACING, Direction.WEST).setValue(LeverBlock.POWERED, true);
		BlockState observer = ModBlocks.SECRET_OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.UP);
		BlockState hopper = ModBlocks.SECRET_HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST)
				.setValue(HopperBlock.ENABLED, false);
		BlockState dispenser = ModBlocks.SECRET_DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP)
				.setValue(DispenserBlock.TRIGGERED, true);
		BlockState repeater = ModBlocks.SECRET_REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.EAST)
				.setValue(RepeaterBlock.DELAY, 3).setValue(RepeaterBlock.LOCKED, true).setValue(RepeaterBlock.POWERED, true);
		BlockState comparator = ModBlocks.SECRET_COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST)
				.setValue(ComparatorBlock.MODE, ComparatorMode.SUBTRACT).setValue(ComparatorBlock.POWERED, true);
		BlockState daylight = ModBlocks.SECRET_DAYLIGHT_DETECTOR.defaultBlockState().setValue(DaylightDetectorBlock.POWER, 7)
				.setValue(DaylightDetectorBlock.INVERTED, true);
		BlockState dropper = ModBlocks.SECRET_DROPPER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.DOWN)
				.setValue(DispenserBlock.TRIGGERED, true);
		BlockState sculk = ModBlocks.SECRET_SCULK_SENSOR.defaultBlockState().setValue(SculkSensorBlock.PHASE, SculkSensorPhase.COOLDOWN)
				.setValue(SculkSensorBlock.POWER, 7);
		for (BlockState secret : new BlockState[] {door, wire, stairs, slab, lever, observer, hopper, dispenser, repeater, comparator, daylight, dropper, sculk}) {
			BlockState vanilla = Counterparts.vanillaStateOf(secret).orElseThrow();
			for (var property : secret.getProperties()) {
				if (vanilla.hasProperty(property)) {
					check(h, vanilla.getValue(property).equals(secret.getValue(property)),
							secret + " → " + vanilla + ": " + property.getName() + " was not copied");
				}
			}
			// And back again: the round trip is the same state.
			BlockState back = Counterparts.secretStateOf(vanilla).orElseThrow();
			check(h, back.equals(secret), secret + " → " + vanilla + " → " + back + " is not the same state");
		}
		check(h, Counterparts.vanillaStateOf(door).orElseThrow().is(Blocks.IRON_DOOR), "the Secret Iron Door is an iron door");
		check(h, Counterparts.vanillaStateOf(wire).orElseThrow().is(Blocks.REDSTONE_WIRE), "Secret Redstone Dust is redstone wire");
		// The Wire converts one way only (redstone wire converts back to the Dust); its power is kept.
		BlockState sixSided = ModBlocks.SECRET_REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.POWER, 7);
		BlockState asVanilla = Counterparts.vanillaStateOf(sixSided).orElseThrow();
		check(h, asVanilla.is(Blocks.REDSTONE_WIRE) && asVanilla.getValue(RedStoneWireBlock.POWER) == 7,
				"the Wire at power 7 should show as redstone wire at power 7, got " + asVanilla);
		h.succeed();
	}

	/** Every secret block's vanilla counterpart leads back to it (so no two rows collide by accident). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyCounterpartLeadsBack(GameTestHelper h) {
		int pairs = 0;
		for (Block block : modBlocks()) {
			var vanilla = Counterparts.vanillaOf(block);
			if (vanilla.isPresent()) {
				var back = Counterparts.secretOf(vanilla.get().defaultBlockState());
				// Two secret blocks may share a vanilla block (Dust and Wire both stand in
				// for redstone wire); then it leads back to the one listed first.
				boolean shared = SHARED.contains(block);
				check(h, back.isPresent() && (back.get() == block || shared && back.get() == ModBlocks.SECRET_REDSTONE),
						BuiltInRegistries.BLOCK.getKey(block) + " → " + vanilla.get() + " → " + back + " does not lead back");
				pairs++;
			}
		}
		check(h, pairs >= 15, "expected the counterpart rows, found " + pairs);
		h.succeed();
	}

	/** The wand's direction: exact pairs, whole families (any wood) and blocks with no secret version. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void vanillaBlocksFindTheirSecretVersion(GameTestHelper h) {
		Object[][] cases = {
				{Blocks.CHEST, ModBlocks.SECRET_CHEST},
				{Blocks.REDSTONE_WIRE, ModBlocks.SECRET_REDSTONE},
				{Blocks.IRON_TRAPDOOR, ModBlocks.IRON_TRAPDOOR_BLOCK},
				{Blocks.SPRUCE_STAIRS, ModBlocks.STAIR_BLOCK},
				{Blocks.STONE_BRICK_STAIRS, ModBlocks.STAIR_BLOCK},
				{Blocks.BIRCH_DOOR, ModBlocks.DOOR_BLOCK},
				{Blocks.DARK_OAK_TRAPDOOR, ModBlocks.TRAPDOOR_BLOCK},
				{Blocks.ACACIA_BUTTON, ModBlocks.WOODEN_BUTTON},
				{Blocks.MANGROVE_PRESSURE_PLATE, ModBlocks.PRESSURE_PLATE},
				{Blocks.STONE_PRESSURE_PLATE, ModBlocks.PLAYER_PRESSURE_PLATE},
				{Blocks.SANDSTONE_SLAB, ModBlocks.SLAB_BLOCK},
		};
		for (Object[] c : cases) {
			Block vanilla = (Block) c[0];
			var secret = Counterparts.secretOf(vanilla.defaultBlockState());
			check(h, secret.isPresent() && secret.get() == c[1], vanilla + " should become " + c[1] + ", got " + secret);
		}
		for (Block none : new Block[] {Blocks.STONE, Blocks.GLASS, Blocks.OAK_LOG, Blocks.AIR}) {
			check(h, Counterparts.secretOf(none.defaultBlockState()).isEmpty(), none + " has no secret version");
		}
		h.succeed();
	}

	/**
	 * Clients receive a secret block's disguise and nothing else (contents,
	 * outputs and owners stay on the server). Covers every secret block with a
	 * block entity, so each new one is checked when it is registered.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everySecretBlockEntitySendsOnlyItsDisguise(GameTestHelper h) {
		Set<String> allowed = new Disguise().save(new CompoundTag()).getAllKeys();
		check(h, allowed.contains("northState"), "the disguise keys should include northState, are " + allowed);
		// A floor, so blocks that need support (Secret Redstone Dust) stay placed.
		floor(h);
		BlockPos rel = new BlockPos(1, 1, 1);
		int checked = 0;
		for (Block block : modBlocks()) {
			if (!(block instanceof EntityBlock)) {
				continue;
			}
			h.setBlock(rel, block.defaultBlockState());
			BlockEntity entity = h.getBlockEntity(rel);
			check(h, entity instanceof DisguisedBlockEntity, BuiltInRegistries.BLOCK.getKey(block) + " has no disguise block entity, has " + entity);
			((DisguisedBlockEntity) entity).setState(Direction.NORTH, Blocks.BRICKS.defaultBlockState());
			if (entity instanceof Container container && container.getContainerSize() > 0) {
				container.setItem(0, new ItemStack(Items.DIAMOND, 3));
			}
			// Both paths to the client: the chunk-load tag and the update packet.
			CompoundTag update = entity.getUpdateTag();
			var packet = entity.getUpdatePacket();
			CompoundTag packetTag = packet instanceof ClientboundBlockEntityDataPacket data ? data.getTag() : null;
			check(h, packetTag != null, BuiltInRegistries.BLOCK.getKey(block) + " sends no block-entity update packet");
			for (CompoundTag tag : new CompoundTag[] {update, packetTag}) {
				check(h, tag.contains("northState"), BuiltInRegistries.BLOCK.getKey(block) + " sends no disguise: " + tag);
				for (String key : tag.getAllKeys()) {
					check(h, allowed.contains(key), BuiltInRegistries.BLOCK.getKey(block) + " sends '" + key + "' to clients: " + tag);
				}
			}
			h.setBlock(rel, Blocks.AIR);
			checked++;
		}
		check(h, checked >= 20, "expected every secret block with a block entity, checked " + checked);
		h.succeed();
	}
}
