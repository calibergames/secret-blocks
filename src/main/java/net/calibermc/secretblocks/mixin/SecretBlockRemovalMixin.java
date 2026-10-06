package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When a secret block is removed, drop any items stuck inside it (before
 * 2.0.0, hoppers could push items into secret blocks that
 * are not containers). Runs at the one call every block's removal goes
 * through, before the block's own code, while the block entity still exists;
 * several vanilla block classes never call the base {@code onRemove}.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class SecretBlockRemovalMixin {

	@Shadow
	protected abstract BlockState asState();

	/**
	 * A secret block moved by a piston leaves no block entity
	 * behind: vanilla's wire and diode blocks skip the base removal code when
	 * moved, which is what removes it. Its data travels with the move.
	 */
	@Inject(method = "onRemove", at = @At("TAIL"))
	private void secretblocks$removeMovedBlockEntity(Level level, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
		BlockState state = asState();
		if (!level.isClientSide && moved && state.getBlock() instanceof SecretBlock && !newState.is(state.getBlock())
				&& level.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) instanceof DisguisedBlockEntity) {
			level.removeBlockEntity(pos);
		}
	}

	@Inject(method = "onRemove", at = @At("HEAD"))
	private void secretblocks$dropStoredItems(Level level, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
		BlockState state = asState();
		if (!level.isClientSide && !moved && state.getBlock() instanceof SecretBlock && !newState.is(state.getBlock())
				&& level.getBlockEntity(pos) instanceof SecretBlockEntity entity) {
			entity.dropStoredItems(level, pos);
		}
	}
}
