package com.shouyun.inventorylens.container;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Register integrations during initialization on both sides, before requests are processed. */
public final class ContainerResolverRegistry {
    private static final List<ContainerResolver> RESOLVERS = new ArrayList<>(List.of(new VanillaContainerResolver()));
    private ContainerResolverRegistry() { }
    public static void register(ContainerResolver resolver) { RESOLVERS.add(java.util.Objects.requireNonNull(resolver)); }
    @Nullable public static ResolvedContainer resolve(Level level, BlockPos pos) {
        return resolve(level.dimension(), level, level::hasChunkAt, pos);
    }
    @Nullable public static ResolvedContainer resolve(ResourceKey<Level> dimension, BlockGetter blocks,
            Predicate<BlockPos> loaded, BlockPos pos) {
        if (!loaded.test(pos)) return null;
        for (ContainerResolver resolver : RESOLVERS) {
            ResolvedContainer result = resolver.find(dimension, blocks, loaded, pos);
            if (result != null) return result;
        }
        return null;
    }
    @Nullable public static ContainerSnapshot read(ResolvedContainer target, ContainerReadContext context) {
        for (ContainerResolver resolver : RESOLVERS) {
            if (resolver.supports(target.type())) return resolver.read(target, context);
        }
        return null;
    }
}
