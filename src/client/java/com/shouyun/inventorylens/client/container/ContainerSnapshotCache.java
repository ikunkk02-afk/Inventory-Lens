package com.shouyun.inventorylens.client.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ResolvedContainer;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload;
import com.shouyun.inventorylens.network.ContainerSnapshotRequestPayload;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** A bounded, single-target cache keyed by dimension + canonical block position and topology. */
public final class ContainerSnapshotCache {
	public static final long REFRESH_MS = 300;
	public static final long EXPIRY_MS = 1500;
	private final Map<Long, Long> pending = new HashMap<>();
	@Nullable private ResolvedContainer target;
	@Nullable private ContainerSnapshot snapshot;
	private long nextRequestId;
	private long lastAppliedId;
	private long lastRequestMs = Long.MIN_VALUE;
	private long updatedMs;

	public void setTarget(@Nullable ResolvedContainer next) {
		if (!Objects.equals(target, next)) {
			clear();
			target = next;
		}
	}

	@Nullable
	public ContainerSnapshotRequestPayload request(BlockPos hitPosition, long nowMs) {
		expire(nowMs);
		if (target == null || !target.members().contains(hitPosition)
				|| (lastRequestMs != Long.MIN_VALUE && nowMs - lastRequestMs < REFRESH_MS)) {
			return null;
		}
		lastRequestMs = nowMs;
		long requestId = ++nextRequestId;
		pending.put(requestId, nowMs);
		return new ContainerSnapshotRequestPayload(target.identity().dimension(), hitPosition, requestId);
	}

	public void receive(ContainerSnapshotPayload response, long nowMs) {
		expire(nowMs);
		Long sentAt = pending.remove(response.requestId());
		if (sentAt == null || target == null || response.requestId() <= lastAppliedId) {
			return;
		}
		if (response.snapshot() != null && !target.equals(response.snapshot().container())) {
			return;
		}
		lastAppliedId = response.requestId();
		pending.keySet().removeIf(id -> id <= lastAppliedId);
		snapshot = response.snapshot();
		updatedMs = nowMs;
	}

	@Nullable
	public ContainerSnapshot snapshot(long nowMs) {
		expire(nowMs);
		return snapshot;
	}

	public long updatedMs() {
		return updatedMs;
	}

	private void expire(long nowMs) {
		pending.values().removeIf(sentAt -> nowMs - sentAt >= EXPIRY_MS);
		if (snapshot != null && nowMs - updatedMs >= EXPIRY_MS) {
			snapshot = null;
		}
	}

	public void clear() {
		target = null;
		snapshot = null;
		pending.clear();
		lastRequestMs = Long.MIN_VALUE;
		updatedMs = 0;
		// Never reuse request IDs, including on world changes or reconnects.
	}
}
