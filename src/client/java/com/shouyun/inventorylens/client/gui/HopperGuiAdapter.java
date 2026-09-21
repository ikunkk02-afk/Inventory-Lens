package com.shouyun.inventorylens.client.gui;
import com.shouyun.inventorylens.container.ContainerSnapshot;
/** HopperScreen / HopperMenu: five slots at (44 + i*18, 20), original height 133. */
public final class HopperGuiAdapter implements ContainerGuiAdapter {
    public ContainerGuiDefinition define(ContainerSnapshot snapshot) {
        return VanillaGuiLayouts.definition("hopper", 133, 38, 126, false, VanillaGuiLayouts.grid(5, 5, 44, 20));
    }
}
