package com.shouyun.inventorylens.server;

import java.util.function.Predicate;

import com.shouyun.inventorylens.container.ContainerSight;
import com.shouyun.inventorylens.container.ResolvedContainer;
import com.shouyun.inventorylens.container.VanillaContainerResolver;
import com.shouyun.inventorylens.network.ContainerSnapshotRequestPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

public final class ContainerRequestValidator {
	private ContainerRequestValidator() {
	}

	@Nullable
	public static ResolvedContainer validate(ContainerSnapshotRequestPayload request, ResourceKey<Level> dimension,
			BlockGetter blocks, Predicate<BlockPos> loaded, Vec3 eye, Vec3 look, CollisionContext collisionContext) {
		if (!dimension.equals(request.dimension())) {
			return null;
		}
		BlockHitResult hit = ContainerSight.pick(blocks, loaded, eye, look, collisionContext);
		if (hit == null) {
			return null;
		}
		// Resolve what the server ray hit, not the untrusted requested position.
		ResolvedContainer target = VanillaContainerResolver.resolve(dimension, blocks, loaded, hit.getBlockPos());
		return target != null && target.members().contains(request.position()) ? target : null;
	}
}
