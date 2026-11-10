package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How disguise faces are drawn: in the pass their look's block is drawn in,
 * stained glass translucent, leaves with holes on Fancy graphics and solid on
 * Fast, as vanilla draws them.
 */
@Environment(EnvType.CLIENT)
public final class DisguiseMaterials {

	private DisguiseMaterials() {
	}

	/** How a disguise face showing {@code disguise} is drawn. */
	static FabricMaterials.Material forDisguise(BlockState disguise) {
		return FabricMaterials.layer(ItemBlockRenderTypes.getChunkRenderType(disguise));
	}
}
