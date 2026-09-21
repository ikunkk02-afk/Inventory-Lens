package com.shouyun.inventorylens.container;

import com.shouyun.inventorylens.TestWorld;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhaseThreeResolverTest {
    @BeforeAll static void bootstrap() { TestWorld.bootstrap(); }
    @Test void resolvesEveryAddedBlockWithoutReadingInventory() {
        var expected = Map.ofEntries(Map.entry(Blocks.ENDER_CHEST, ContainerType.ENDER_CHEST),
                Map.entry(Blocks.HOPPER, ContainerType.HOPPER), Map.entry(Blocks.DISPENSER, ContainerType.DISPENSER),
                Map.entry(Blocks.DROPPER, ContainerType.DROPPER), Map.entry(Blocks.FURNACE, ContainerType.FURNACE),
                Map.entry(Blocks.BLAST_FURNACE, ContainerType.BLAST_FURNACE), Map.entry(Blocks.SMOKER, ContainerType.SMOKER),
                Map.entry(Blocks.BREWING_STAND, ContainerType.BREWING_STAND), Map.entry(Blocks.CRAFTER, ContainerType.CRAFTER),
                Map.entry(Blocks.TRAPPED_CHEST, ContainerType.TRAPPED_CHEST));
        var world = new TestWorld();
        expected.forEach((block, type) -> {
            world.states.put(BlockPos.ZERO, block.defaultBlockState());
            assertEquals(type, resolve(world, BlockPos.ZERO).type());
            world.unloaded.add(BlockPos.ZERO);
            world.reads.clear();
            assertNull(resolve(world, BlockPos.ZERO));
            assertTrue(world.reads.isEmpty());
            world.unloaded.clear();
        });
        assertEquals(0, world.inventoryLookups);
    }
    @Test void allSeventeenVanillaShulkerVariantsResolve() {
        var world = new TestWorld();
        int count = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof ShulkerBoxBlock && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft")) {
                world.states.put(BlockPos.ZERO, block.defaultBlockState());
                assertEquals(ContainerType.SHULKER_BOX, resolve(world, BlockPos.ZERO).type());
                assertEquals(27, resolve(world, BlockPos.ZERO).type().slots());
                count++;
            }
        }
        assertEquals(17, count);
    }
    @Test void trappedDoubleUsesVanillaOrderAndCannotPairWithOrdinaryChest() {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var world = new TestWorld();
            var state = Blocks.TRAPPED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, direction).setValue(ChestBlock.TYPE, ChestType.RIGHT);
            var partner = BlockPos.ZERO.relative(ChestBlock.getConnectedDirection(state));
            world.states.put(BlockPos.ZERO, state);
            world.states.put(partner, state.setValue(ChestBlock.TYPE, ChestType.LEFT));
            var target = resolve(world, partner);
            assertEquals(ContainerType.DOUBLE_TRAPPED_CHEST, target.type());
            assertEquals(BlockPos.ZERO, target.members().getFirst());
            assertEquals(target, resolve(world, BlockPos.ZERO));
            world.unloaded.add(partner);
            assertNull(resolve(world, BlockPos.ZERO));
            world.unloaded.clear();
            world.states.put(partner, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, direction).setValue(ChestBlock.TYPE, ChestType.LEFT));
            assertNull(resolve(world, BlockPos.ZERO));
        }
    }
    @Test void hopperAndCrafterUseTheirActualVanillaOrientationProperties() {
        var world = new TestWorld();
        for (Direction direction : HopperBlock.FACING.getPossibleValues()) {
            world.states.put(BlockPos.ZERO, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, direction));
            assertEquals(direction, resolve(world, BlockPos.ZERO).facing());
        }
        for (var orientation : net.minecraft.core.FrontAndTop.values()) {
            world.states.put(BlockPos.ZERO, Blocks.CRAFTER.defaultBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.ORIENTATION, orientation));
            assertEquals(orientation.front(), resolve(world, BlockPos.ZERO).facing());
        }
    }
    private static ResolvedContainer resolve(TestWorld world, BlockPos pos) {
        return ContainerResolverRegistry.resolve(Level.OVERWORLD, world, world::loaded, pos);
    }
}
