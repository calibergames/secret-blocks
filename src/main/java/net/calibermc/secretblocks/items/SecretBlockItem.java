package net.calibermc.secretblocks.items;

import net.calibermc.secretblocks.placement.DisguisePlacement;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Block item for every secret block. After vanilla placement succeeds, the
 * server picks the disguise from the clicked block and face and writes it to
 * the new block entity; clients get it through the vanilla block-entity sync.
 * Works with or without a player (automation).
 */
public class SecretBlockItem extends BlockItem {

	public SecretBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		// Decide before vanilla placement changes the world (the neighbor and
		// any replaced block must be read as they were).
		Map<Direction, BlockState> disguise = context.getLevel().isClientSide()
				? null : DisguisePlacement.choose(context, getBlock());
		InteractionResult result = super.place(context);
		if (disguise != null && result.consumesAction()) {
			DisguisePlacement.apply(context.getLevel(), context.getClickedPos(), disguise);
		}
		return result;
	}
}
