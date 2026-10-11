package net.calibermc.secretblocks.blocks;

import org.jetbrains.annotations.Nullable;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SecretRedstone extends net.minecraft.world.level.block.RedStoneWireBlock implements EntityBlock, SecretBlock {

    public SecretRedstone(Properties settings) {
        // A piston breaks it, as it breaks vanilla redstone dust.
        super(settings.pushReaction(PushReaction.DESTROY));
    }

//    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.0d, 0.0d, 0.0d, 1.0d, 1.0d, 1.0d);

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
    public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
        return (stateFrom.getBlock() instanceof SecretBlock) ? true : super.skipRendering(state, stateFrom, direction);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

//    @Override
//    public VoxelShape getOutlineShape(BlockState state, BlockView blockView, BlockPos pos, ShapeContext context) {
//        return SHAPE;
//    }

	/**
	 * Forge and NeoForge ask a wire's neighbours whether wire connects to them
	 * (vanilla's code asks "is this redstone wire?", widened on Fabric by a
	 * mixin to every wire block). Vanilla wire answers yes from any side, and
	 * so does this one. On those loaders this overrides their block hook;
	 * elsewhere nothing calls it.
	 */
	public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
		return true;
	}
}
