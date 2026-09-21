package com.shouyun.inventorylens.container;

import com.shouyun.inventorylens.TestWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaContainerResolverTest {
	@BeforeAll
	static void initialize() {
		TestWorld.bootstrap();
	}

	@Test
	void doubleChestHasOneIdentityAnchorAndVanillaOrderInEveryDirection() {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			TestWorld world = new TestWorld();
			BlockPos right = new BlockPos(15, 64, 15);
			BlockState state = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing)
					.setValue(ChestBlock.TYPE, ChestType.RIGHT);
			BlockPos left = right.relative(ChestBlock.getConnectedDirection(state));
			world.states.put(right, state);
			world.states.put(left, state.setValue(ChestBlock.TYPE, ChestType.LEFT));
			ResolvedContainer fromRight = resolve(world, right);
			assertNotNull(fromRight);
			assertEquals(fromRight, resolve(world, left));
			assertEquals(ContainerType.DOUBLE_CHEST, fromRight.type());
			assertEquals(right, fromRight.members().get(0));
			assertEquals(left, fromRight.members().get(1));
			assertEquals(Vec3.atCenterOf(right).add(Vec3.atCenterOf(left)).scale(0.5), fromRight.center());
			assertEquals(0, world.inventoryLookups, "Client resolution must not consult inventories");
		}
	}

	@Test
	void missingUnloadedOrMismatchedPartnerIsNotTreatedAsSingleChest() {
		TestWorld world = new TestWorld();
		BlockPos right = BlockPos.ZERO;
		BlockState state = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT);
		BlockPos left = right.relative(ChestBlock.getConnectedDirection(state));
		world.states.put(right, state);
		assertNull(resolve(world, right));
		world.unloaded.add(left);
		world.reads.clear();
		assertNull(resolve(world, right));
		assertFalse(world.reads.contains(left));
		world.unloaded.clear();
		world.states.put(left, state);
		assertNull(resolve(world, right));
		world.states.put(left, state.setValue(ChestBlock.TYPE, ChestType.LEFT));
		assertNotNull(resolve(world, right));
		world.states.put(right, state.setValue(ChestBlock.TYPE, ChestType.SINGLE));
		assertEquals(ContainerType.CHEST, resolve(world, right).type());
	}

	@Test
	void onlyExactVanillaChestAndBarrelAreAccepted() {
		TestWorld world = new TestWorld();
		for (var block : new net.minecraft.world.level.block.Block[] {
				Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.SHULKER_BOX, Blocks.HOPPER,
				Blocks.FURNACE, Blocks.DISPENSER, Blocks.DROPPER, Blocks.BREWING_STAND }) {
			world.states.put(BlockPos.ZERO, block.defaultBlockState());
			assertNull(resolve(world, BlockPos.ZERO));
		}
		for (Direction facing : Direction.values()) {
			world.states.put(BlockPos.ZERO, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing));
			ResolvedContainer container = resolve(world, BlockPos.ZERO);
			assertEquals(27, container.type().slots());
			assertEquals(facing, container.facing());
			assertEquals(0.52, container.anchor().distanceTo(container.center()), 0.000001);
		}
	}

	private ResolvedContainer resolve(TestWorld world, BlockPos pos) {
		return VanillaContainerResolver.resolve(Level.OVERWORLD, world, world::loaded, pos);
	}
}
