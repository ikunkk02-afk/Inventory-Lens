package com.shouyun.inventorylens.container;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

public final class ContainerSight {
	public static final double RANGE = 6.0;

	private ContainerSight() {
	}

	@Nullable
	public static BlockHitResult pick(BlockGetter blocks, Predicate<BlockPos> loaded, Vec3 eye, Vec3 direction,
			CollisionContext collisionContext) {
		Vec3 end = eye.add(direction.scale(RANGE));
		// Preflight the exact bounded ray, including the epsilon cells used by vanilla clipping.
		// Neither the preflight nor a subsequently rejected request may load a chunk.
		boolean ready = BlockGetter.traverseBlocks(eye, end, loaded,
				(predicate, pos) -> predicate.test(pos) ? null : Boolean.FALSE, predicate -> Boolean.TRUE);
		if (!ready) {
			return null;
		}
		BlockHitResult hit = blocks.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
				ClipContext.Fluid.NONE, collisionContext));
		return hit.getType() == HitResult.Type.BLOCK && !hit.isInside()
				&& eye.distanceToSqr(hit.getLocation()) <= RANGE * RANGE ? hit : null;
	}
}
