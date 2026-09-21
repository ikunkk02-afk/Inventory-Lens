package com.shouyun.inventorylens.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.Lifecycle;
import com.shouyun.inventorylens.TestWorld;
import com.shouyun.inventorylens.container.ContainerIdentity;
import com.shouyun.inventorylens.container.ContainerSnapshot;
import com.shouyun.inventorylens.container.ContainerType;
import com.shouyun.inventorylens.container.ContainerProperties;
import com.shouyun.inventorylens.container.ResolvedContainer;
import com.shouyun.inventorylens.network.ContainerSnapshotPayload.Status;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerSnapshotCodecTest {
	private static RegistryAccess registries;

	@BeforeAll
	static void initialize() {
		TestWorld.bootstrap();
		var vanilla = VanillaRegistries.createLookup();
		MappedRegistry<Enchantment> enchantments = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());
		vanilla.lookupOrThrow(Registries.ENCHANTMENT).listElements()
				.forEach(holder -> Registry.register(enchantments, holder.key(), holder.value()));
		enchantments.freeze();
		registries = new RegistryAccess.ImmutableRegistryAccess(Stream.concat(
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registries(),
				Stream.of(new RegistryAccess.RegistryEntry<>(Registries.ENCHANTMENT, enchantments))));
	}

	@Test
	void roundTripsComponentsCountsAndEmptySlotsForAllSupportedContainers() {
		for (ContainerType type : ContainerType.values()) {
			List<ItemStack> items = new ArrayList<>(Collections.nCopies(type.slots(), ItemStack.EMPTY));
			ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
			sword.setDamageValue(400);
			sword.set(DataComponents.CUSTOM_NAME, Component.literal("Projection test sword"));
			sword.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(123));
			sword.enchant(registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS), 3);
			items.set(0, sword);
			items.set(1, new ItemStack(Items.COBBLESTONE, 64));
			items.set(2, PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING));
			ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
			book.enchant(registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING), 3);
			items.set(type.slots() - 1, book);
			List<BlockPos> members = type.memberCount() == 2
					? List.of(BlockPos.ZERO, BlockPos.ZERO.east()) : List.of(BlockPos.ZERO);
			ResolvedContainer target = new ResolvedContainer(new ContainerIdentity(Level.OVERWORLD, BlockPos.ZERO), type, members, Direction.NORTH);
			ContainerSnapshotPayload original = new ContainerSnapshotPayload(123, Status.OK, new ContainerSnapshot(target, items, type.gui(),
                    Component.literal("钻石仓库"), sample(type)));
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
			try {
				ContainerSnapshotPayload.STREAM_CODEC.encode(buffer, original);
				ContainerSnapshotPayload decoded = ContainerSnapshotPayload.STREAM_CODEC.decode(buffer);
				assertEquals(123, decoded.requestId());
				assertEquals(target, decoded.snapshot().container());
                assertEquals(original.snapshot().title(), decoded.snapshot().title());
                assertEquals(type.gui(), decoded.snapshot().gui());
                assertEquals(sample(type), decoded.snapshot().properties());
				assertEquals(0, buffer.readableBytes());
				for (int i = 0; i < items.size(); i++) {
					assertTrue(ItemStack.matches(items.get(i), decoded.snapshot().items().get(i)), "Slot " + i);
				}
			} finally {
				buffer.release();
			}
		}
	}

	@Test
	void requestAndUnavailableResponsesRoundTrip() {
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try {
			var request = new ContainerSnapshotRequestPayload(Level.NETHER, new BlockPos(-20, 65, 30), 42);
			ContainerSnapshotRequestPayload.STREAM_CODEC.encode(buffer, request);
			assertEquals(request, ContainerSnapshotRequestPayload.STREAM_CODEC.decode(buffer));
			for (Status status : List.of(Status.UNAVAILABLE, Status.UNGENERATED_LOOT)) {
				var denied = new ContainerSnapshotPayload(42, status, null);
				ContainerSnapshotPayload.STREAM_CODEC.encode(buffer, denied);
				assertEquals(denied, ContainerSnapshotPayload.STREAM_CODEC.decode(buffer));
			}
		} finally {
			buffer.release();
		}
	}

	@Test
	void malformedSlotCountIsRejectedBeforeAllocatingOrReadingStacks() {
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try {
			buffer.writeVarLong(1);
			buffer.writeVarInt(Status.OK.ordinal());
			buffer.writeResourceLocation(Level.OVERWORLD.location());
			buffer.writeBlockPos(BlockPos.ZERO);
			buffer.writeResourceLocation(ContainerType.CHEST.id());
			buffer.writeVarInt(Direction.NORTH.ordinal());
			buffer.writeBlockPos(BlockPos.ZERO);
			buffer.writeVarInt(Integer.MAX_VALUE);
			assertThrows(DecoderException.class, () -> ContainerSnapshotPayload.STREAM_CODEC.decode(buffer));
		} finally {
			buffer.release();
		}
	}
    private static ContainerProperties sample(ContainerType type) {
        return switch (type.propertiesKind()) {
            case NONE -> new ContainerProperties.None();
            case FURNACE -> new ContainerProperties.Furnace(777, 1600, 50, 200);
            case BREWING -> new ContainerProperties.Brewing(213, 17);
            case CRAFTER -> new ContainerProperties.Crafter(511, true);
        };
    }
    @Test void malformedPropertiesAndDisabledBitsAreRejected() {
        for (var type : List.of(ContainerType.CHEST, ContainerType.CRAFTER)) {
            var target = new ResolvedContainer(new ContainerIdentity(Level.OVERWORLD, BlockPos.ZERO), type, List.of(BlockPos.ZERO), Direction.NORTH);
            var snapshot = new ContainerSnapshot(target, Collections.nCopies(type.slots(), ItemStack.EMPTY), type.gui(), Component.empty(), sample(type));
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                ContainerSnapshotPayload.STREAM_CODEC.encode(buffer, new ContainerSnapshotPayload(1, Status.OK, snapshot));
                if (type == ContainerType.CHEST) buffer.setByte(buffer.writerIndex() - 1, ContainerProperties.Kind.FURNACE.ordinal());
                else buffer.setByte(buffer.writerIndex() - 2, 4); // 511's FF 03 becomes FF 04: outside nine bits.
                assertThrows(DecoderException.class, () -> ContainerSnapshotPayload.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }
    @Test void unknownTypeIsRejectedBeforeReadingItems() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            buffer.writeVarLong(1); buffer.writeVarInt(Status.OK.ordinal());
            buffer.writeResourceLocation(Level.OVERWORLD.location()); buffer.writeBlockPos(BlockPos.ZERO);
            buffer.writeResourceLocation(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("example", "unknown"));
            assertThrows(DecoderException.class, () -> ContainerSnapshotPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

}
