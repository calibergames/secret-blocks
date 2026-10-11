package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.mixin.PistonBaseBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Secret Piston and Secret Sticky Piston. A vanilla piston base drawn as its
 * disguise: vanilla's own code decides when it extends (power from any side,
 * quasi-connectivity), what it pushes (12 blocks) and pulls (sticky), and
 * the 2-tick move. Its head is a {@link SecretPistonHead}, a full block that
 * wears the base's disguise.
 *
 * Vanilla names its own piston and head in several places; the mixins make
 * those places accept these: {@code PistonBaseBlockMixin} (the head it
 * builds, which blocks it can push, the disguise carried through a move),
 * {@code PistonHeadBlockMixin} (which base a head belongs to),
 * {@code PistonMovingBlockEntityMixin} (the disguise during and after a move,
 * the moving head's collision) and, on clients,
 * {@code PistonHeadRendererMixin} (the move drawn in the disguise, never the
 * vanilla head).
 *
 * A full block extended or not; vanilla toughness and sound; not a
 * conductor, as vanilla's.
 */
public class SecretPiston extends PistonBaseBlock implements EntityBlock, SecretBlock {

	private final boolean sticky;

	public SecretPiston(boolean sticky, Properties settings) {
		super(sticky, settings);
		this.sticky = sticky;
	}

	public boolean isSticky() {
		return sticky;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretBlockEntity(pos, state);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return false;
	}

	@Override
	public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return stateFrom.getBlock() instanceof SecretBlock || super.skipRendering(state, stateFrom, direction);
	}

	/**
	 * The base's disguise changed (a probe, later the wand) while it is extended:
	 * the head wears it too, as it would after the next extension.
	 */
	public static void disguiseChanged(DisguisedBlockEntity base) {
		BlockEntity entity = (BlockEntity) base;
		Level level = entity.getLevel();
		BlockState state = entity.getBlockState();
		if (level == null || !(state.getBlock() instanceof SecretPiston) || !state.getValue(EXTENDED)) {
			return;
		}
		BlockPos headPos = entity.getBlockPos().relative(state.getValue(FACING));
		if (level.getBlockState(headPos).getBlock() instanceof SecretPistonHead && level.getBlockEntity(headPos) instanceof DisguisedBlockEntity head) {
			head.getDisguise().load(base.getDisguise().save(new CompoundTag()));
			head.markDisguiseChanged();
		}
	}

	/**
	 * Vanilla looks at its power when placed only if there is no block entity
	 * at the spot; this one always has one, so it looks every time it is placed
	 * (by a player, or at the end of a move).
	 */
	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (!oldState.is(state.getBlock()) && !level.isClientSide()) {
			((PistonBaseBlockAccessor) this).secretblocks$checkIfExtend(level, pos, state);
		}
	}
}
