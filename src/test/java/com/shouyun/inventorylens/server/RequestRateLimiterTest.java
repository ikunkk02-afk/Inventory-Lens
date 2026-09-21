package com.shouyun.inventorylens.server;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestRateLimiterTest {
	@Test
	void limitsFloodIndependentlyPerPlayerAndReleasesStateOnDisconnect() {
		RequestRateLimiter limiter = new RequestRateLimiter();
		UUID player = UUID.randomUUID();
		assertTrue(limiter.allow(player, 0));
		assertTrue(limiter.allow(player, 0));
		for (int i = 0; i < 180; i++) {
			assertFalse(limiter.allow(player, 0));
		}
		assertTrue(limiter.allow(UUID.randomUUID(), 0));
		assertFalse(limiter.allow(player, 99));
		assertTrue(limiter.allow(player, 100));
		assertFalse(limiter.allow(player, 100));
		limiter.remove(player);
		assertTrue(limiter.allow(player, 100));
		limiter.clear();
		assertTrue(limiter.allow(player, 100));
	}
}
