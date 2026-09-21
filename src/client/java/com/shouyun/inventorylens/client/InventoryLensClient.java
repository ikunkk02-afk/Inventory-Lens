package com.shouyun.inventorylens.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;

import com.shouyun.inventorylens.client.render.WorldEquipmentRenderer;
import com.shouyun.inventorylens.client.target.EquipmentTargetTracker;
import com.shouyun.inventorylens.client.container.ContainerSnapshotCache;
import com.shouyun.inventorylens.client.container.ContainerTargetTracker;
import com.shouyun.inventorylens.client.render.WorldContainerRenderer;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload;
import com.shouyun.inventorylens.network.ContainerSnapshotRequestPayload;

public class InventoryLensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EquipmentTargetTracker tracker = new EquipmentTargetTracker();
		WorldEquipmentRenderer renderer = new WorldEquipmentRenderer();
		ContainerSnapshotCache containerCache = new ContainerSnapshotCache();
		ContainerTargetTracker containerTracker = new ContainerTargetTracker(containerCache);
		WorldContainerRenderer containerRenderer = new WorldContainerRenderer();
		ClientPlayNetworking.registerGlobalReceiver(ContainerSnapshotPayload.TYPE, (payload, context) -> {
			// Recheck current world/target before accepting a response, even between render frames.
			containerTracker.update(context.client(), 1, ClientPlayNetworking.canSend(ContainerSnapshotRequestPayload.TYPE), containerRenderer.previewFocused());
			containerCache.receive(payload, Util.getMillis());
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			containerTracker.update(client, 1, ClientPlayNetworking.canSend(ContainerSnapshotRequestPayload.TYPE), containerRenderer.previewFocused());
			if (containerTracker.hitPosition() != null) {
				ContainerSnapshotRequestPayload request = containerCache.request(containerTracker.hitPosition(),
						Util.getMillis(), containerRenderer.previewFocused());
				if (request != null) {
					ClientPlayNetworking.send(request);
				}
			}
		});
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
			containerTracker.update(minecraft, context.camera().getPartialTickTime(),
					ClientPlayNetworking.canSend(ContainerSnapshotRequestPayload.TYPE), containerRenderer.previewFocused());
			ContainerSnapshot snapshot = containerCache.snapshot(Util.getMillis());
			if (snapshot != null && context.matrixStack() != null && containerTracker.hitResult() != null) {
				containerRenderer.render(minecraft, context.matrixStack(), context.camera(), snapshot, containerTracker.hitResult());
			} else {
				containerRenderer.resetPlacement();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			tracker.clear();
			renderer.resetPlacement();
			containerTracker.clear();
			containerRenderer.resetPlacement();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			renderer.close();
			containerRenderer.close();
			containerTracker.clear();
		});
	}
}
