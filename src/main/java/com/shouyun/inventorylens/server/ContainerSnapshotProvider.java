package com.shouyun.inventorylens.server;

import java.util.function.Function;

import com.shouyun.inventorylens.container.*;
import com.shouyun.inventorylens.mixin.ContainerLockAccessor;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload.Status;
import com.shouyun.inventorylens.network.ContainerSnapshotRequestPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Pure Minecraft code, called only on the logical server thread. Never opens a menu. */
public final class ContainerSnapshotProvider {
	private final Function<BaseContainerBlockEntity, LockCode> locks;
    private final Function<BlockEntity, ContainerProperties> properties;

	public ContainerSnapshotProvider() {
		this(container -> ((ContainerLockAccessor) container).inventorylens$getLockKey(), ContainerPropertyReader::read);
	}

	// Also permits testing permission checks without booting a loader/mixin environment.
	ContainerSnapshotProvider(Function<BaseContainerBlockEntity, LockCode> locks) {
		this(locks, entity -> new ContainerProperties.None());
    }

    ContainerSnapshotProvider(Function<BaseContainerBlockEntity, LockCode> locks, Function<BlockEntity, ContainerProperties> properties) {
        this.locks = locks;
        this.properties = properties;
	}

	public ContainerSnapshotPayload snapshot(ServerPlayer player, ContainerSnapshotRequestPayload request) {
		if (!player.isAlive() || player.isRemoved() || player.getCamera() != player) {
			return unavailable(request.requestId());
		}
		ServerLevel level = player.serverLevel();
		ResolvedContainer target = ContainerRequestValidator.validate(request, level.dimension(), level,
				level::hasChunkAt, player.getEyePosition(), player.getViewVector(1), CollisionContext.of(player));
		return target == null ? unavailable(request.requestId())
				: read(request.requestId(), target, level, player.getMainHandItem(), player.isSpectator(), player.getEnderChestInventory());
	}

    ContainerSnapshotPayload read(long requestId, ResolvedContainer target, BlockGetter blocks,
            ItemStack key, boolean spectator) {
        return read(requestId, target, blocks, key, spectator, null);
    }
    ContainerSnapshotPayload read(long requestId, ResolvedContainer target, BlockGetter blocks,
            ItemStack key, boolean spectator, Container enderInventory) {
        for (BlockPos position : target.members()) {
            BlockEntity entity = blocks.getBlockEntity(position);
            if (entity == null || entity.isRemoved()) return unavailable(requestId);
            if (entity instanceof BaseContainerBlockEntity base && !spectator && !locks.apply(base).unlocksWith(key))
                return unavailable(requestId);
            // Validate ALL members before any getItem call can unpack loot.
            if (entity instanceof RandomizableContainerBlockEntity random && random.getLootTable() != null)
                return new ContainerSnapshotPayload(requestId, Status.UNGENERATED_LOOT, null);
        }
        ContainerSnapshot snapshot = ContainerResolverRegistry.read(target,
                new ContainerReadContext(blocks, enderInventory, properties));
        return snapshot == null ? unavailable(requestId) : new ContainerSnapshotPayload(requestId, Status.OK, snapshot);
    }

	private static ContainerSnapshotPayload unavailable(long requestId) {
		return new ContainerSnapshotPayload(requestId, Status.UNAVAILABLE, null);
	}
}
