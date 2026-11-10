package net.calibermc.secretblocks.blocks.entity;

import com.mojang.datafixers.DSL;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import java.util.List;

/**
 * Upgrades block entity data saved by older versions of the game. Minecraft's
 * own data fixers upgrade only the block entities it knows, never a mod's, so
 * the mod's block entities run the parts that name blocks and items through
 * those fixers themselves when they load:
 * <ul>
 * <li>the disguise's block states and the remembered original block (blocks
 * are renamed between versions: {@code minecraft:grass} is
 * {@code minecraft:short_grass} from 1.20.3), when the data carries no
 * {@link #VERSION} (everything saved before this version);</li>
 * <li>the vanilla parts of a secret container's data (its stored item
 * stacks, its name and its lock), through the fixers of the vanilla block
 * entity it stands in for: item stacks moved to {@code count} and
 * {@code components} in 1.20.5, and a lock became an item predicate
 * ({@code lock}) in place of a name ({@code Lock}) in 1.21.2.</li>
 * </ul>
 */
public final class LegacyData {

	/** The game data version the disguise was saved with; written with every disguise from this version on. */
	public static final String VERSION = "secretblocks:data_version";

	/** Data without a {@link #VERSION} counts as 1.19.2's, the oldest version the mod was released for. */
	private static final int OLDEST = 3120;

	private LegacyData() {
	}

	private static int current() {
		return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
	}

	/** Marks {@code tag} as saved by this version. */
	public static void markCurrent(CompoundTag tag) {
		tag.putInt(VERSION, current());
	}

	/**
	 * {@code tag} with the disguise's block states and original block brought up
	 * to date: {@code tag} itself when it is current, else an upgraded copy.
	 */
	public static CompoundTag upgradeDisguise(CompoundTag tag, String originalKey) {
		int from = tag.get(VERSION) instanceof IntTag ? tag.getIntOr(VERSION, 0) : OLDEST;
		int to = current();
		if (from >= to) {
			return tag;
		}
		tag = tag.copy();
		for (Direction face : Direction.values()) {
			String key = face.getName() + "State";
			if (tag.get(key) instanceof CompoundTag) {
				tag.put(key, fix(References.BLOCK_STATE, tag.getCompoundOrEmpty(key), from, to));
			}
		}
		if (tag.get(originalKey) instanceof StringTag) {
			tag.put(originalKey, fix(References.BLOCK_NAME, tag.get(originalKey), from, to));
		}
		tag.putInt(VERSION, to);
		return tag;
	}

	/**
	 * Brings the vanilla parts of a secret container's data up to date, in
	 * place: {@code tag} is run through the fixers of the vanilla block entity
	 * {@code vanillaId} it stands in for ({@code minecraft:chest},
	 * {@code minecraft:hopper} or {@code minecraft:dispenser}), from the
	 * version it was saved with. Its own keys are left as they are; the disguise
	 * is brought up to date when it loads ({@link #upgradeDisguise}).
	 */
	public static void upgradeContainer(CompoundTag tag, String vanillaId) {
		int from = tag.get(VERSION) instanceof IntTag ? tag.getIntOr(VERSION, 0) : OLDEST;
		int to = current();
		if (from >= to) {
			return;
		}
		Tag id = tag.get("id");
		CompoundTag data = tag.copy();
		data.putString("id", vanillaId);
		CompoundTag fixed = (CompoundTag) fix(References.BLOCK_ENTITY, data, from, to);
		if (from < TEXT_AS_TAGS) {
			unflattenLockName(fixed);
		}
		for (String key : List.copyOf(tag.keySet())) {
			tag.remove(key);
		}
		for (String key : fixed.keySet()) {
			tag.put(key, fixed.get(key));
		}
		if (id != null) {
			tag.put("id", id);
		} else {
			tag.remove("id");
		}
	}

	/** The data version from which text components are saved as tags, not JSON strings (Minecraft 1.21.5). */
	private static final int TEXT_AS_TAGS = 4290;

	/**
	 * The name a lock asks for, brought from a JSON string to a text tag. The
	 * game's own upgrade turns a lock saved before 1.21.2 into a predicate whose
	 * name is a JSON string, then from 1.21.5 on reads that string as plain text
	 * (quotes and all), so the lock would open for no item. Called after the
	 * game's own upgrade, for data saved before 1.21.5.
	 */
	private static void unflattenLockName(CompoundTag tag) {
		if (tag.get("lock") instanceof CompoundTag lock && lock.get("components") instanceof CompoundTag components
				&& components.get("minecraft:custom_name") instanceof StringTag name) {
			try {
				Component text = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(name.value()))
						.getOrThrow();
				components.put("minecraft:custom_name", ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, text).getOrThrow());
			} catch (RuntimeException notJson) {
				// Already plain text: left as it is.
			}
		}
	}

	private static Tag fix(DSL.TypeReference type, Tag data, int from, int to) {
		return DataFixers.getDataFixer().update(type, new Dynamic<>(NbtOps.INSTANCE, data), from, to).getValue();
	}
}
