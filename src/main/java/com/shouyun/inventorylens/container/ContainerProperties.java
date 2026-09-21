package com.shouyun.inventorylens.container;

/** Only detached UI data; no NBT, menu state or player inventory. */
public sealed interface ContainerProperties {
    enum Kind { NONE, FURNACE, BREWING, CRAFTER }
    Kind kind();
    record None() implements ContainerProperties { public Kind kind() { return Kind.NONE; } }
    record Furnace(int litTime, int litDuration, int cookingProgress, int cookingTotalTime) implements ContainerProperties {
        public Kind kind() { return Kind.FURNACE; }
    }
    record Brewing(int brewTime, int fuel) implements ContainerProperties { public Kind kind() { return Kind.BREWING; } }
    record Crafter(int disabledSlots, boolean powered) implements ContainerProperties {
        public Crafter { if ((disabledSlots & ~511) != 0) throw new IllegalArgumentException("Invalid disabled slots"); }
        public Kind kind() { return Kind.CRAFTER; }
    }
    static ContainerProperties empty(Kind kind) {
        return switch (kind) {
            case NONE -> new None();
            case FURNACE -> new Furnace(0, 0, 0, 0);
            case BREWING -> new Brewing(0, 0);
            case CRAFTER -> new Crafter(0, false);
        };
    }
}
