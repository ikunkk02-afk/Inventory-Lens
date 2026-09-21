package com.shouyun.inventorylens.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread token buckets: ten requests/second, with room for two immediate target changes. */
public final class RequestRateLimiter {
	private final Map<UUID, Bucket> buckets = new HashMap<>();

	public boolean allow(UUID player, long nowMs) {
		Bucket bucket = buckets.computeIfAbsent(player, ignored -> new Bucket(nowMs));
		bucket.tokens = Math.min(2.0, bucket.tokens + Math.max(0, nowMs - bucket.lastMs) / 100.0);
		bucket.lastMs = nowMs;
		if (bucket.tokens < 1.0) {
			return false;
		}
		bucket.tokens--;
		return true;
	}

	public void remove(UUID player) {
		buckets.remove(player);
	}

	public void clear() {
		buckets.clear();
	}

	private static final class Bucket {
		private double tokens = 2;
		private long lastMs;

		private Bucket(long nowMs) {
			lastMs = nowMs;
		}
	}
}
