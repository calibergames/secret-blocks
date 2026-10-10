package net.calibermc.secretblocks.blocks;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class IronDoorBlock extends net.minecraft.world.level.block.DoorBlock implements EntityBlock, SecretBlock {

	public IronDoorBlock(Properties settings) {
		super(settings, BlockSetType.IRON);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretBlockEntity(pos, state);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof SecretBlockEntity) {
			SecretBlockEntity blockEntity = (SecretBlockEntity) world.getBlockEntity(pos);
		}
		return super.use(state, world, pos, player, hand, hit);
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
