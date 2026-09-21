package com.shouyun.inventorylens.server;

import com.shouyun.inventorylens.TestWorld;
import com.shouyun.inventorylens.container.ResolvedContainer;
import com.shouyun.inventorylens.network.ContainerSnapshotRequestPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerRequestValidatorTest {
	@BeforeAll
	static void initialize() {
		TestWorld.bootstrap();
	}

	@Test
	void realRayAllowsVisibleChestAndRejectsWallAndWrongDirection() {
		TestWorld world = new TestWorld();
		BlockPos chest = new BlockPos(0, 64, 4);
		world.states.put(chest, Blocks.CHEST.defaultBlockState());
		assertNotNull(validate(world, chest, new Vec3(0, 0, 1)));
		assertNull(validate(world, chest, new Vec3(0, 0, -1)));
		world.states.put(new BlockPos(0, 64, 2), Blocks.STONE.defaultBlockState());
		assertNull(validate(world, chest, new Vec3(0, 0, 1)));
		assertEquals(0, world.inventoryLookups);
	}

	@Test
	void rejectsWrongDimensionWithoutWorldAccess() {
		TestWorld world = new TestWorld();
		assertNull(ContainerRequestValidator.validate(
				new ContainerSnapshotRequestPayload(Level.NETHER, new BlockPos(0, 64, 3), 1),
				Level.OVERWORLD, world, world::loaded, new Vec3(0.5, 64.5, 0), new Vec3(0, 0, 1), CollisionContext.empty()));
		assertTrue(world.reads.isEmpty());
	}

	@Test
	void rejectsFarSpoofedAndUnloadedPositionsWithoutReadingTheirBlocks() {
		TestWorld world = new TestWorld();
		BlockPos far = new BlockPos(10000, 64, 10000);
		assertNull(validate(world, far, new Vec3(0, 0, 1)));
		assertFalse(world.reads.contains(far));
		BlockPos chest = new BlockPos(0, 64, 4);
		world.states.put(chest, Blocks.CHEST.defaultBlockState());
		world.unloaded.add(new BlockPos(0, 64, 2));
		world.reads.clear();
		assertNull(validate(world, chest, new Vec3(0, 0, 1)));
		assertTrue(world.reads.isEmpty(), "Preflight must reject before clipping touches unloaded cells");
		assertEquals(0, world.inventoryLookups);
	}

	@Test
	void distanceIsMeasuredToSurfaceAndCappedAtSixBlocks() {
		TestWorld world = new TestWorld();
		BlockPos chest = new BlockPos(0, 64, 6);
		world.states.put(chest, Blocks.CHEST.defaultBlockState());
		assertNotNull(validateAt(world, chest, new Vec3(0.5, 64.5, 0.0635)));
		assertNull(validateAt(world, chest, new Vec3(0.5, 64.5, 0.0615)));
	}

	private ResolvedContainer validate(TestWorld world, BlockPos pos, Vec3 direction) {
		return ContainerRequestValidator.validate(new ContainerSnapshotRequestPayload(Level.OVERWORLD, pos, 1),
				Level.OVERWORLD, world, world::loaded, new Vec3(0.5, 64.5, 0), direction, CollisionContext.empty());
	}

	private ResolvedContainer validateAt(TestWorld world, BlockPos pos, Vec3 eye) {
		return ContainerRequestValidator.validate(new ContainerSnapshotRequestPayload(Level.OVERWORLD, pos, 1),
				Level.OVERWORLD, world, world::loaded, eye, new Vec3(0, 0, 1), CollisionContext.empty());
	}
}
