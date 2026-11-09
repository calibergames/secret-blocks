package net.calibermc.secretblocks.registry;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.SecretChest;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretComparatorBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretDispenserBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretGateSegmentBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretHopperBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretSculkSensorBlockEntity;
import net.calibermc.secretblocks.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Arrays;
import java.util.Set;

import static net.calibermc.secretblocks.SecretBlocks.id;

public class ModEntities {
    public static BlockEntityType<SecretBlockEntity> SECRET_BLOCK_ENTITY;
    public static BlockEntityType<SecretDispenserBlockEntity> SECRET_DISPENSER;
    public static BlockEntityType<SecretHopperBlockEntity> SECRET_HOPPER;
    public static BlockEntityType<SecretComparatorBlockEntity> SECRET_COMPARATOR;
    public static BlockEntityType<SecretSculkSensorBlockEntity> SECRET_SCULK_SENSOR;
    public static BlockEntityType<SecretGateSegmentBlockEntity> SECRET_GATE_SEGMENT;

    // Registry
    public static void registerAllEntities() {
        // Every secret block except those with a block entity type of their own.
        Block[] plainSecretBlocks = Arrays.stream(ModBlocks.secretBlocksList)
                .filter(block -> block != ModBlocks.SECRET_DISPENSER && block != ModBlocks.SECRET_DROPPER && block != ModBlocks.SECRET_HOPPER
                        && block != ModBlocks.SECRET_COMPARATOR && block != ModBlocks.SECRET_SCULK_SENSOR
                        && block != ModBlocks.SECRET_GATE_SEGMENT)
                .toArray(Block[]::new);
        // One type for all of them; the Secret Chest gets the container subclass,
        // both when placed and when loaded from a save.
        SECRET_BLOCK_ENTITY = Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("secret_block_entity_type"), type(
                        (pos, state) -> state.getBlock() instanceof SecretChest
                                ? new SecretChestBlockEntity(pos, state) : new SecretBlockEntity(pos, state),
                        plainSecretBlocks));

        SECRET_DISPENSER = Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("secret_dispenser"), type(SecretDispenserBlockEntity::new,
                        ModBlocks.SECRET_DISPENSER, ModBlocks.SECRET_DROPPER));

        SECRET_HOPPER = Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("secret_hopper"), type(SecretHopperBlockEntity::new,
                        ModBlocks.SECRET_HOPPER));

        SECRET_COMPARATOR = Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("secret_comparator"), type(SecretComparatorBlockEntity::new,
                        ModBlocks.SECRET_COMPARATOR));

        SECRET_SCULK_SENSOR = Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("secret_sculk_sensor"), type(SecretSculkSensorBlockEntity::new,
                        ModBlocks.SECRET_SCULK_SENSOR));

        SECRET_GATE_SEGMENT = Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("secret_gate_segment"), type(SecretGateSegmentBlockEntity::new,
                        ModBlocks.SECRET_GATE_SEGMENT));

        //Console Output
        System.out.println("Registering Entities for " + SecretBlocks.MOD_ID);
    }

    private static <T extends BlockEntity> BlockEntityType<T> type(BlockEntityType.BlockEntitySupplier<? extends T> factory, Block... blocks) {
        return new BlockEntityType<>(factory, Set.of(blocks));
    }
}
