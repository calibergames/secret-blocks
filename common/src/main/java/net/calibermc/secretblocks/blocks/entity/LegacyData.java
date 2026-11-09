package net.calibermc.secretblocks.blocks.entity;

import com.mojang.datafixers.DSL;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

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
 * <li>stored item stacks still in the format used before 1.20.5
 * ({@code Count} and {@code tag}; later versions save {@code count} and
 * {@code components}).</li>
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
		int from = tag.contains(VERSION, Tag.TAG_INT) ? tag.getInt(VERSION) : OLDEST;
		int to = current();
		if (from >= to) {
			return tag;
		}
		tag = tag.copy();
		for (Direction face : Direction.values()) {
			String key = face.getName() + "State";
			if (tag.contains(key, Tag.TAG_COMPOUND)) {
				tag.put(key, fix(References.BLOCK_STATE, tag.getCompound(key), from, to));
			}
		}
		if (tag.contains(originalKey, Tag.TAG_STRING)) {
			tag.put(originalKey, fix(References.BLOCK_NAME, tag.get(originalKey), from, to));
		}
		tag.putInt(VERSION, to);
		return tag;
	}

	/**
	 * Brings each stack of the {@code Items} list in {@code tag} that is still in
	 * the old item format up to date, in place, keeping its slot.
	 */
	public static void upgradeItems(CompoundTag tag) {
		if (!tag.contains("Items", Tag.TAG_LIST)) {
			return;
		}
		ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
		for (int i = 0; i < items.size(); i++) {
			CompoundTag item = items.getCompound(i);
			if (item.contains("Count", Tag.TAG_ANY_NUMERIC) && !item.contains("count")) {
				CompoundTag fixed = (CompoundTag) fix(References.ITEM_STACK, item, OLDEST, current());
				if (item.contains("Slot", Tag.TAG_ANY_NUMERIC)) {
					fixed.putByte("Slot", item.getByte("Slot"));
				}
				items.set(i, fixed);
			}
		}
	}

	private static Tag fix(DSL.TypeReference type, Tag data, int from, int to) {
		return DataFixers.getDataFixer().update(type, new Dynamic<>(NbtOps.INSTANCE, data), from, to).getValue();
	}
}
