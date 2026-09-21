package com.shouyun.inventorylens.client.container;

import com.shouyun.inventorylens.container.ContainerSight;
import com.shouyun.inventorylens.container.ResolvedContainer;
import com.shouyun.inventorylens.container.ContainerResolverRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

public final class ContainerTargetTracker {
	private final ContainerSnapshotCache cache;
	@Nullable private ClientLevel world;
	@Nullable private BlockPos hitPosition;
	@Nullable private BlockHitResult hitResult;

	public ContainerTargetTracker(ContainerSnapshotCache cache) {
		this.cache = cache;
	}

	public void update(Minecraft minecraft, float partialTick, boolean channelAvailable) {
		if (world != minecraft.level) {
			clear();
			world = minecraft.level;
		}
		hitPosition = null;
		hitResult = null;
		if (!channelAvailable || world == null || minecraft.player == null || !minecraft.player.isAlive()
				|| minecraft.getCameraEntity() != minecraft.player || minecraft.screen != null || minecraft.options.hideGui) {
			cache.setTarget(null);
			return;
		}
		Vec3 eye = minecraft.player.getEyePosition(partialTick);
		BlockHitResult hit = ContainerSight.pick(world, world::hasChunkAt, eye,
				minecraft.player.getViewVector(partialTick), CollisionContext.of(minecraft.player));
		if (hit == null || (minecraft.hitResult instanceof EntityHitResult entityHit
				&& eye.distanceToSqr(entityHit.getLocation()) <= eye.distanceToSqr(hit.getLocation()))) {
			cache.setTarget(null);
			return;
		}
		ResolvedContainer target = ContainerResolverRegistry.resolve(world, hit.getBlockPos());
		cache.setTarget(target);
		if (target != null) {
			hitPosition = hit.getBlockPos().immutable();
			hitResult = hit;
		}
	}

	@Nullable
	public BlockPos hitPosition() {
		return hitPosition;
	}

	@Nullable
	public BlockHitResult hitResult() {
		return hitResult;
	}

	public void clear() {
		cache.clear();
		hitPosition = null;
		hitResult = null;
		world = null;
	}
}
