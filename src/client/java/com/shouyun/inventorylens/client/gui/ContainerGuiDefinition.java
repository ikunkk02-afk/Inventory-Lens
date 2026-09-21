package com.shouyun.inventorylens.client.gui;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Vanilla source dimensions and cropped projection dimensions remain separate. */
public record ContainerGuiDefinition(ResourceLocation texture, int guiWidth, int guiHeight,
        int bodyHeight, int footerV, int footerHeight, boolean centeredTitle, int titleX, int titleY,
        List<SlotPosition> slotLayout, List<SpriteElement> progressElements) {
    public ContainerGuiDefinition {
        slotLayout = List.copyOf(slotLayout); progressElements = List.copyOf(progressElements);
    }
    public int width() { return guiWidth; }
    public int height() { return bodyHeight + footerHeight; }
    public record SlotPosition(int index, int x, int y) { }
    public record SpriteElement(ResourceLocation sprite, int fullWidth, int fullHeight,
            int u, int v, int x, int y, int width, int height) { }
}
