package com.shouyun.inventorylens.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SlotPosition;

final class VanillaGuiLayouts {
    private VanillaGuiLayouts() { }
    static List<SlotPosition> grid(int count, int columns, int x, int y) {
        List<SlotPosition> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) slots.add(new SlotPosition(i, x + i % columns * 18, y + i / columns * 18));
        return slots;
    }
    static ContainerGuiDefinition definition(String texture, int originalHeight, int bodyHeight,
            int footerV, boolean centered, List<SlotPosition> slots) {
        return new ContainerGuiDefinition(ResourceLocation.withDefaultNamespace("textures/gui/container/" + texture + ".png"),
                176, originalHeight, bodyHeight, footerV, 7, centered, 8, 6, slots, List.of());
    }
}
