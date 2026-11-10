package net.calibermc.secretblocks.blocks.entity;

import net.calibermc.secretblocks.mixin.TagValueInputAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity data as tags, for the code that reads and writes it key by key
 * (disguises, legacy upgrades). The keys are written and read exactly as tags
 * hold them, so the saved data is the same as before.
 */
public final class SavedData {

	private SavedData() {
	}

	/** The data {@code input} reads from (a copy), or an empty tag when it is not tag data. */
	public static CompoundTag read(ValueInput input) {
		return input instanceof TagValueInput ? ((TagValueInputAccessor) input).secretblocks$input().copy() : new CompoundTag();
	}

	/** Writes every key of {@code tag} to {@code output} as it is. */
	public static void write(ValueOutput output, CompoundTag tag) {
		for (String key : tag.keySet()) {
			output.store(key, ExtraCodecs.NBT, tag.get(key));
		}
	}

	/** Input that reads {@code tag}. */
	public static ValueInput input(CompoundTag tag, HolderLookup.Provider registries) {
		return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
	}

	/**
	 * {@code input}, with a secret container's data saved by an older version
	 * brought up to date through the vanilla container {@code vanillaId}'s fixers
	 * ({@link LegacyData#upgradeContainer}).
	 */
	public static ValueInput upgradedContainer(ValueInput input, String vanillaId) {
		CompoundTag tag = read(input);
		CompoundTag before = tag.copy();
		LegacyData.upgradeContainer(tag, vanillaId);
		return tag.equals(before) ? input : input(tag, input.lookup());
	}
}
