package com.shouyun.inventorylens.network;

import com.shouyun.inventorylens.server.ContainerSnapshotProvider;
import com.shouyun.inventorylens.server.RequestRateLimiter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.Util;

/** Fabric registration boundary. Container resolution and validation do not depend on Fabric. */
public final class InventoryLensNetworking {
	private InventoryLensNetworking() {
	}

	public static void initialize() {
		PayloadTypeRegistry.playC2S().register(ContainerSnapshotRequestPayload.TYPE, ContainerSnapshotRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ContainerSnapshotPayload.TYPE, ContainerSnapshotPayload.STREAM_CODEC);
		ContainerSnapshotProvider provider = new ContainerSnapshotProvider();
		RequestRateLimiter limiter = new RequestRateLimiter();
		ServerPlayNetworking.registerGlobalReceiver(ContainerSnapshotRequestPayload.TYPE, (request, context) -> {
			if (limiter.allow(context.player().getUUID(), Util.getMillis())
					&& ServerPlayNetworking.canSend(context.player(), ContainerSnapshotPayload.TYPE)) {
				ServerPlayNetworking.send(context.player(), provider.snapshot(context.player(), request));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> limiter.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> limiter.clear());
	}
}
