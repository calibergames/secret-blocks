package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretDispenserBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Secret Dropper: a vanilla dropper that looks like its disguise. Dropping
 * an item, pushing it into the container it faces, the 3×3 screen,
 * comparators and dropping the contents are all vanilla's code. It shares
 * the Secret Dispenser's block entity: vanilla's own dropper block entity
 * is a dispenser block entity with another name, and ours takes its name
 * from the block.
 */
public class SecretDropper extends DropperBlock implements SecretBlock {

	public SecretDropper(Properties settings) {
		super(settings);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretDispenserBlockEntity(pos, state);
	}

	/**
	 * Vanilla's own code, but for the statistic: vanilla counts "Droppers
	 * Searched" only for its own dropper block entity class, and would count a
	 * Secret Dropper as a dispenser.
	 */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof DispenserBlockEntity dropper) {
			player.openMenu(dropper);
			player.awardStat(Stats.INSPECT_DROPPER);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return (stateFrom.getBlock() instanceof SecretBlock) ? true : super.skipRendering(state, stateFrom, direction);
	}
}
