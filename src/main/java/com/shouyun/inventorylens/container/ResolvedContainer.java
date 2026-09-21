package com.shouyun.inventorylens.container;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Members are in vanilla inventory order, independently of the canonical identity position. */
public record ResolvedContainer(ContainerIdentity identity, ContainerType type, List<BlockPos> members,
		Direction facing) {
	public ResolvedContainer {
		members = members.stream().map(BlockPos::immutable).toList();
		if (members.size() != (type == ContainerType.DOUBLE_CHEST ? 2 : 1)
				|| !members.contains(identity.position())) {
			throw new IllegalArgumentException("Invalid container members");
		}
	}

	public Vec3 center() {
		Vec3 first = Vec3.atCenterOf(members.getFirst());
		return members.size() == 1 ? first : first.add(Vec3.atCenterOf(members.get(1))).scale(0.5);
	}

	public Vec3 anchor() {
		// The chest front is 1/16 inside its block; barrels occupy the full block.
		double offset = (type == ContainerType.BARREL ? 0.5 : 7.0 / 16.0) + 0.02;
		return center().add(facing.getStepX() * offset, facing.getStepY() * offset,
				facing.getStepZ() * offset);
	}
}
