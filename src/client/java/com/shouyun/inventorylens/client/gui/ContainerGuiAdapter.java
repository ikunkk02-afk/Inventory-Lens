package com.shouyun.inventorylens.client.gui;
import com.shouyun.inventorylens.container.ContainerSnapshot;
@FunctionalInterface
public interface ContainerGuiAdapter {
    ContainerGuiDefinition define(ContainerSnapshot snapshot);
}
