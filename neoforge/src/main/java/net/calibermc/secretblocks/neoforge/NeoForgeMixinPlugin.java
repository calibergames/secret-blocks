package net.calibermc.secretblocks.neoforge;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the mixins that target another mod only when that mod is
 * installed: {@code EmbeddiumOcclusionMixin} needs Embeddium. Without this,
 * Mixin looks for Embeddium's class and logs a warning on every launch
 * without it. Lives outside the mixin package, which Mixin reserves for
 * mixin classes.
 */
public class NeoForgeMixinPlugin implements IMixinConfigPlugin {

	private static final String EMBEDDIUM_MIXIN = "net.calibermc.secretblocks.neoforge.mixin.EmbeddiumOcclusionMixin";

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.equals(EMBEDDIUM_MIXIN)) {
			return LoadingModList.get().getModFileById("embeddium") != null;
		}
		return true;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
