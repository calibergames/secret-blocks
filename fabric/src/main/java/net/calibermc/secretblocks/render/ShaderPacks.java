package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Whether an Iris shader pack is in use, read through Iris's public API by
 * reflection (no build dependency on Iris). False without Iris, or if its
 * API cannot be read.
 */
@Environment(EnvType.CLIENT)
final class ShaderPacks {

	/** Iris's API class: its current package, then the one before Iris 1.6. */
	private static final String[] API_CLASSES = {"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"};

	private static final Object API;
	private static final Method IN_USE;

	static {
		Object api = null;
		Method inUse = null;
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			for (String name : API_CLASSES) {
				try {
					Class<?> type = Class.forName(name);
					api = type.getMethod("getInstance").invoke(null);
					inUse = type.getMethod("isShaderPackInUse");
					break;
				} catch (ReflectiveOperationException | LinkageError e) {
					api = null;
					inUse = null;
				}
			}
		}
		API = api;
		IN_USE = inUse;
	}

	private ShaderPacks() {
	}

	static boolean inUse() {
		if (IN_USE == null) {
			return false;
		}
		try {
			return (boolean) IN_USE.invoke(API);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}
}
