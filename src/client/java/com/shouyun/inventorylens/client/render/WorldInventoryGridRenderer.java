package com.shouyun.inventorylens.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Slot coordinates only; all transforms, lighting and item drawing use the shared world renderer. */
public final class WorldInventoryGridRenderer {
	public static final int SLOT_SIZE = 18;
	public static final int WIDTH = 176;
	private static final int ITEM_X = 8;
	private static final int ITEM_Y = 18;
	private static final int HEADER_HEIGHT = 17;
	private static final int FOOTER_HEIGHT = 7;
	private static final ResourceLocation BACKGROUND =
			ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final Component CHEST_TITLE = Component.translatable("container.chest");
	private static final Component DOUBLE_CHEST_TITLE = Component.translatable("container.chestDouble");
	private static final Component BARREL_TITLE = Component.translatable("container.barrel");

	public static int height(ContainerType type) {
		return HEADER_HEIGHT + type.rows() * SLOT_SIZE + FOOTER_HEIGHT;
	}

	public void render(Minecraft minecraft, PoseStack pose, WorldItemRenderer items, ContainerSnapshot snapshot) {
		ContainerType type = snapshot.container().type();
		int bodyHeight = HEADER_HEIGHT + type.rows() * SLOT_SIZE;
		// The same header and slots used by ContainerScreen, capped with the texture's bottom border.
		// Crop out the player inventory: this panel previews only the targeted container.
		items.blit(pose, BACKGROUND, 0, 0, WIDTH, bodyHeight, 0, 0, 256, 256, 0);
		items.blit(pose, BACKGROUND, 0, bodyHeight, WIDTH, FOOTER_HEIGHT, 0, 215, 256, 256, 0);
		items.flush();
		Component title = switch (type) {
			case CHEST -> CHEST_TITLE;
			case DOUBLE_CHEST -> DOUBLE_CHEST_TITLE;
			case BARREL -> BARREL_TITLE;
		};
		items.label(minecraft, pose, title, 8, 6, 0xFF404040);
		int seed = snapshot.container().identity().position().hashCode();
		for (int slot = 0; slot < snapshot.items().size(); slot++) {
			ItemStack stack = snapshot.items().get(slot);
			if (!stack.isEmpty()) {
				items.render(minecraft, pose, stack, minecraft.level, null,
						ITEM_X + slot % 9 * SLOT_SIZE, ITEM_Y + slot / 9 * SLOT_SIZE, seed + slot);
			}
		}
		items.flush();
	}
}
