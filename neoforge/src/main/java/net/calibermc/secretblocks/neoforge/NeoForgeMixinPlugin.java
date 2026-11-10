package net.calibermc.secretblocks.neoforge;

import net.calibermc.secretblocks.util.MixinHooks;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the mixins that target another mod only when that mod is
 * installed: {@code SodiumOcclusionMixin} needs Sodium. Without this,
 * Mixin looks for Sodium's class and logs a warning on every launch
 * without it. Reports a Sodium version it could not hook
 * ({@link MixinHooks}). Lives outside the mixin package, which Mixin
 * reserves for mixin classes.
 */
public class NeoForgeMixinPlugin implements IMixinConfigPlugin {

	private static final String SODIUM_MIXIN = "net.calibermc.secretblocks.neoforge.mixin.SodiumOcclusionMixin";

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.equals(SODIUM_MIXIN)) {
			return LoadingModList.get().getModFileById("sodium") != null;
		}
		return true;
	}

	@Override
	public void onLoad(String mixinPackage) {
		var sodium = LoadingModList.get().getModFileById("sodium");
		if (sodium != null) {
			MixinHooks.checkHookClass("Sodium", sodium.getFile().getContents().containsFile(MixinHooks.SODIUM_HOOK_CLASS));
		}
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
