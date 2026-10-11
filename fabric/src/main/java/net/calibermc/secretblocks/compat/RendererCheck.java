package net.calibermc.secretblocks.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.util.Optional;

/**
 * Secret blocks draw their disguises through the Fabric Rendering API.
 * Sodium before 0.6 replaces the block renderer without implementing that
 * API, so with it and without Indium every secret block is invisible. The
 * client refuses to start in that setup, before the game window opens, and
 * says what to install, rather than run with blocks nobody can see.
 */
public final class RendererCheck implements PreLaunchEntrypoint {

	/** The first Sodium with the Fabric Rendering API built in. */
	private static final String BUILT_IN_SINCE = "0.6.0";

	@Override
	public void onPreLaunch() {
		FabricLoader loader = FabricLoader.getInstance();
		if (loader.getEnvironmentType() != EnvType.CLIENT) {
			return;
		}
		Optional<ModContainer> sodium = loader.getModContainer("sodium");
		String problem = problem(sodium.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse(null),
				loader.isModLoaded("indium"));
		if (problem != null) {
			throw new IllegalStateException(problem);
		}
	}

	/**
	 * Why the client cannot start, or null if it can.
	 *
	 * @param sodiumVersion the installed Sodium's version, or null without Sodium
	 * @param indiumLoaded  whether Indium is installed
	 */
	public static String problem(String sodiumVersion, boolean indiumLoaded) {
		if (sodiumVersion == null || indiumLoaded || hasBuiltInSupport(sodiumVersion)) {
			return null;
		}
		return "Secret Blocks needs Indium (https://modrinth.com/mod/indium) when Sodium " + sodiumVersion
				+ " is installed: without it Sodium cannot draw secret blocks, and they would be invisible."
				+ " Install Indium for this Minecraft version, or remove Sodium.";
	}

	private static boolean hasBuiltInSupport(String sodiumVersion) {
		try {
			Version version = Version.parse(sodiumVersion);
			return version instanceof SemanticVersion semantic
					&& semantic.compareTo((Version) SemanticVersion.parse(BUILT_IN_SINCE)) >= 0;
		} catch (VersionParsingException e) {
			// An unreadable version is treated as an old one: the safe answer is to ask for Indium.
			return false;
		}
	}
}
