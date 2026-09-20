package com.shouyun.inventorylens.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

import com.shouyun.inventorylens.client.render.WorldEquipmentRenderer;
import com.shouyun.inventorylens.client.target.EquipmentTargetTracker;

public class InventoryLensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EquipmentTargetTracker tracker = new EquipmentTargetTracker();
		WorldEquipmentRenderer renderer = new WorldEquipmentRenderer();
		WorldRenderEvents.LAST.register(context -> {
			Minecraft minecraft = Minecraft.getInstance();
			tracker.update(minecraft, context.camera().getPartialTickTime());
			if (tracker.target() != null && context.matrixStack() != null) {
				float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(
						!context.world().tickRateManager().isEntityFrozen(tracker.target()));
				renderer.render(minecraft, context.matrixStack(), context.camera(), partialTick, tracker);
			} else {
				renderer.resetPlacement();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			tracker.clear();
			renderer.resetPlacement();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
	}
}
