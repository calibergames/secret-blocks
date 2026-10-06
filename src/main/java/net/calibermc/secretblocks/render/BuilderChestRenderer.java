package net.calibermc.secretblocks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Builder's view for the Secret Chest: vanilla draws chests with a block
 * entity renderer, not a block model, so the chest inside the faded disguise
 * comes from vanilla's chest renderer. It draws a stand-in chest with no
 * level, which vanilla renders as a single closed chest facing south; it is
 * turned to face north, as the Secret Chest converts to its counterpart,
 * and lit from the sides the camera sees. The Secret Trapped Chest shows the
 * trapped chest's texture. Registered for
 * the shared secret block-entity type, so it returns at once for every
 * other block and while the view is off.
 */
@Environment(EnvType.CLIENT)
public class BuilderChestRenderer implements BlockEntityRenderer<SecretBlockEntity> {

	private final ChestRenderer<ChestBlockEntity> chestRenderer;
	private ChestBlockEntity standIn;
	private ChestBlockEntity trappedStandIn;

	public BuilderChestRenderer(BlockEntityRendererProvider.Context context) {
		this.chestRenderer = new ChestRenderer<>(context);
	}

	@Override
	public void render(SecretBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		if (!BuilderView.isActive() || !(entity instanceof SecretChestBlockEntity) || entity.getLevel() == null) {
			return;
		}
		if (standIn == null) {
			// Never given a level: only its (default) state is read. Vanilla picks the
			// trapped chest's texture by the block entity's class.
			standIn = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
			trappedStandIn = new TrappedChestBlockEntity(BlockPos.ZERO, Blocks.TRAPPED_CHEST.defaultBlockState());
		}
		ChestBlockEntity chest = entity.getBlockState().is(ModBlocks.SECRET_TRAPPED_CHEST) ? trappedStandIn : standIn;
		poseStack.pushPose();
		poseStack.translate(0.5, 0.0, 0.5);
		poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0f));
		poseStack.translate(-0.5, 0.0, -0.5);
		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		chestRenderer.render(chest, partialTick, poseStack, buffers, seenSideLight(entity.getLevel(), entity.getBlockPos(), camera), overlay);
		poseStack.popPose();
	}

	/**
	 * The light for the chest. The Secret Chest is a full, opaque block, so the
	 * light at its own position is 0 (the chest would draw black). It takes the
	 * brightest of the neighbouring spaces on the sides the camera can see, as
	 * those faces of a solid block are lit: a chest set in a wall is lit from
	 * the room it is seen from, not from daylight on the far side. With the
	 * camera inside the block, every side counts.
	 */
	private static int seenSideLight(Level level, BlockPos pos, Vec3 camera) {
		int seen = SeenFaces.of(pos.getX(), pos.getY(), pos.getZ(), camera.x, camera.y, camera.z);
		if (seen == 0) {
			seen = SeenFaces.ALL;
		}
		BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
		int block = 0;
		int sky = 0;
		for (Direction direction : Direction.values()) {
			if ((seen & (1 << direction.get3DDataValue())) != 0) {
				int packed = LevelRenderer.getLightColor(level, neighbour.setWithOffset(pos, direction));
				block = Math.max(block, LightTexture.block(packed));
				sky = Math.max(sky, LightTexture.sky(packed));
			}
		}
		return LightTexture.pack(block, sky);
	}
}
