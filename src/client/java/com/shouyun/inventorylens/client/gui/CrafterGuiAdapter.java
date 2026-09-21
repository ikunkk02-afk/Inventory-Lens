package com.shouyun.inventorylens.client.gui;

import java.util.ArrayList;
import java.util.List;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerProperties;
import net.minecraft.resources.ResourceLocation;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SlotPosition;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SpriteElement;

/** CrafterMenu / CrafterScreen. Result preview is intentionally empty; it is not stored inventory. */
public final class CrafterGuiAdapter implements ContainerGuiAdapter {
    public ContainerGuiDefinition define(ContainerSnapshot snapshot) {
        var data = (ContainerProperties.Crafter)snapshot.properties();
        var gui = VanillaGuiLayouts.definition("crafter", 166, 71, 159, true, VanillaGuiLayouts.grid(9, 3, 26, 17));
        List<SpriteElement> elements = new ArrayList<>();
        List<SlotPosition> enabled = new ArrayList<>();
        for (var slot : gui.slotLayout()) {
            if ((data.disabledSlots() & (1 << slot.index())) != 0)
                elements.add(element("disabled_slot", slot.x() - 1, slot.y() - 1, 18));
            else enabled.add(slot);
        }
        // Screen center (+9, -48) relative to original 176x166 gives (97,35).
        elements.add(element(data.powered() ? "powered_redstone" : "unpowered_redstone", 97, 35, 16));
        return new ContainerGuiDefinition(gui.texture(), gui.guiWidth(), gui.guiHeight(), gui.bodyHeight(),
                gui.footerV(), gui.footerHeight(), gui.centeredTitle(), gui.titleX(), gui.titleY(), enabled, elements);
    }
    private static SpriteElement element(String name, int x, int y, int size) {
        return new SpriteElement(ResourceLocation.withDefaultNamespace("container/crafter/" + name), size, size, 0, 0, x, y, size, size);
    }
}
