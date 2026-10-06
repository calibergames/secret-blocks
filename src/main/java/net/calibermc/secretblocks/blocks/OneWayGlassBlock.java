package net.calibermc.secretblocks.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OneWayGlassBlock extends SolidBlock {

    public OneWayGlassBlock(Properties settings) {
        super(settings);
    }

    @Override  // OUTLINE FULL CUBE
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();

    }

//	@Override  // COLLIDE FULL CUBE
//	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
//		return VoxelShapes.fullCube();
//
//	}

}
