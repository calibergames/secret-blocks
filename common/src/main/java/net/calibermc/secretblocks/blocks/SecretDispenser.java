package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretDispenserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Secret Dispenser: a vanilla dispenser that looks like its disguise. Its
 * block entity is a vanilla dispenser block entity with the disguise added,
 * so dispensing, the dispenser screen, comparators and dropping the contents
 * are all vanilla.
 */
public class SecretDispenser extends net.minecraft.world.level.block.DispenserBlock implements SecretBlock {

    public SecretDispenser(Properties settings) {
        super(settings);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SecretDispenserBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
        return (stateFrom.getBlock() instanceof SecretBlock) ? true : super.skipRendering(state, stateFrom, direction);
    }
}
