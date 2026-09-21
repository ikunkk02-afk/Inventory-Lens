package com.shouyun.inventorylens.network;

import java.util.ArrayList;
import java.util.List;

import com.shouyun.inventorylens.InventoryLens;
import com.shouyun.inventorylens.container.ContainerIdentity;
import com.shouyun.inventorylens.container.ContainerProperties;
import net.minecraft.network.chat.ComponentSerialization;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerType;
import com.shouyun.inventorylens.container.ResolvedContainer;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record ContainerSnapshotPayload(long requestId, Status status, @Nullable ContainerSnapshot snapshot)
		implements CustomPacketPayload {
	public enum Status { OK, UNAVAILABLE, UNGENERATED_LOOT }

	public ContainerSnapshotPayload {
		if ((status == Status.OK) != (snapshot != null)) {
			throw new IllegalArgumentException("Only a successful response may contain a snapshot");
		}
	}

	public static final Type<ContainerSnapshotPayload> TYPE = new Type<>(InventoryLens.id("container_snapshot_v2"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ContainerSnapshotPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public ContainerSnapshotPayload decode(RegistryFriendlyByteBuf buffer) {
			long requestId = buffer.readVarLong();
			Status status = readEnum(buffer, Status.values());
			if (status != Status.OK) {
				return new ContainerSnapshotPayload(requestId, status, null);
			}
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation());
			BlockPos canonical = buffer.readBlockPos();
			ContainerType type = ContainerType.byId(buffer.readResourceLocation());
			if (type == null) throw new DecoderException("Unknown container type");
			Direction facing = readEnum(buffer, Direction.values());
			List<BlockPos> members = new ArrayList<>(2);
			members.add(buffer.readBlockPos());
			if (type.memberCount() == 2) {
				members.add(buffer.readBlockPos());
			}
			int count = buffer.readVarInt();
			if (count != type.slots()) {
				throw new DecoderException("Invalid inventory snapshot slot count: " + count);
			}
			List<ItemStack> items = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
			}
			try {
                ResolvedContainer resolved = new ResolvedContainer(new ContainerIdentity(dimension, canonical), type, members, facing);
                var gui = buffer.readResourceLocation();
                if (!gui.equals(type.gui())) throw new DecoderException("Mismatched GUI type");
                return new ContainerSnapshotPayload(requestId, status, new ContainerSnapshot(resolved, items, gui,
                        ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer), readProperties(buffer, type)));
            } catch (IllegalArgumentException error) {
                throw new DecoderException("Invalid container snapshot", error);
            }
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, ContainerSnapshotPayload payload) {
			buffer.writeVarLong(payload.requestId);
			buffer.writeVarInt(payload.status.ordinal());
			if (payload.snapshot == null) {
				return;
			}
			ResolvedContainer container = payload.snapshot.container();
			buffer.writeResourceLocation(container.identity().dimension().location());
			buffer.writeBlockPos(container.identity().position());
			buffer.writeResourceLocation(container.type().id());
			buffer.writeVarInt(container.facing().ordinal());
			container.members().forEach(buffer::writeBlockPos);
			buffer.writeVarInt(payload.snapshot.items().size());
			payload.snapshot.items().forEach(stack -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack));
            buffer.writeResourceLocation(payload.snapshot.gui());
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, payload.snapshot.title());
            writeProperties(buffer, payload.snapshot.properties());
		}
	};

    private static ContainerProperties readProperties(RegistryFriendlyByteBuf b, ContainerType type) {
        ContainerProperties.Kind kind = readEnum(b, ContainerProperties.Kind.values());
        if (kind != type.propertiesKind()) throw new DecoderException("Mismatched container properties");
        return switch (kind) {
            case NONE -> new ContainerProperties.None();
            case FURNACE -> new ContainerProperties.Furnace(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
            case BREWING -> new ContainerProperties.Brewing(b.readVarInt(), b.readVarInt());
            case CRAFTER -> {
                int mask = b.readVarInt();
                if ((mask & ~511) != 0) throw new DecoderException("Invalid disabled slots");
                yield new ContainerProperties.Crafter(mask, b.readBoolean());
            }
        };
    }
    private static void writeProperties(RegistryFriendlyByteBuf b, ContainerProperties properties) {
        b.writeVarInt(properties.kind().ordinal());
        switch (properties) {
            case ContainerProperties.None ignored -> { }
            case ContainerProperties.Furnace f -> {
                b.writeVarInt(f.litTime()); b.writeVarInt(f.litDuration());
                b.writeVarInt(f.cookingProgress()); b.writeVarInt(f.cookingTotalTime());
            }
            case ContainerProperties.Brewing brew -> { b.writeVarInt(brew.brewTime()); b.writeVarInt(brew.fuel()); }
            case ContainerProperties.Crafter c -> { b.writeVarInt(c.disabledSlots()); b.writeBoolean(c.powered()); }
        }
    }

	private static <T> T readEnum(RegistryFriendlyByteBuf buffer, T[] values) {
		int ordinal = buffer.readVarInt();
		if (ordinal < 0 || ordinal >= values.length) {
			throw new DecoderException("Invalid inventory snapshot enum: " + ordinal);
		}
		return values[ordinal];
	}

	@Override
	public Type<ContainerSnapshotPayload> type() {
		return TYPE;
	}
}
