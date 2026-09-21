package com.shouyun.inventorylens.client.gui;

import java.util.ArrayList;
import java.util.List;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SlotPosition;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SpriteElement;

/** BrewingStandMenu / BrewingStandScreen, including its seven-step bubble cycle. */
public final class BrewingStandGuiAdapter implements ContainerGuiAdapter {
    private static final int[] BUBBLE_LENGTHS = {29, 24, 20, 16, 11, 6, 0};
    public ContainerGuiDefinition define(ContainerSnapshot snapshot) {
        var data = (ContainerProperties.Brewing)snapshot.properties();
        // Middle potion occupies y=58..73, so the standard 71px crop would cut the icon.
        var gui = VanillaGuiLayouts.definition("brewing_stand", 166, 76, 159, true,
                List.of(new SlotPosition(0, 56, 51), new SlotPosition(1, 79, 58), new SlotPosition(2, 102, 51),
                        new SlotPosition(3, 79, 17), new SlotPosition(4, 17, 17)));
        List<SpriteElement> elements = new ArrayList<>();
        int fuel = (int)Mth.clamp((18L * data.fuel() + 19) / 20, 0L, 18L);
        if (fuel > 0) elements.add(element("fuel_length", 18, 4, 0, 0, 60, 44, fuel, 4));
        if (data.brewTime() > 0) {
            int progress = Mth.clamp((int)(28.0F * (1.0F - data.brewTime() / 400.0F)), 0, 28);
            if (progress > 0) elements.add(element("brew_progress", 9, 28, 0, 0, 97, 16, 9, progress));
            int bubbles = BUBBLE_LENGTHS[data.brewTime() / 2 % 7];
            if (bubbles > 0) elements.add(element("bubbles", 12, 29, 0, 29 - bubbles, 63, 43 - bubbles, 12, bubbles));
        }
        return new ContainerGuiDefinition(gui.texture(), gui.guiWidth(), gui.guiHeight(), gui.bodyHeight(),
                gui.footerV(), gui.footerHeight(), gui.centeredTitle(), gui.titleX(), gui.titleY(), gui.slotLayout(), elements);
    }
    private static SpriteElement element(String name, int fw, int fh, int u, int v, int x, int y, int w, int h) {
        return new SpriteElement(ResourceLocation.withDefaultNamespace("container/brewing_stand/" + name), fw, fh, u, v, x, y, w, h);
    }
}
