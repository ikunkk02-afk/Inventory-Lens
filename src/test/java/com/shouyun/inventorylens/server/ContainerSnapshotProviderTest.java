package com.shouyun.inventorylens.server;

import com.shouyun.inventorylens.TestWorld;

import com.shouyun.inventorylens.container.ContainerType;

import com.shouyun.inventorylens.container.ResolvedContainer;

import com.shouyun.inventorylens.container.VanillaContainerResolver;

import com.shouyun.inventorylens.network.ContainerSnapshotPayload.Status;

import net.minecraft.core.BlockPos;

import net.minecraft.core.registries.Registries;

import net.minecraft.network.chat.Component;

import net.minecraft.core.component.DataComponents;

import net.minecraft.resources.ResourceKey;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.LockCode;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.Items;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.ChestBlock;

import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.block.state.properties.ChestType;

import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerSnapshotProviderTest {

	@BeforeAll

	static void initialize() {

		TestWorld.bootstrap();

	}

	@Test

	void copiesRealInventoryInVanillaDoubleChestOrderAndPreservesEmptySlots() {

		TestWorld world = new TestWorld();

		BlockState rightState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT);

		ChestBlockEntity right = new ChestBlockEntity(BlockPos.ZERO, rightState);

		ChestBlockEntity left = new ChestBlockEntity(BlockPos.ZERO.relative(ChestBlock.getConnectedDirection(rightState)),

				rightState.setValue(ChestBlock.TYPE, ChestType.LEFT));

		right.setItem(0, new ItemStack(Items.DIAMOND, 1));

		left.setItem(0, new ItemStack(Items.APPLE, 32));

		left.setItem(26, new ItemStack(Items.COBBLESTONE, 64));

		world.put(right);

		world.put(left);

		ResolvedContainer target = resolve(world, left.getBlockPos());

		var response = new ContainerSnapshotProvider(container -> LockCode.NO_LOCK)

				.read(1, target, world, ItemStack.EMPTY, false);

		assertEquals(Status.OK, response.status());

		assertEquals(54, response.snapshot().items().size());

		assertTrue(response.snapshot().items().get(0).is(Items.DIAMOND));

		assertTrue(response.snapshot().items().get(1).isEmpty());

		assertEquals(32, response.snapshot().items().get(27).getCount());

		assertEquals(64, response.snapshot().items().get(53).getCount());

		left.getItem(0).setCount(2);

		assertEquals(32, response.snapshot().items().get(27).getCount(), "Snapshot must be detached from server stacks");

	}

	@Test

	void emptyBarrelStillProvidesAllTwentySevenSlots() {

		TestWorld world = new TestWorld();

		world.put(new BarrelBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState()));

		var response = new ContainerSnapshotProvider(container -> LockCode.NO_LOCK)

				.read(1, resolve(world, BlockPos.ZERO), world, ItemStack.EMPTY, false);

		assertEquals(ContainerType.BARREL, response.snapshot().container().type());

		assertEquals(27, response.snapshot().items().size());

		assertTrue(response.snapshot().items().stream().allMatch(ItemStack::isEmpty));

	}

	@Test

	void ungeneratedLootInEitherHalfPreventsAllInventoryReads() {

		TestWorld world = new TestWorld();

		BlockState rightState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT);

		CountingChest right = new CountingChest(BlockPos.ZERO, rightState);

		CountingChest left = new CountingChest(BlockPos.ZERO.relative(ChestBlock.getConnectedDirection(rightState)),

				rightState.setValue(ChestBlock.TYPE, ChestType.LEFT));

		left.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/simple_dungeon")));

		world.put(right);

		world.put(left);

		var response = new ContainerSnapshotProvider(container -> LockCode.NO_LOCK)

				.read(1, resolve(world, BlockPos.ZERO), world, ItemStack.EMPTY, false);

		assertEquals(Status.UNGENERATED_LOOT, response.status());

		assertNull(response.snapshot());

		assertNotNull(left.getLootTable());

		assertEquals(0, right.itemReads + left.itemReads);

	}

	@Test

	void lockedContainersRequireVanillaKeyBeforeReadingAnySlot() {

		TestWorld world = new TestWorld();

		CountingChest chest = new CountingChest(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());

		world.put(chest);

		var provider = new ContainerSnapshotProvider(container -> new LockCode("test key"));

		assertEquals(Status.UNAVAILABLE, provider.read(1, resolve(world, BlockPos.ZERO), world, ItemStack.EMPTY, false).status());

		assertEquals(0, chest.itemReads);

		ItemStack key = new ItemStack(Items.STICK);

		key.set(DataComponents.CUSTOM_NAME, Component.literal("test key"));

		assertEquals(Status.OK, provider.read(2, resolve(world, BlockPos.ZERO), world, key, false).status());

		assertEquals(27, chest.itemReads);

	}

    @Test

    void enderSnapshotsUseOnlyTheRequestLocalPlayerInventory() {

        TestWorld world = new TestWorld();

        world.put(new net.minecraft.world.level.block.entity.EnderChestBlockEntity(BlockPos.ZERO, Blocks.ENDER_CHEST.defaultBlockState()));

        var a = new net.minecraft.world.inventory.PlayerEnderChestContainer();

        var b = new net.minecraft.world.inventory.PlayerEnderChestContainer();

        a.setItem(0, new ItemStack(Items.DIAMOND, 3));

        b.setItem(0, new ItemStack(Items.APPLE, 12));

        var target = resolve(world, BlockPos.ZERO);

        var provider = new ContainerSnapshotProvider(container -> LockCode.NO_LOCK);

        var snapshotA = provider.read(1, target, world, ItemStack.EMPTY, false, a).snapshot();

        var snapshotB = provider.read(2, target, world, ItemStack.EMPTY, false, b).snapshot();

        assertTrue(snapshotA.items().get(0).is(Items.DIAMOND));

        assertTrue(snapshotB.items().get(0).is(Items.APPLE));

        assertEquals(3, snapshotA.items().get(0).getCount());

        assertEquals(12, snapshotB.items().get(0).getCount());

        assertEquals(Status.UNAVAILABLE, provider.read(3, target, world, ItemStack.EMPTY, false).status());

        assertFalse(a.isActiveChest((net.minecraft.world.level.block.entity.EnderChestBlockEntity)world.getBlockEntity(BlockPos.ZERO)));

        a.setItem(0, ItemStack.EMPTY);

        assertEquals(3, snapshotA.items().get(0).getCount());

    }

    @Test

    void eachPersistentContainerCopiesItsActualSlotOrderAndCustomName() {

        for (var block : java.util.List.of(Blocks.SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.HOPPER,

                Blocks.DISPENSER, Blocks.DROPPER, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,

                Blocks.BREWING_STAND, Blocks.CRAFTER, Blocks.TRAPPED_CHEST)) {

            var world = new TestWorld();

            var entity = ((net.minecraft.world.level.block.EntityBlock)block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());

            var inventory = (net.minecraft.world.Container)entity;

            var registries = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
            var contents = net.minecraft.core.NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
            for (int slot = 0; slot < contents.size(); slot++) contents.set(slot, new ItemStack(Items.COBBLESTONE, slot + 1));
            var saved = new net.minecraft.nbt.CompoundTag();
            net.minecraft.world.ContainerHelper.saveAllItems(saved, contents, registries);
            entity.loadWithComponents(saved, registries);

            name(entity, "钻石仓库");

            world.put(entity);

            var target = resolve(world, BlockPos.ZERO);

            var state = com.shouyun.inventorylens.container.ContainerProperties.empty(target.type().propertiesKind());

            var result = new ContainerSnapshotProvider(c -> LockCode.NO_LOCK, e -> state)

                    .read(1, target, world, ItemStack.EMPTY, false);

            assertEquals(Status.OK, result.status(), block.toString());

            assertEquals("钻石仓库", result.snapshot().title().getString());

            assertEquals(inventory.getContainerSize(), result.snapshot().items().size());

            for (int slot = 0; slot < inventory.getContainerSize(); slot++) assertEquals(slot + 1, result.snapshot().items().get(slot).getCount());

            assertEquals(state, result.snapshot().properties());

        }

    }

    @Test

    void newLootContainersDoNotGenerateLootAndRespectLocks() {

        for (var block : java.util.List.of(Blocks.SHULKER_BOX, Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER, Blocks.CRAFTER)) {

            var world = new TestWorld();

            var entity = ((net.minecraft.world.level.block.EntityBlock)block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());

            world.put(entity);

            var target = resolve(world, BlockPos.ZERO);

            var provider = new ContainerSnapshotProvider(c -> new LockCode("key"));

            assertEquals(Status.UNAVAILABLE, provider.read(1, target, world, ItemStack.EMPTY, false).status());

            if (entity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity random) {

                random.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/simple_dungeon")));

                assertEquals(Status.UNGENERATED_LOOT, new ContainerSnapshotProvider(c -> LockCode.NO_LOCK)

                        .read(2, target, world, ItemStack.EMPTY, false).status());

                assertNotNull(random.getLootTable());

            }

        }

    }

    @Test

    void doubleChestCustomNameUsesFirstThenSecondVanillaMember() {

        var world = new TestWorld();

        var state = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT);

        var first = new ChestBlockEntity(BlockPos.ZERO, state);

        var second = new ChestBlockEntity(BlockPos.ZERO.relative(ChestBlock.getConnectedDirection(state)), state.setValue(ChestBlock.TYPE, ChestType.LEFT));

        world.put(first); world.put(second);

        name(second, "Second");

        var provider = new ContainerSnapshotProvider(c -> LockCode.NO_LOCK);

        assertEquals("Second", provider.read(1, resolve(world, first.getBlockPos()), world, ItemStack.EMPTY, false).snapshot().title().getString());

        name(first, "First");

        assertEquals("First", provider.read(2, resolve(world, second.getBlockPos()), world, ItemStack.EMPTY, false).snapshot().title().getString());

    }

    private static void name(net.minecraft.world.level.block.entity.BlockEntity entity, String name) {
        var registries = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        var tag = entity.saveWithoutMetadata(registries);
        tag.putString("CustomName", Component.Serializer.toJson(Component.literal(name), registries));
        entity.loadWithComponents(tag, registries);
    }

	private ResolvedContainer resolve(TestWorld world, BlockPos position) {

		return VanillaContainerResolver.resolve(Level.OVERWORLD, world, world::loaded, position);

	}

	private static class CountingChest extends ChestBlockEntity {

		private int itemReads;

		CountingChest(BlockPos position, BlockState state) {

			super(position, state);

		}

		@Override

		public ItemStack getItem(int slot) {

			itemReads++;

			return super.getItem(slot);

		}

	}

}
