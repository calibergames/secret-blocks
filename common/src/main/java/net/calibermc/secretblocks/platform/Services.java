package net.calibermc.secretblocks.platform;

import java.util.ServiceLoader;

/**
 * The loader-specific implementations of the platform interfaces, found with
 * {@link ServiceLoader}: each loader's jar lists its implementation in
 * {@code META-INF/services}.
 */
public final class Services {

	public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

	private Services() {
	}

	private static <T> T load(Class<T> type) {
		return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
				.orElseThrow(() -> new IllegalStateException("No implementation of " + type.getName() + " for this loader"));
	}
}
