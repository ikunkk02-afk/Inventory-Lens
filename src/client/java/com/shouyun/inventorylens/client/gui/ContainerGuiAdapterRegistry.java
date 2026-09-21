package com.shouyun.inventorylens.client.gui;

import java.util.HashMap;
import java.util.Map;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Client-only visual extension point; unknown adapters produce no projection. */
public final class ContainerGuiAdapterRegistry {
    private static final Map<ResourceLocation, ContainerGuiAdapter> ADAPTERS = new HashMap<>();
    static { register(ResourceLocation.withDefaultNamespace("chest"), new ChestGuiAdapter());
        register(ResourceLocation.withDefaultNamespace("shulker_box"), new ShulkerBoxGuiAdapter());
        register(ResourceLocation.withDefaultNamespace("hopper"), new HopperGuiAdapter());
        register(ResourceLocation.withDefaultNamespace("dispenser"), new DispenserGuiAdapter());
        register(ResourceLocation.withDefaultNamespace("furnace"), new FurnaceGuiAdapter("furnace"));
        register(ResourceLocation.withDefaultNamespace("blast_furnace"), new FurnaceGuiAdapter("blast_furnace"));
        register(ResourceLocation.withDefaultNamespace("smoker"), new FurnaceGuiAdapter("smoker"));
        register(ResourceLocation.withDefaultNamespace("brewing_stand"), new BrewingStandGuiAdapter());
        register(ResourceLocation.withDefaultNamespace("crafter"), new CrafterGuiAdapter());
    }
    private ContainerGuiAdapterRegistry() { }
    public static void register(ResourceLocation id, ContainerGuiAdapter adapter) {
        if (ADAPTERS.putIfAbsent(id, java.util.Objects.requireNonNull(adapter)) != null)
            throw new IllegalArgumentException("Duplicate GUI adapter: " + id);
    }
    @Nullable public static ContainerGuiDefinition definition(ContainerSnapshot snapshot) {
        ContainerGuiAdapter adapter = ADAPTERS.get(snapshot.gui());
        return adapter == null ? null : adapter.define(snapshot);
    }
}
