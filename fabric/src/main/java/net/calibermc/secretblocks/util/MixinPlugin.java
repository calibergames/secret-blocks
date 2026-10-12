package net.calibermc.secretblocks.util;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the mixins that target another mod only when that mod is
 * installed: {@code SodiumOcclusionMixin} needs Sodium. Without this,
 * Mixin looks for Sodium's class and logs a warning on every launch without
 * Sodium, and reports a Sodium version it could not hook ({@link MixinHooks}).
 * Lives outside the mixin package, which Mixin reserves for mixin classes.
 */
public class MixinPlugin implements IMixinConfigPlugin {

	private static final String SODIUM_MIXIN = "net.calibermc.secretblocks.fabric.mixin.SodiumOcclusionMixin";

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.equals(SODIUM_MIXIN)) {
			return FabricLoader.getInstance().isModLoaded("sodium");
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
		if (mixinClassName.equals(SODIUM_MIXIN)) {
			MixinHooks.checkOcclusionHook("Sodium", targetClass);
		}
	}
}
