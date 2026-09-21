package com.shouyun.inventorylens.network;

import com.shouyun.inventorylens.InventoryLens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ContainerSnapshotRequestPayload(ResourceKey<Level> dimension, BlockPos position, long requestId)
		implements CustomPacketPayload {
	public static final Type<ContainerSnapshotRequestPayload> TYPE = new Type<>(InventoryLens.id("container_request_v1"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ContainerSnapshotRequestPayload> STREAM_CODEC =
			new StreamCodec<>() {
				@Override
				public ContainerSnapshotRequestPayload decode(RegistryFriendlyByteBuf buffer) {
					return new ContainerSnapshotRequestPayload(ResourceKey.create(Registries.DIMENSION,
							buffer.readResourceLocation()), buffer.readBlockPos(), buffer.readVarLong());
				}

				@Override
				public void encode(RegistryFriendlyByteBuf buffer, ContainerSnapshotRequestPayload payload) {
					buffer.writeResourceLocation(payload.dimension.location());
					buffer.writeBlockPos(payload.position);
					buffer.writeVarLong(payload.requestId);
				}
			};

	@Override
	public Type<ContainerSnapshotRequestPayload> type() {
		return TYPE;
	}
}
