package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How disguise faces are drawn: each in the pass its texture gives it
 * (stained glass translucent), and leaves solid where the graphics settings
 * draw leaves without holes, as vanilla draws them.
 */
@Environment(EnvType.CLIENT)
public final class DisguiseMaterials {

	private DisguiseMaterials() {
	}

	/** How a disguise face showing {@code disguise} is drawn. */
	static FabricMaterials.Material forDisguise(BlockState disguise) {
		boolean cutoutLeaves = Minecraft.getInstance().options.cutoutLeaves().get();
		return FabricMaterials.layer(ModelBlockRenderer.forceOpaque(cutoutLeaves, disguise) ? ChunkSectionLayer.SOLID : null);
	}
}
