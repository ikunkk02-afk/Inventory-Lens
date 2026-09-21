package com.shouyun.inventorylens.client.gui;

import java.util.ArrayList;
import java.util.List;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SlotPosition;
import com.shouyun.inventorylens.client.gui.ContainerGuiDefinition.SpriteElement;

/** AbstractFurnaceMenu / AbstractFurnaceScreen formulas; the concrete screen supplies resource IDs. */
public final class FurnaceGuiAdapter implements ContainerGuiAdapter {
    private final String furnace;
    public FurnaceGuiAdapter(String furnace) { this.furnace = furnace; }
    public ContainerGuiDefinition define(ContainerSnapshot snapshot) {
        var data = (ContainerProperties.Furnace)snapshot.properties();
        var gui = VanillaGuiLayouts.definition(furnace, 166, 71, 159, true,
                List.of(new SlotPosition(0, 56, 17), new SlotPosition(1, 56, 53), new SlotPosition(2, 116, 35)));
        List<SpriteElement> elements = new ArrayList<>();
        if (data.litTime() > 0) {
            int n = flameHeight(data);
            elements.add(new SpriteElement(sprite("lit_progress"), 14, 14, 0, 14 - n, 56, 50 - n, 14, n));
        }
        int progress = arrowWidth(data);
        if (progress > 0) elements.add(new SpriteElement(sprite("burn_progress"), 24, 16, 0, 0, 79, 34, progress, 16));
        return new ContainerGuiDefinition(gui.texture(), gui.guiWidth(), gui.guiHeight(), gui.bodyHeight(),
                gui.footerV(), gui.footerHeight(), gui.centeredTitle(), gui.titleX(), gui.titleY(), gui.slotLayout(), elements);
    }
    public static int flameHeight(ContainerProperties.Furnace data) {
        if (data.litTime() <= 0) return 0;
        int duration = data.litDuration() == 0 ? 200 : data.litDuration();
        return Mth.ceil(Mth.clamp((float)data.litTime() / duration, 0, 1) * 13) + 1;
    }
    public static int arrowWidth(ContainerProperties.Furnace data) {
        return data.cookingProgress() == 0 || data.cookingTotalTime() == 0 ? 0
                : Mth.ceil(Mth.clamp((float)data.cookingProgress() / data.cookingTotalTime(), 0, 1) * 24);
    }
    private ResourceLocation sprite(String name) {
        return ResourceLocation.withDefaultNamespace("container/" + furnace + "/" + name);
    }
}
