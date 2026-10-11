package net.calibermc.secretblocks.items;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.placement.DisguisePlacement;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Block item for every secret block. After vanilla placement succeeds, the
 * server picks the disguise from the clicked block and face and writes it to
 * the new block entity; clients get it through the vanilla block-entity sync.
 * Works with or without a player (automation).
 *
 * The place sound is the look's: the block it copies (the look most of its
 * faces get), or, stacking a slab into a slab, the look it already has. The
 * placing client works it out too, as it plays its own place sound.
 */
public class SecretBlockItem extends BlockItem {

	/** The look of the block being placed, while vanilla placement runs (per thread: client and server both place). */
	private static final ThreadLocal<BlockState> PLACING_LOOK = new ThreadLocal<>();

	public SecretBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	/** The look of the secret block this thread is placing, or {@code null} when it is placing none. */
	@Nullable
	public static BlockState placingLook() {
		return PLACING_LOOK.get();
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		// Decide before vanilla placement changes the world (the neighbor and
		// any replaced block must be read as they were).
		Map<Direction, BlockState> disguise = DisguisePlacement.choose(context, getBlock());
		PLACING_LOOK.set(disguise != null ? mostFaces(disguise)
				: DisguiseLook.shown(context.getLevel(), context.getClickedPos(), Direction.UP));
		InteractionResult result;
		try {
			result = super.place(context);
		} finally {
			PLACING_LOOK.remove();
		}
		if (!context.getLevel().isClientSide() && disguise != null && result.consumesAction()) {
			DisguisePlacement.apply(context.getLevel(), context.getClickedPos(), disguise);
		}
		return result;
	}

	@Override
	protected SoundEvent getPlaceSound(BlockState state) {
		BlockState look = PLACING_LOOK.get();
		return look != null ? look.getSoundType().getPlaceSound() : super.getPlaceSound(state);
	}

	/** The look most faces get (all six, or five beside One Way Glass's glass). */
	private static BlockState mostFaces(Map<Direction, BlockState> disguise) {
		return disguise.values().stream()
				.max((a, b) -> Integer.compare(Collections.frequency(disguise.values(), a), Collections.frequency(disguise.values(), b)))
				.orElseThrow();
	}
}
