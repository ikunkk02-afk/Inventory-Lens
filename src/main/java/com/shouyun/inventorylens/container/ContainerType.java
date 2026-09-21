package com.shouyun.inventorylens.container;

public enum ContainerType {
	CHEST(3), DOUBLE_CHEST(6), BARREL(3);

	private final int rows;

	ContainerType(int rows) {
		this.rows = rows;
	}

	public int rows() {
		return rows;
	}

	public int slots() {
		return rows * 9;
	}
}
