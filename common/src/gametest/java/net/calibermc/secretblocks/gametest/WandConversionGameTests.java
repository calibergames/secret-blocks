package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.LegacyData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.advancements.critereon.DataComponentMatchers;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.LockCode;
import net.minecraft.world.RandomizableContainer;
import net.calibermc.secretblocks.items.ItemData;
import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.calibermc.secretblocks.items.CamouflageWand;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.calibermc.secretblocks.gametest.support.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Camouflage Wand, converting: plain use turns a vanilla block into its
 * secret counterpart wearing the loaded disguise, and a secret block back,
 * keeping state, contents, custom name and lock, and the original block
 * (a spruce door comes back as spruce). Every server check refuses with
 * nothing changed.
 */
public class WandConversionGameTests {

	private static final BlockPos TARGET = new BlockPos(3, 1, 3);
	private static final Vec3 FEET_NORTH = new Vec3(3.5, 1, 0.5);
	private static final BlockState GRANITE = Blocks.POLISHED_GRANITE.defaultBlockState();

	/** One vanilla state per counterpart row, placed on the test floor. */
	private static final Map<BlockState, Block> PAIRS = new LinkedHashMap<>();

	static {
		PAIRS.put(Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
				.setValue(BlockStateProperties.HALF, Half.TOP), ModBlocks.STAIR_BLOCK);
		PAIRS.put(Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP), ModBlocks.SLAB_BLOCK);
		PAIRS.put(Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.OPEN, true)
				.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT), ModBlocks.DOOR_BLOCK);
		PAIRS.put(Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
				.setValue(BlockStateProperties.OPEN, true).setValue(BlockStateProperties.HALF, Half.TOP), ModBlocks.TRAPDOOR_BLOCK);
		PAIRS.put(Blocks.OAK_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), ModBlocks.WOODEN_BUTTON);
		PAIRS.put(Blocks.OAK_PRESSURE_PLATE.defaultBlockState(), ModBlocks.PRESSURE_PLATE);
		PAIRS.put(Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), ModBlocks.PLAYER_PRESSURE_PLATE);
		PAIRS.put(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState(), ModBlocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
		PAIRS.put(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState(), ModBlocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
		PAIRS.put(Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST), ModBlocks.IRON_DOOR_BLOCK);
		PAIRS.put(Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, Half.TOP), ModBlocks.IRON_TRAPDOOR_BLOCK);
		PAIRS.put(Blocks.STONE_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR), ModBlocks.STONE_BUTTON);
		PAIRS.put(Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).setValue(LeverBlock.POWERED, true), ModBlocks.SECRET_LEVER);
		PAIRS.put(Blocks.REDSTONE_WIRE.defaultBlockState(), ModBlocks.SECRET_REDSTONE);
		PAIRS.put(Blocks.CHEST.defaultBlockState(), ModBlocks.SECRET_CHEST);
		PAIRS.put(Blocks.TRAPPED_CHEST.defaultBlockState(), ModBlocks.SECRET_TRAPPED_CHEST);
		PAIRS.put(Blocks.OBSERVER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), ModBlocks.SECRET_OBSERVER);
		PAIRS.put(Blocks.DISPENSER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST), ModBlocks.SECRET_DISPENSER);
		PAIRS.put(Blocks.DROPPER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN), ModBlocks.SECRET_DROPPER);
		PAIRS.put(Blocks.SCULK_SENSOR.defaultBlockState(), ModBlocks.SECRET_SCULK_SENSOR);
		PAIRS.put(Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST), ModBlocks.SECRET_PISTON);
		PAIRS.put(Blocks.STICKY_PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), ModBlocks.SECRET_STICKY_PISTON);
		PAIRS.put(Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.EAST), ModBlocks.SECRET_HOPPER);
		PAIRS.put(Blocks.REPEATER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
				.setValue(BlockStateProperties.DELAY, 3), ModBlocks.SECRET_REPEATER);
		PAIRS.put(Blocks.COMPARATOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
				.setValue(BlockStateProperties.MODE_COMPARATOR, ComparatorMode.SUBTRACT), ModBlocks.SECRET_COMPARATOR);
		PAIRS.put(Blocks.DAYLIGHT_DETECTOR.defaultBlockState().setValue(BlockStateProperties.INVERTED, true), ModBlocks.SECRET_DAYLIGHT_DETECTOR);
	}

	private static WandTestPlayer op(GameTestHelper h) {
		return WandTestPlayer.at(h, FEET_NORTH, true, 0, false);
	}

	private static ItemStack wand(BlockState disguise) {
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		if (disguise != null) {
			ItemData.update(wand, tag -> tag.put(CamouflageWand.DISGUISE, NbtUtils.writeBlockState(disguise)));
		}
		return wand;
	}

	/**
	 * Place a vanilla or secret state at {@link #TARGET}, both halves for a
	 * door. Redstone wire takes the connections it would have where it stands.
	 */
	private static void place(GameTestHelper h, BlockState state) {
		if (state.getBlock() instanceof net.minecraft.world.level.block.RedStoneWireBlock) {
			state = Block.updateFromNeighbourShapes(state, h.getLevel(), h.absolutePos(TARGET));
		}
		h.setBlock(TARGET, state);
		if (state.getBlock() instanceof DoorBlock) {
			h.setBlock(TARGET.above(), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		}
	}

	private static InteractionResult convert(GameTestHelper h, WandTestPlayer player, ItemStack wand) {
		return player.click(h, wand, TARGET, Direction.NORTH, true);
	}

	private static String name(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	/** Every row both ways: the secret block with the state and the disguise on every face, then back to the same vanilla state. */
	@GameTest
	public void everyPairConvertsBothWays(GameTestHelper h) {
		floor(h);
		Set<Block> covered = PAIRS.values().stream().collect(Collectors.toSet());
		List<Block> missing = BuiltInRegistries.BLOCK.stream()
				.filter(b -> BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(SecretBlocks.MOD_ID))
				.filter(b -> Counterparts.vanillaOf(b).isPresent() && !covered.contains(b) && b != ModBlocks.SECRET_REDSTONE_WIRE)
				.toList();
		check(h, missing.isEmpty(), "rows without a conversion test: " + missing);
		StringBuilder problems = new StringBuilder();
		for (Map.Entry<BlockState, Block> pair : PAIRS.entrySet()) {
			place(h, pair.getKey());
			BlockState vanilla = h.getBlockState(TARGET);
			WandTestPlayer player = op(h);
			InteractionResult result = convert(h, player, wand(GRANITE));
			BlockState secret = h.getBlockState(TARGET);
			if (result != InteractionResult.SUCCESS || !secret.is(pair.getValue())) {
				problems.append(name(vanilla.getBlock())).append(" -> ").append(secret).append(" (").append(result).append(", ")
						.append(player.messages).append("); ");
				continue;
			}
			for (BlockPos part : vanilla.getBlock() instanceof DoorBlock ? List.of(TARGET, TARGET.above()) : List.of(TARGET)) {
				if (!(blockEntityAt(h, part) instanceof DisguisedBlockEntity entity)) {
					problems.append(name(vanilla.getBlock())).append(": no disguise at ").append(part).append("; ");
					continue;
				}
				for (Direction face : Direction.values()) {
					if (!entity.getState(face).equals(GRANITE)) {
						problems.append(name(vanilla.getBlock())).append(": ").append(face).append(" face ").append(entity.getState(face)).append("; ");
					}
				}
				if (entity.getOriginal() != null) {
					problems.append(name(vanilla.getBlock())).append(": the usual counterpart needs no original; ");
				}
			}
			convert(h, op(h), wand(GRANITE));
			if (!h.getBlockState(TARGET).equals(vanilla)) {
				problems.append(name(vanilla.getBlock())).append(" back as ").append(h.getBlockState(TARGET)).append("; ");
			}
			if (vanilla.getBlock() instanceof DoorBlock && !h.getBlockState(TARGET.above()).equals(vanilla.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER))) {
				problems.append(name(vanilla.getBlock())).append(" upper half back as ").append(h.getBlockState(TARGET.above())).append("; ");
			}
			h.setBlock(TARGET.above(), Blocks.AIR);
			h.setBlock(TARGET, Blocks.AIR);
		}
		// The Secret Redstone Wire has no vanilla twin of its own: it turns into redstone wire.
		h.setBlock(TARGET, ModBlocks.SECRET_REDSTONE_WIRE);
		convert(h, op(h), wand(GRANITE));
		if (!h.getBlockState(TARGET).is(Blocks.REDSTONE_WIRE)) {
			problems.append("secret redstone wire -> ").append(h.getBlockState(TARGET)).append("; ");
		}
		check(h, problems.length() == 0, problems.toString());
		h.succeed();
	}

	/** A block of another kind than the usual counterpart is remembered and comes back exactly. */
	@GameTest
	public void theOriginalBlockComesBack(GameTestHelper h) {
		floor(h);
		StringBuilder problems = new StringBuilder();
		for (BlockState vanilla : List.of(
				Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH),
				Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HALF, Half.TOP),
				Blocks.BIRCH_SLAB.defaultBlockState(),
				Blocks.ACACIA_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.OPEN, true),
				Blocks.DARK_OAK_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR),
				Blocks.JUNGLE_PRESSURE_PLATE.defaultBlockState(),
				Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR),
				Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultBlockState())) {
			place(h, vanilla);
			convert(h, op(h), wand(GRANITE));
			if (!(blockEntityAt(h, TARGET) instanceof DisguisedBlockEntity entity) || entity.getOriginal() != vanilla.getBlock()) {
				problems.append(name(vanilla.getBlock())).append(": original not kept; ");
			}
			convert(h, op(h), wand(GRANITE));
			if (!h.getBlockState(TARGET).equals(vanilla)) {
				problems.append(name(vanilla.getBlock())).append(" back as ").append(h.getBlockState(TARGET)).append("; ");
			}
			h.setBlock(TARGET.above(), Blocks.AIR);
			h.setBlock(TARGET, Blocks.AIR);
		}
		// A crafted Secret Door has no original: it goes back as oak.
		place(h, ModBlocks.DOOR_BLOCK.defaultBlockState());
		convert(h, op(h), wand(GRANITE));
		if (!h.getBlockState(TARGET).is(Blocks.OAK_DOOR)) {
			problems.append("crafted door back as ").append(h.getBlockState(TARGET)).append("; ");
		}
		h.setBlock(TARGET.above(), Blocks.AIR);
		// An original that does not lead back to this secret block is ignored.
		place(h, ModBlocks.DOOR_BLOCK.defaultBlockState());
		secret(h, TARGET).setOriginal(Blocks.STONE_BRICK_STAIRS);
		convert(h, op(h), wand(GRANITE));
		if (!h.getBlockState(TARGET).is(Blocks.OAK_DOOR)) {
			problems.append("door with a stair original back as ").append(h.getBlockState(TARGET)).append("; ");
		}
		check(h, problems.length() == 0, problems.toString());
		h.succeed();
	}

	/** A chest turned secret with a glass look lets light through, and keeps its contents. */
	@GameTest
	public void aGlassLookConversionLetsLightThrough(GameTestHelper h) {
		floor(h);
		place(h, Blocks.CHEST.defaultBlockState());
		((net.minecraft.world.Container) blockEntityAt(h, TARGET)).setItem(3, new ItemStack(Items.DIAMOND, 2));
		convert(h, op(h), wand(Blocks.GLASS.defaultBlockState()));
		BlockState state = h.getBlockState(TARGET);
		check(h, state.is(ModBlocks.SECRET_CHEST), "the chest should be secret: " + state);
		check(h, !state.getValue(net.calibermc.secretblocks.blocks.Opacity.OPAQUE)
				&& state.getLightBlock() == 0, "it should let light through: " + state);
		check(h, ((net.minecraft.world.Container) blockEntityAt(h, TARGET)).getItem(3).is(Items.DIAMOND), "it should keep its contents");
		h.succeed();
	}

	/** The original is saved with the block, and stays on the server. */
	@GameTest
	public void theOriginalIsSavedButNotSentToClients(GameTestHelper h) {
		floor(h);
		place(h, Blocks.SPRUCE_DOOR.defaultBlockState());
		convert(h, op(h), wand(GRANITE));
		BlockEntity entity = blockEntityAt(h, TARGET);
		CompoundTag saved = entity.saveWithFullMetadata(h.getLevel().registryAccess());
		check(h, "minecraft:spruce_door".equals(saved.getStringOr(Disguise.ORIGINAL, "")), "the original should be saved, got " + saved);
		var loaded = (DisguisedBlockEntity) BlockEntity.loadStatic(h.absolutePos(TARGET), h.getBlockState(TARGET), saved, h.getLevel().registryAccess());
		check(h, loaded.getOriginal() == Blocks.SPRUCE_DOOR, "the original should load back");
		check(h, !entity.getUpdateTag(h.getLevel().registryAccess()).contains(Disguise.ORIGINAL), "clients must not get the original");
		check(h, entity.getUpdateTag(h.getLevel().registryAccess()).contains("northState"), "clients still get the disguise");
		h.succeed();
	}

	private static void fill(Container container) {
		container.setItem(0, new ItemStack(Items.DIAMOND, 5));
		container.setItem(3, named(new ItemStack(Items.STICK), Component.literal("Marked")));
		container.setItem(container.getContainerSize() - 1, new ItemStack(Items.COBBLESTONE, 64));
	}

	private static String sameItems(Container a, Container b) {
		if (a.getContainerSize() != b.getContainerSize()) {
			return "sizes " + a.getContainerSize() + " and " + b.getContainerSize();
		}
		for (int i = 0; i < a.getContainerSize(); i++) {
			if (!ItemStack.matches(a.getItem(i), b.getItem(i))) {
				return "slot " + i + ": " + a.getItem(i) + " / " + b.getItem(i);
			}
		}
		return null;
	}

	/** Contents, custom name and lock go along both ways, and no item is dropped or doubled. */
	@GameTest
	public void containersKeepContentsNameAndLock(GameTestHelper h) {
		floor(h);
		StringBuilder problems = new StringBuilder();
		for (BlockState vanilla : List.of(Blocks.CHEST.defaultBlockState(), Blocks.TRAPPED_CHEST.defaultBlockState(),
				Blocks.DISPENSER.defaultBlockState(), Blocks.DROPPER.defaultBlockState(), Blocks.HOPPER.defaultBlockState())) {
			place(h, vanilla);
			BaseContainerBlockEntity original = (BaseContainerBlockEntity) blockEntityAt(h, TARGET);
			fill(original);
			nameContainer(original, Component.literal("Stash"));
			lockWithKey(h, original);
			Container reference = new net.minecraft.world.SimpleContainer(original.getContainerSize());
			for (int i = 0; i < original.getContainerSize(); i++) {
				reference.setItem(i, original.getItem(i).copy());
			}
			convert(h, op(h), wand(GRANITE));
			BlockEntity secret = blockEntityAt(h, TARGET);
			String label = name(vanilla.getBlock());
			if (!(secret instanceof Container items) || sameItems(reference, items) != null) {
				problems.append(label).append(" secret: ").append(secret instanceof Container c ? sameItems(reference, c) : secret).append("; ");
			}
			CompoundTag secretData = secret.saveWithoutMetadata(h.getLevel().registryAccess());
			if (!secretData.getStringOr("CustomName", "").contains("Stash") || !lockedWithKey(h, secretData)) {
				problems.append(label).append(" secret name/lock: ").append(secretData.getStringOr("CustomName", "")).append(" / ")
						.append(secretData.get(LockCode.TAG_LOCK)).append("; ");
			}
			convert(h, op(h), wand(GRANITE));
			BlockEntity back = blockEntityAt(h, TARGET);
			if (!(back instanceof BaseContainerBlockEntity container) || sameItems(reference, container) != null
					|| container.getCustomName() == null || !"Stash".equals(container.getCustomName().getString())
					|| !lockedWithKey(h, container.saveWithoutMetadata(h.getLevel().registryAccess()))) {
				problems.append(label).append(" back: ").append(back instanceof Container c ? sameItems(reference, c) : back).append("; ");
			}
			if (!h.getEntities(EntityType.ITEM, TARGET, 6).isEmpty()) {
				problems.append(label).append(": items were dropped; ");
			}
			((Container) back).clearContent();
			h.setBlock(TARGET, Blocks.AIR);
		}
		check(h, problems.length() == 0, problems.toString());
		h.succeed();
	}

	/** Locks {@code container} so that only an item named "key" opens it. */
	private static void lockWithKey(GameTestHelper h, BlockEntity container) {
		CompoundTag data = container.saveWithoutMetadata(h.getLevel().registryAccess());
		new LockCode(ItemPredicate.Builder.item().withComponents(DataComponentMatchers.Builder.components()
				.exact(DataComponentExactPredicate.expect(DataComponents.CUSTOM_NAME, Component.literal("key"))).build()).build())
				.addToTag(data, h.getLevel().registryAccess());
		container.loadWithComponents(data, h.getLevel().registryAccess());
	}

	/** Whether saved container data holds a lock that an item named "key" opens and another item does not. */
	private static boolean lockedWithKey(GameTestHelper h, CompoundTag data) {
		LockCode lock = LockCode.fromTag(data, h.getLevel().registryAccess());
		return lock.unlocksWith(named(new ItemStack(Items.TRIPWIRE_HOOK), Component.literal("key")))
				&& !lock.unlocksWith(named(new ItemStack(Items.TRIPWIRE_HOOK), Component.literal("other")));
	}

	/**
	 * A lock saved before 1.21.2 (a {@code Lock} name, not an item predicate) still locks
	 * the secret containers once loaded, as vanilla's own containers are upgraded.
	 */
	@GameTest
	public void anOlderLockStillLocks(GameTestHelper h) {
		floor(h);
		StringBuilder problems = new StringBuilder();
		for (Block block : List.of(ModBlocks.SECRET_CHEST, ModBlocks.SECRET_DISPENSER, ModBlocks.SECRET_HOPPER)) {
			h.setBlock(TARGET, block);
			BlockEntity container = blockEntityAt(h, TARGET);
			CompoundTag old = container.saveWithoutMetadata(h.getLevel().registryAccess());
			old.remove(LockCode.TAG_LOCK);
			old.putString("Lock", "key");
			old.putInt(LegacyData.VERSION, MINECRAFT_1_21_1);
			container.loadWithComponents(old, h.getLevel().registryAccess());
			CompoundTag saved = container.saveWithoutMetadata(h.getLevel().registryAccess());
			if (!lockedWithKey(h, saved) || saved.contains("Lock")) {
				problems.append(name(block)).append(": ").append(saved).append("; ");
			}
			h.setBlock(TARGET, Blocks.AIR);
		}
		check(h, problems.length() == 0, "an older lock should still lock: " + problems);
		h.succeed();
	}

	/** The game data version of Minecraft 1.21.1. */
	private static final int MINECRAFT_1_21_1 = 3955;

	/** A Secret Chest shows its custom name as the title, takes a renamed item's name, and keeps a lock. */
	@GameTest
	public void aSecretChestHasANameAndALock(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, ModBlocks.SECRET_CHEST);
		BlockState state = h.getBlockState(TARGET);
		WandTestPlayer player = op(h);
		ModBlocks.SECRET_CHEST.setPlacedBy(h.getLevel(), h.absolutePos(TARGET), state, player,
				named(new ItemStack(ModBlocks.SECRET_CHEST), Component.literal("Hidden")));
		SecretChestBlockEntity chest = (SecretChestBlockEntity) blockEntityAt(h, TARGET);
		check(h, chest.getCustomName() != null && "Hidden".equals(chest.getCustomName().getString()), "a renamed item should name the chest");
		var provider = state.getMenuProvider(h.getLevel(), h.absolutePos(TARGET));
		check(h, "Hidden".equals(provider.getDisplayName().getString()), "the title should be the name, got " + provider.getDisplayName());
		lockWithKey(h, chest);
		// An empty hand, through the server's real use path.
		player.click(h, ItemStack.EMPTY, TARGET, Direction.NORTH, false);
		check(h, "container.isLocked".equals(player.lastMessageKey()), "a locked chest should refuse, got " + player.messages);
		check(h, player.containerMenu == player.inventoryMenu, "the locked chest must not open");
		player.setItemInHand(InteractionHand.MAIN_HAND, named(new ItemStack(Items.TRIPWIRE_HOOK), Component.literal("key")));
		check(h, BaseContainerBlockEntity.canUnlock(player, chest.getLockKey(), Component.empty()), "the key should open it");
		check(h, chest.getUpdateTag(h.getLevel().registryAccess()).keySet().stream().noneMatch(k -> k.equals(LockCode.TAG_LOCK) || k.equals("CustomName")),
				"clients must not get the lock or the name");
		// Mined, it drops a chest item with the name, as vanilla's does.
		List<ItemStack> drops = Block.getDrops(state, h.getLevel(), h.absolutePos(TARGET), chest);
		check(h, drops.size() == 1 && drops.get(0).is(ModBlocks.SECRET_CHEST.asItem()) && "Hidden".equals(drops.get(0).getHoverName().getString()),
				"a named chest should drop a named item, got " + drops);
		h.succeed();
	}

	/** A pressed button or a powered plate keeps its release after converting (its pending tick moves with it). */
	@GameTest(maxTicks = 100)
	public void pendingTicksCarryOver(GameTestHelper h) {
		floor(h);
		BlockPos plate = TARGET.east(2);
		h.setBlock(TARGET, Blocks.OAK_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
				.setValue(BlockStateProperties.POWERED, true));
		h.getLevel().scheduleTick(h.absolutePos(TARGET), Blocks.OAK_BUTTON, 30);
		h.setBlock(plate, ModBlocks.PRESSURE_PLATE.defaultBlockState().setValue(BlockStateProperties.POWERED, true));
		h.getLevel().scheduleTick(h.absolutePos(plate), ModBlocks.PRESSURE_PLATE, 20);
		convert(h, op(h), wand(GRANITE));
		check(h, h.getBlockState(TARGET).is(ModBlocks.WOODEN_BUTTON) && h.getBlockState(TARGET).getValue(BlockStateProperties.POWERED),
				"the pressed button should convert pressed: " + h.getBlockState(TARGET));
		op(h).click(h, wand(GRANITE), plate, Direction.NORTH, true);
		check(h, h.getBlockState(plate).is(Blocks.OAK_PRESSURE_PLATE), "the plate should convert back: " + h.getBlockState(plate));
		// An observer in the middle of its pulse stays on until its own tick ends it.
		BlockPos observer = TARGET.west(2);
		// (The tick goes in first: a powered observer placed with none pending switches itself off.)
		h.getLevel().scheduleTick(h.absolutePos(observer), Blocks.OBSERVER, 2);
		h.setBlock(observer, Blocks.OBSERVER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP)
				.setValue(BlockStateProperties.POWERED, true));
		check(h, h.getBlockState(observer).getValue(BlockStateProperties.POWERED), "the test observer should start on");
		op(h).click(h, wand(GRANITE), observer, Direction.NORTH, true);
		check(h, h.getBlockState(observer).is(ModBlocks.SECRET_OBSERVER) && h.getBlockState(observer).getValue(BlockStateProperties.POWERED),
				"the observer should convert still on: " + h.getBlockState(observer));
		h.succeedWhen(() -> {
			check(h, !h.getBlockState(TARGET).getValue(BlockStateProperties.POWERED), "the secret button should be released");
			check(h, !h.getBlockState(plate).getValue(BlockStateProperties.POWERED), "the vanilla plate should be released");
			check(h, !h.getBlockState(observer).getValue(BlockStateProperties.POWERED), "the observer's pulse should end");
		});
	}

	/** A door half without its other half is refused; its neighbour is not touched. */
	@GameTest
	public void aLoneDoorHalfIsRefused(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET.above(), Blocks.STONE);
		h.setBlock(TARGET, Blocks.OAK_DOOR.defaultBlockState());
		WandTestPlayer player = op(h);
		check(h, player.click(h, wand(GRANITE), TARGET, Direction.NORTH, true) == InteractionResult.FAIL, "a lone half should be refused");
		check(h, player.lastMessageKey().endsWith(".door_half"), "expected the door-half reason, got " + player.messages);
		check(h, h.getBlockState(TARGET.above()).is(Blocks.STONE) && h.getBlockState(TARGET).is(Blocks.OAK_DOOR), "nothing should change");
		h.succeed();
	}

	/** Converted redstone works out its connections again: a six-sided Secret Redstone Wire becomes joined-up wire. */
	@GameTest
	public void convertedWireConnects(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET.east(), Blocks.REDSTONE_WIRE);
		h.setBlock(TARGET.west(), Blocks.REDSTONE_WIRE);
		h.setBlock(TARGET, ModBlocks.SECRET_REDSTONE_WIRE);
		convert(h, op(h), wand(GRANITE));
		BlockState wire = h.getBlockState(TARGET);
		check(h, wire.is(Blocks.REDSTONE_WIRE) && wire.getValue(BlockStateProperties.EAST_REDSTONE).isConnected()
				&& wire.getValue(BlockStateProperties.WEST_REDSTONE).isConnected(), "the wire should join its neighbours: " + wire);
		convert(h, op(h), wand(GRANITE));
		BlockState dust = h.getBlockState(TARGET);
		check(h, dust.is(ModBlocks.SECRET_REDSTONE) && dust.getValue(BlockStateProperties.EAST_REDSTONE).isConnected(),
				"the Secret Redstone Dust should join them too: " + dust);
		h.succeed();
	}

	/** Items stuck in a plain secret block (put there by a hopper before 2.0.0) drop when it turns vanilla; none are lost. */
	@GameTest
	public void stuckItemsDropWhenTurningVanilla(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, ModBlocks.SECRET_LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
		BlockEntity entity = blockEntityAt(h, TARGET);
		CompoundTag stuck = entity.saveWithoutMetadata(h.getLevel().registryAccess());
		CompoundTag diamonds = (CompoundTag) new ItemStack(Items.DIAMOND, 3).save(h.getLevel().registryAccess(), new CompoundTag());
		diamonds.putByte("Slot", (byte) 0);
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		list.add(diamonds);
		stuck.put("Items", list);
		entity.loadWithComponents(stuck, h.getLevel().registryAccess());
		convert(h, op(h), wand(GRANITE));
		check(h, h.getBlockState(TARGET).is(Blocks.LEVER), "the lever should turn vanilla: " + h.getBlockState(TARGET));
		int dropped = h.getEntities(EntityType.ITEM, TARGET, 3).stream().mapToInt(e -> e.getItem().is(Items.DIAMOND) ? e.getItem().getCount() : 0).sum();
		check(h, dropped == 3, "the three stuck diamonds should drop, found " + dropped);
		h.succeed();
	}

	/** The remembered original moves with the block when a piston pushes it. */
	@GameTest(maxTicks = 60)
	public void theOriginalMovesWithAPush(GameTestHelper h) {
		floor(h);
		place(h, Blocks.STONE_BRICK_STAIRS.defaultBlockState());
		convert(h, op(h), wand(GRANITE));
		check(h, secret(h, TARGET).getOriginal() == Blocks.STONE_BRICK_STAIRS, "the stair should remember stone bricks");
		h.setBlock(TARGET.west(), Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
		h.setBlock(TARGET.west(2), Blocks.REDSTONE_BLOCK);
		h.succeedWhen(() -> {
			check(h, h.getBlockState(TARGET.east()).is(ModBlocks.STAIR_BLOCK), "the stair should have moved east");
			check(h, secret(h, TARGET.east()).getOriginal() == Blocks.STONE_BRICK_STAIRS, "the original should move with it");
		});
	}

	/** Plain use reaches the wand before the block: a lever converts instead of flipping, a chest instead of opening. */
	@GameTest
	public void plainUseGoesBeforeTheBlock(GameTestHelper h) {
		floor(h);
		place(h, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
		WandTestPlayer player = op(h);
		check(h, convert(h, player, wand(GRANITE)) == InteractionResult.SUCCESS, "the wand should take the click");
		check(h, h.getBlockState(TARGET).is(ModBlocks.SECRET_LEVER) && !h.getBlockState(TARGET).getValue(LeverBlock.POWERED),
				"the lever should be converted, not flipped: " + h.getBlockState(TARGET));
		check(h, player.lastMessageKey().endsWith(".converted"), "the player should be told, got " + player.messages);
		place(h, Blocks.CHEST.defaultBlockState());
		check(h, convert(h, player, wand(GRANITE)) == InteractionResult.SUCCESS, "the wand should take the click on a chest");
		check(h, h.getBlockState(TARGET).is(ModBlocks.SECRET_CHEST), "the chest should be converted: " + h.getBlockState(TARGET));
		check(h, player.containerMenu == player.inventoryMenu, "the chest must not open");
		h.succeed();
	}

	/** Turning a secret block back needs no disguise; making one does. */
	@GameTest
	public void anEmptyWandOnlyTurnsBlocksBack(GameTestHelper h) {
		floor(h);
		place(h, Blocks.OAK_DOOR.defaultBlockState());
		WandTestPlayer player = op(h);
		check(h, convert(h, player, wand(null)) == InteractionResult.FAIL, "an empty wand cannot make a secret block");
		check(h, player.lastMessageKey().endsWith(".empty"), "the player should be told, got " + player.messages);
		check(h, h.getBlockState(TARGET).is(Blocks.OAK_DOOR), "nothing should change");
		h.setBlock(TARGET.above(), Blocks.AIR);
		place(h, ModBlocks.DOOR_BLOCK.defaultBlockState());
		check(h, convert(h, op(h), wand(null)) == InteractionResult.SUCCESS, "an empty wand turns a secret block back");
		check(h, h.getBlockState(TARGET).is(Blocks.OAK_DOOR), "the door should be oak again: " + h.getBlockState(TARGET));
		h.succeed();
	}

	/** Refused, with the reason, and nothing changes: the block, its neighbour half, its contents and the wand. */
	private static void refused(GameTestHelper h, WandTestPlayer player, BlockState placed, String reason, boolean listed) {
		place(h, placed);
		ItemStack wand = wand(GRANITE);
		CompoundTag wandBefore = ItemData.read(wand).copy();
		BlockState before = h.getBlockState(TARGET);
		InteractionResult result = player.click(h, wand, TARGET, Direction.NORTH, listed);
		check(h, result == InteractionResult.FAIL, reason + ": the click should fail, got " + result);
		check(h, h.getBlockState(TARGET).equals(before), reason + ": the block changed to " + h.getBlockState(TARGET));
		check(h, wandBefore.equals(ItemData.read(wand)), reason + ": the wand changed");
		check(h, player.lastMessageKey().endsWith("." + reason), reason + ": expected that reason, got " + player.messages);
		h.setBlock(TARGET.above(), Blocks.AIR);
		h.setBlock(TARGET.west(), Blocks.AIR);
		h.setBlock(TARGET, Blocks.AIR);
	}

	@GameTest
	public void blocksThatCannotBeConvertedAreRefused(GameTestHelper h) {
		floor(h);
		refused(h, op(h), Blocks.STONE.defaultBlockState(), "no_counterpart", true);
		refused(h, op(h), ModBlocks.SOLID_BLOCK.defaultBlockState(), "no_counterpart", true);
		refused(h, op(h), ModBlocks.TORCH_LEVER.defaultBlockState(), "no_counterpart", true);
		// A north-facing right half pairs with the left half to its west.
		h.setBlock(TARGET.west(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT));
		refused(h, op(h), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT), "double_chest", true);
		refused(h, op(h), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.EXTENDED, true), "piston_extended", true);
		refused(h, op(h), ModBlocks.SECRET_PISTON.defaultBlockState().setValue(PistonBaseBlock.EXTENDED, true), "piston_extended", true);
		for (Block container : new Block[] {Blocks.CHEST, Blocks.DISPENSER}) {
			h.setBlock(TARGET, container);
			RandomizableContainer.setBlockEntityLootTable(h.getLevel(), h.getLevel().getRandom(), h.absolutePos(TARGET), BuiltInLootTables.SIMPLE_DUNGEON);
			WandTestPlayer player = op(h);
			InteractionResult result = player.click(h, wand(GRANITE), TARGET, Direction.NORTH, true);
			check(h, result == InteractionResult.FAIL && h.getBlockState(TARGET).is(container), name(container) + " with a loot table should be refused");
			check(h, player.lastMessageKey().endsWith(".loot_table"), "expected the loot-table reason, got " + player.messages);
			h.setBlock(TARGET, Blocks.AIR);
		}
		h.succeed();
	}

	@GameTest
	public void playersWhoMayNotConvertAreRefused(GameTestHelper h) {
		floor(h);
		BlockState lever = Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
		// Survival and not an operator.
		refused(h, WandTestPlayer.at(h, FEET_NORTH, false, 0, false), lever, "not_allowed", true);
		// Not a player the server lists as connected (a machine acting as a player).
		refused(h, op(h), lever, "not_allowed", false);
		// Out of reach: about 7 blocks from the eyes to the block's centre (vanilla accepts 6).
		refused(h, WandTestPlayer.at(h, new Vec3(3.5, 1, -3.5), true, 0, false), lever, "too_far", true);
		// Spawn protection.
		WandTestPlayer protectedSpot = op(h);
		protectedSpot.protectedSpot = true;
		refused(h, protectedSpot, lever, "not_here", true);
		// Adventure mode: an operator who may not build here.
		WandTestPlayer adventure = WandTestPlayer.at(h, FEET_NORTH, false, 2, false);
		adventure.getAbilities().mayBuild = false;
		refused(h, adventure, lever, "not_here", true);
		h.succeed();
	}

	/** A block in an unloaded chunk is refused without loading it. */
	@GameTest
	public void anUnloadedChunkIsRefused(GameTestHelper h) {
		BlockPos far = h.absolutePos(TARGET).offset(1_000_000, 0, 1_000_000);
		check(h, !h.getLevel().isLoaded(far), "the far block should not be loaded");
		WandTestPlayer player = op(h);
		ItemStack wand = wand(GRANITE);
		player.setItemInHand(InteractionHand.MAIN_HAND, wand);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(far), Direction.NORTH, far, false);
		InteractionResult result = player.listed(() -> CamouflageWand.onUseBlock(player, h.getLevel(), InteractionHand.MAIN_HAND, hit));
		check(h, result == InteractionResult.FAIL, "an unloaded block should be refused, got " + result);
		check(h, player.lastMessageKey().endsWith(".not_here"), "expected the not-here reason, got " + player.messages);
		check(h, !h.getLevel().isLoaded(far), "the far chunk must not be loaded by the check");
		h.succeed();
	}
}
