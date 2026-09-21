package com.shouyun.inventorylens.container;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Registered descriptors, independent of loader and client rendering classes. */
public record ContainerType(ResourceLocation id, int slots, int memberCount, ResourceLocation gui,
        String translationKey, boolean insetBounds, ContainerProperties.Kind propertiesKind) {
    private static final Map<ResourceLocation, ContainerType> TYPES = new LinkedHashMap<>();
    public static final ContainerType CHEST = vanilla("chest", 27, 1, "chest", "chest", true);
    public static final ContainerType DOUBLE_CHEST = vanilla("double_chest", 54, 2, "chest", "chestDouble", true);
    public static final ContainerType BARREL = vanilla("barrel", 27, 1, "chest", "barrel", false);
    public static final ContainerType TRAPPED_CHEST = vanilla("trapped_chest", 27, 1, "chest", "chest", true);
    public static final ContainerType DOUBLE_TRAPPED_CHEST = vanilla("double_trapped_chest", 54, 2, "chest", "chestDouble", true);

    public static final ContainerType SHULKER_BOX = vanilla("shulker_box", 27, 1, "shulker_box", "shulkerBox", false);

    public static final ContainerType HOPPER = vanilla("hopper", 5, 1, "hopper", "hopper", false);

    public static final ContainerType DISPENSER = vanilla("dispenser", 9, 1, "dispenser", "dispenser", false);
    public static final ContainerType DROPPER = vanilla("dropper", 9, 1, "dispenser", "dropper", false);

    public static final ContainerType ENDER_CHEST = vanilla("ender_chest", 27, 1, "chest", "enderchest", true);

    public static final ContainerType FURNACE = register(new ContainerType(ResourceLocation.withDefaultNamespace("furnace"), 3, 1,
            ResourceLocation.withDefaultNamespace("furnace"), "container.furnace", false, ContainerProperties.Kind.FURNACE));

    public static final ContainerType BLAST_FURNACE = register(new ContainerType(ResourceLocation.withDefaultNamespace("blast_furnace"), 3, 1,
            ResourceLocation.withDefaultNamespace("blast_furnace"), "container.blast_furnace", false, ContainerProperties.Kind.FURNACE));

    public static final ContainerType SMOKER = register(new ContainerType(ResourceLocation.withDefaultNamespace("smoker"), 3, 1,
            ResourceLocation.withDefaultNamespace("smoker"), "container.smoker", false, ContainerProperties.Kind.FURNACE));

    public static final ContainerType BREWING_STAND = register(new ContainerType(ResourceLocation.withDefaultNamespace("brewing_stand"), 5, 1,
            ResourceLocation.withDefaultNamespace("brewing_stand"), "container.brewing", true, ContainerProperties.Kind.BREWING));

    public static final ContainerType CRAFTER = register(new ContainerType(ResourceLocation.withDefaultNamespace("crafter"), 9, 1,
            ResourceLocation.withDefaultNamespace("crafter"), "container.crafter", false, ContainerProperties.Kind.CRAFTER));

    public ContainerType {
        if (slots < 1 || slots > 256 || memberCount < 1 || memberCount > 2) throw new IllegalArgumentException("Invalid container size");
        java.util.Objects.requireNonNull(id);
        java.util.Objects.requireNonNull(gui);
        java.util.Objects.requireNonNull(translationKey);
        java.util.Objects.requireNonNull(propertiesKind);
    }
    public static ContainerType register(ContainerType type) {
        if (TYPES.putIfAbsent(type.id(), type) != null) throw new IllegalArgumentException("Duplicate container type: " + type.id());
        return type;
    }
    private static ContainerType vanilla(String id, int slots, int members, String gui, String title, boolean chest) {
        return register(new ContainerType(ResourceLocation.withDefaultNamespace(id), slots, members,
                ResourceLocation.withDefaultNamespace(gui), "container." + title, chest, ContainerProperties.Kind.NONE));
    }
    public static ContainerType byId(ResourceLocation id) { return TYPES.get(id); }
    public static ContainerType[] values() { return TYPES.values().toArray(ContainerType[]::new); }
}
