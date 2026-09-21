package com.shouyun.inventorylens.client.gui;

import com.shouyun.inventorylens.container.ContainerSnapshot;

/** 1.21.1 ContainerScreen.renderBg / ChestMenu: preserve vanilla row and texture coordinates. */
public final class ChestGuiAdapter implements ContainerGuiAdapter {
    public ContainerGuiDefinition define(ContainerSnapshot snapshot) {
        int rows = snapshot.items().size() / 9;
        return VanillaGuiLayouts.definition("generic_54", 114 + rows * 18, 17 + rows * 18, 215,
                false, VanillaGuiLayouts.grid(rows * 9, 9, 8, 18));
    }
}
