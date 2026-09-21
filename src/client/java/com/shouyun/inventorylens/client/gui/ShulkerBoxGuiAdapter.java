package com.shouyun.inventorylens.client.gui;
import com.shouyun.inventorylens.container.ContainerSnapshot;
/** ShulkerBoxScreen (167px), ShulkerBoxMenu. Color does not affect the vanilla GUI. */
public final class ShulkerBoxGuiAdapter implements ContainerGuiAdapter {
    public ContainerGuiDefinition define(ContainerSnapshot snapshot) {
        return VanillaGuiLayouts.definition("shulker_box", 167, 71, 160, false, VanillaGuiLayouts.grid(27, 9, 8, 18));
    }
}
