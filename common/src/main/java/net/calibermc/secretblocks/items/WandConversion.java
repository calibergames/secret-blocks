package net.calibermc.secretblocks.items;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Camouflage Wand's plain use, on the server: turn a vanilla block into
 * its secret counterpart (the wand's disguise on every face) or a secret
 * block back into its vanilla one, keeping the block's state, contents,
 * custom name and lock.
 *
 * Nothing changes unless every check passes. A container's items are moved,
 * never dropped and never doubled: the old block's data is captured, its
 * container emptied (its removal would otherwise drop the items), the new
 * block placed, and the data loaded into it.
 */
public final class WandConversion {

	/** Containers on both sides of a pair: their items move to the new block. */
	private static final Set<Block> CONTAINERS = Set.of(
			Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.DISPENSER, Blocks.DROPPER, Blocks.HOPPER,
			ModBlocks.SECRET_CHEST, ModBlocks.SECRET_TRAPPED_CHEST, ModBlocks.SECRET_DISPENSER,
			ModBlocks.SECRET_DROPPER, ModBlocks.SECRET_HOPPER);

	/**
	 * How far a converted block's centre may be from the player's eyes: 6
	 * blocks, the distance at which vanilla's server stops accepting a use on
	 * a block from a client.
	 */
	private static final double REACH_SQR = 6.0 * 6.0;

	/** The outcome: {@code null} reason when the block was converted, else the refusal's message key suffix. */
	public record Result(String reason, Block converted) {
		static Result refused(String reason) {
			return new Result(reason, null);
		}
	}

	private WandConversion() {
	}

	/** Run every check, then convert the clicked block. Server side only. */
	public static Result convert(ServerPlayer player, Level level, InteractionHand hand, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		ItemStack wand = player.getItemInHand(hand);
		if (!CamouflageWand.mayUse(player)) {
			return Result.refused("not_allowed");
		}
		// A player the server lists as connected: not a machine acting as a player.
		if (player.getServer() == null || !player.getServer().getPlayerList().getPlayers().contains(player)) {
			return Result.refused("not_allowed");
		}
		if (!level.isLoaded(pos)) {
			return Result.refused("not_here");
		}
		if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQR) {
			return Result.refused("too_far");
		}
		if (!player.mayInteract(level, pos) || !player.mayUseItemAt(pos, hit.getDirection(), wand)) {
			return Result.refused("not_here");
		}
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof SecretBlock) {
			return toVanilla(level, pos, state);
		}
		return toSecret(level, pos, state, wand);
	}

	private static Result toSecret(Level level, BlockPos pos, BlockState vanilla, ItemStack wand) {
		Optional<BlockState> secret = Counterparts.secretStateOf(vanilla);
		if (secret.isEmpty()) {
			return Result.refused("no_counterpart");
		}
		Result common = refusalFor(level, pos, vanilla);
		if (common != null) {
			return common;
		}
		Optional<BlockState> disguise = CamouflageWand.loadedDisguise(wand);
		if (disguise.isEmpty()) {
			return Result.refused("empty");
		}
		Block secretBlock = secret.get().getBlock();
		// The usual counterpart needs no note; another (spruce door, stone brick stair) is remembered.
		Block original = Counterparts.vanillaOf(secretBlock).filter(usual -> usual != vanilla.getBlock()).isPresent()
				? vanilla.getBlock() : null;
		for (BlockPos part : parts(pos, vanilla)) {
			BlockState from = level.getBlockState(part);
			replace(level, part, from, Counterparts.copyShared(from, secretBlock.defaultBlockState()), false);
			if (level.getBlockEntity(part) instanceof DisguisedBlockEntity entity) {
				entity.setState(disguise.get());
				entity.setOriginal(original);
			}
		}
		neighbourUpdates(level, pos, vanilla);
		return new Result(null, secretBlock);
	}

	private static Result toVanilla(Level level, BlockPos pos, BlockState secret) {
		Optional<Block> usual = Counterparts.vanillaOf(secret.getBlock());
		if (usual.isEmpty()) {
			return Result.refused("no_counterpart");
		}
		Result common = refusalFor(level, pos, secret);
		if (common != null) {
			return common;
		}
		Block vanillaBlock = usual.get();
		if (level.getBlockEntity(pos) instanceof DisguisedBlockEntity entity && entity.getOriginal() != null
				&& Counterparts.secretOf(entity.getOriginal().defaultBlockState()).orElse(null) == secret.getBlock()) {
			vanillaBlock = entity.getOriginal();
		}
		for (BlockPos part : parts(pos, secret)) {
			BlockState from = level.getBlockState(part);
			replace(level, part, from, Counterparts.copyShared(from, vanillaBlock.defaultBlockState()), true);
		}
		neighbourUpdates(level, pos, secret);
		return new Result(null, vanillaBlock);
	}

	/** Refusals that apply both ways. */
	private static Result refusalFor(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof DoorBlock) {
			// The other half must be there: a lone half would otherwise turn its neighbour into a door.
			DoubleBlockHalf half = state.getValue(DoorBlock.HALF);
			BlockState other = level.getBlockState(half == DoubleBlockHalf.LOWER ? pos.above() : pos.below());
			if (!other.is(state.getBlock()) || other.getValue(DoorBlock.HALF) == half) {
				return Result.refused("door_half");
			}
		}
		if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			return Result.refused("double_chest");
		}
		if (state.hasProperty(PistonBaseBlock.EXTENDED) && state.getValue(PistonBaseBlock.EXTENDED)) {
			return Result.refused("piston_extended");
		}
		BlockEntity entity = level.getBlockEntity(pos);
		if (entity != null && entity.saveWithoutMetadata().contains("LootTable")) {
			return Result.refused("loot_table");
		}
		return null;
	}

	/** The block's positions: both halves of a door, else the block itself. */
	private static List<BlockPos> parts(BlockPos pos, BlockState state) {
		List<BlockPos> parts = new ArrayList<>();
		parts.add(pos);
		if (state.getBlock() instanceof DoorBlock) {
			parts.add(state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below());
		}
		return parts;
	}

	/**
	 * Replace one block, moving its block entity data to the new block. The
	 * neighbours are told afterwards ({@link #neighbourUpdates}): a door half
	 * placed next to its old other half would otherwise remove itself.
	 */
	private static void replace(Level level, BlockPos pos, BlockState from, BlockState to, boolean toVanilla) {
		// A pending tick (a pressed button's release, a pressure plate's recheck, a
		// sensor's cooldown) only runs for the block it was scheduled for.
		boolean pendingTick = level.getBlockTicks().hasScheduledTick(pos, from.getBlock());
		BlockEntity old = level.getBlockEntity(pos);
		CompoundTag data = old == null ? new CompoundTag() : old.saveWithoutMetadata();
		if (toVanilla) {
			Disguise.stripFrom(data);
		}
		// The new block takes the items, so the old one must not drop them when it is removed.
		if (old instanceof Container container && CONTAINERS.contains(to.getBlock())) {
			container.clearContent();
		}
		// Scheduled before the new block is placed: a powered observer placed with
		// no tick pending switches itself off at once.
		if (pendingTick) {
			level.scheduleTick(pos, to.getBlock(), 1);
		}
		level.setBlock(pos, to, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		BlockEntity fresh = level.getBlockEntity(pos);
		if (fresh != null && !data.isEmpty()) {
			CompoundTag merged = fresh.saveWithoutMetadata();
			merged.merge(data);
			fresh.load(merged);
			fresh.setChanged();
		}
	}

	/**
	 * Neighbour and shape updates for every converted position, once all of
	 * them are in place. Redstone wire also works out its own connections
	 * again (a six-sided Secret Redstone Wire has none of vanilla's).
	 */
	private static void neighbourUpdates(Level level, BlockPos pos, BlockState before) {
		for (BlockPos part : parts(pos, before)) {
			BlockState now = level.getBlockState(part);
			if (now.getBlock() instanceof RedStoneWireBlock) {
				BlockState connected = Block.updateFromNeighbourShapes(now, level, part);
				if (connected.is(now.getBlock()) && connected != now) {
					level.setBlock(part, connected, Block.UPDATE_CLIENTS);
					now = connected;
				}
			}
			level.updateNeighborsAt(part, now.getBlock());
			now.updateNeighbourShapes(level, part, Block.UPDATE_ALL);
		}
	}
}
