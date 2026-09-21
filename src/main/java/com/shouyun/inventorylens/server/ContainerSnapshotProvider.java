package com.shouyun.inventorylens.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerType;
import com.shouyun.inventorylens.container.ResolvedContainer;
import com.shouyun.inventorylens.mixin.ContainerLockAccessor;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload.Status;
import com.shouyun.inventorylens.network.ContainerSnapshotRequestPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Pure Minecraft code, called only on the logical server thread. Never opens a menu. */
public final class ContainerSnapshotProvider {
	private final Function<BaseContainerBlockEntity, LockCode> locks;

	public ContainerSnapshotProvider() {
		this(container -> ((ContainerLockAccessor) container).inventorylens$getLockKey());
	}

	// Also permits testing permission checks without booting a loader/mixin environment.
	ContainerSnapshotProvider(Function<BaseContainerBlockEntity, LockCode> locks) {
		this.locks = locks;
	}

	public ContainerSnapshotPayload snapshot(ServerPlayer player, ContainerSnapshotRequestPayload request) {
		if (!player.isAlive() || player.isRemoved() || player.getCamera() != player) {
			return unavailable(request.requestId());
		}
		ServerLevel level = player.serverLevel();
		ResolvedContainer target = ContainerRequestValidator.validate(request, level.dimension(), level,
				level::hasChunkAt, player.getEyePosition(), player.getViewVector(1), CollisionContext.of(player));
		return target == null ? unavailable(request.requestId())
				: read(request.requestId(), target, level, player.getMainHandItem(), player.isSpectator());
	}

	ContainerSnapshotPayload read(long requestId, ResolvedContainer target, BlockGetter blocks,
			ItemStack key, boolean spectator) {
		List<RandomizableContainerBlockEntity> members = new ArrayList<>(2);
		for (BlockPos position : target.members()) {
			BlockEntity blockEntity = blocks.getBlockEntity(position);
			boolean expected = target.type() == ContainerType.BARREL
					? blockEntity instanceof BarrelBlockEntity : blockEntity instanceof ChestBlockEntity;
			if (!expected || blockEntity.isRemoved()) {
				return unavailable(requestId);
			}
			RandomizableContainerBlockEntity container = (RandomizableContainerBlockEntity) blockEntity;
			if (!spectator && !locks.apply(container).unlocksWith(key)) {
				return unavailable(requestId);
			}
			// getItem and even isEmpty would unpack loot. Check EVERY member before either call.
			if (container.getLootTable() != null) {
				return new ContainerSnapshotPayload(requestId, Status.UNGENERATED_LOOT, null);
			}
			members.add(container);
		}
		Container inventory = members.size() == 2
				? new CompoundContainer(members.get(0), members.get(1)) : members.getFirst();
		if (inventory.getContainerSize() != target.type().slots()) {
			return unavailable(requestId);
		}
		List<ItemStack> stacks = new ArrayList<>(inventory.getContainerSize());
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			stacks.add(inventory.getItem(slot));
		}
		return new ContainerSnapshotPayload(requestId, Status.OK, new ContainerSnapshot(target, stacks));
	}

	private static ContainerSnapshotPayload unavailable(long requestId) {
		return new ContainerSnapshotPayload(requestId, Status.UNAVAILABLE, null);
	}
}
