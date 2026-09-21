package com.shouyun.inventorylens.client.container;

import java.util.Collections;
import java.util.List;

import com.shouyun.inventorylens.TestWorld;
import com.shouyun.inventorylens.container.ContainerIdentity;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerType;
import com.shouyun.inventorylens.container.ResolvedContainer;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload.Status;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerSnapshotCacheTest {
	@BeforeAll
	static void initialize() {
		TestWorld.bootstrap();
	}

	@Test
	void firstRequestIsImmediateThenThrottledEvenAcrossDoubleChestHalves() {
		ContainerSnapshotCache cache = new ContainerSnapshotCache();
		ResolvedContainer target = doubleChest();
		cache.setTarget(target);
		var first = cache.request(BlockPos.ZERO, 0);
		assertNotNull(first);
		cache.receive(response(first.requestId(), target, 32), 10);
		cache.setTarget(doubleChest());
		assertNotNull(cache.snapshot(20));
		assertNull(cache.request(BlockPos.ZERO.east(), 299));
		assertNotNull(cache.request(BlockPos.ZERO.east(), 300));
	}

	@Test
	void olderReplyCannotOverwriteNewerDataAndRefreshKeepsVisibleSnapshot() {
		ContainerSnapshotCache cache = new ContainerSnapshotCache();
		ResolvedContainer target = doubleChest();
		cache.setTarget(target);
		var first = cache.request(BlockPos.ZERO, 0);
		var second = cache.request(BlockPos.ZERO, 300);
		cache.receive(response(second.requestId(), target, 64), 310);
		cache.receive(response(first.requestId(), target, 1), 320);
		assertEquals(64, cache.snapshot(320).items().get(0).getCount());
		assertNotNull(cache.request(BlockPos.ZERO, 600));
		assertEquals(64, cache.snapshot(600).items().get(0).getCount());
	}

	@Test
	void changingWorldOrLookingAwayRejectsOldResponsesEvenAfterReturningToSamePosition() {
		ContainerSnapshotCache cache = new ContainerSnapshotCache();
		ResolvedContainer target = doubleChest();
		cache.setTarget(target);
		var old = cache.request(BlockPos.ZERO, 0);
		cache.clear();
		cache.setTarget(target);
		var fresh = cache.request(BlockPos.ZERO, 10);
		assertTrue(fresh.requestId() > old.requestId());
		cache.receive(response(old.requestId(), target, 64), 20);
		assertNull(cache.snapshot(20));
		cache.receive(response(fresh.requestId(), target, 1), 30);
		assertNotNull(cache.snapshot(30));
		cache.setTarget(null);
		assertNull(cache.snapshot(31));
	}

	@Test
	void expiryDenialAndTopologyChangesClearInventoryWithoutInventingEmptySlots() {
		ContainerSnapshotCache cache = new ContainerSnapshotCache();
		ResolvedContainer target = doubleChest();
		cache.setTarget(target);
		var first = cache.request(BlockPos.ZERO, 0);
		cache.receive(response(first.requestId(), target, 64), 10);
		assertNotNull(cache.snapshot(1509));
		assertNull(cache.snapshot(1510));
		var next = cache.request(BlockPos.ZERO, 1510);
		cache.receive(response(next.requestId(), target, 32), 1520);
		var denied = cache.request(BlockPos.ZERO, 1810);
		cache.receive(new ContainerSnapshotPayload(denied.requestId(), Status.UNAVAILABLE, null), 1820);
		assertNull(cache.snapshot(1820));
		var old = cache.request(BlockPos.ZERO, 2110);
		cache.setTarget(new ResolvedContainer(target.identity(), ContainerType.CHEST, List.of(BlockPos.ZERO), Direction.NORTH));
		cache.receive(response(old.requestId(), target, 64), 2120);
		assertNull(cache.snapshot(2120));
	}

	@Test
	void responseArrivingAfterRequestExpiryIsDiscarded() {
		ContainerSnapshotCache cache = new ContainerSnapshotCache();
		ResolvedContainer target = doubleChest();
		cache.setTarget(target);
		var first = cache.request(BlockPos.ZERO, 0);
		cache.receive(response(first.requestId(), target, 64), 1500);
		assertNull(cache.snapshot(1500));
	}

    @Test void productionStateAndItemsUpdateTogetherAndOldSessionCannotRestoreEnderItems() {
        var cache = new ContainerSnapshotCache();
        var target = new ResolvedContainer(new ContainerIdentity(Level.OVERWORLD, BlockPos.ZERO), ContainerType.FURNACE, List.of(BlockPos.ZERO), Direction.NORTH);
        cache.setTarget(target);
        var first = cache.request(BlockPos.ZERO, 0);
        var second = cache.request(BlockPos.ZERO, 300);
        var state = new com.shouyun.inventorylens.container.ContainerProperties.Furnace(100, 200, 50, 200);
        var snapshot = new ContainerSnapshot(target, List.of(new ItemStack(Items.RAW_IRON, 3), new ItemStack(Items.COAL), ItemStack.EMPTY),
                target.type().gui(), net.minecraft.network.chat.Component.translatable("container.furnace"), state);
        cache.receive(new ContainerSnapshotPayload(second.requestId(), Status.OK, snapshot), 310);
        cache.receive(new ContainerSnapshotPayload(first.requestId(), Status.OK, new ContainerSnapshot(target, Collections.nCopies(3, ItemStack.EMPTY))), 320);
        assertEquals(state, cache.snapshot(320).properties());
        assertEquals(3, cache.snapshot(320).items().getFirst().getCount());
        assertNull(cache.snapshot(1810));
        var ender = new ResolvedContainer(target.identity(), ContainerType.ENDER_CHEST, List.of(BlockPos.ZERO), Direction.NORTH);
        cache.setTarget(ender);
        var old = cache.request(BlockPos.ZERO, 2000);
        cache.clear();
        cache.setTarget(ender);
        cache.receive(response(old.requestId(), ender, 64), 2010);
        assertNull(cache.snapshot(2010));
    }

	private ResolvedContainer doubleChest() {
		return new ResolvedContainer(new ContainerIdentity(Level.OVERWORLD, BlockPos.ZERO), ContainerType.DOUBLE_CHEST,
				List.of(BlockPos.ZERO, BlockPos.ZERO.east()), Direction.NORTH);
	}

	private ContainerSnapshotPayload response(long id, ResolvedContainer target, int count) {
		return new ContainerSnapshotPayload(id, Status.OK,
				new ContainerSnapshot(target, Collections.nCopies(target.type().slots(), new ItemStack(Items.APPLE, count))));
	}
}
