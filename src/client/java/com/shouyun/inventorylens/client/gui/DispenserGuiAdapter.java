package com.shouyun.inventorylens.client.gui;
import com.shouyun.inventorylens.container.ContainerSnapshot;
/** DispenserScreen / DispenserMenu, also used by vanilla DropperBlockEntity. */
public final class DispenserGuiAdapter implements ContainerGuiAdapter {
    public ContainerGuiDefinition define(ContainerSnapshot snapshot) {
        return VanillaGuiLayouts.definition("dispenser", 166, 71, 159, true, VanillaGuiLayouts.grid(9, 3, 62, 17));
    }
}
