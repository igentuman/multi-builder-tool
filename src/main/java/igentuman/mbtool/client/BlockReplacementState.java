package igentuman.mbtool.client;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.config.MbtoolClientConfig;
import igentuman.mbtool.util.BlockReplacementPreferences;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Mbtool.MODID, value = Dist.CLIENT)
public final class BlockReplacementState {
    private static List<BlockReplacementPreferences.Group> groups = List.of();
    private static int revision;

    private BlockReplacementState() {}

    public static List<BlockReplacementPreferences.Group> groups() { return groups; }

    public static int revision() { return revision; }

    public static BlockReplacementPreferences snapshot() {
        return MbtoolClientConfig.blockReplacements().validated(groups);
    }

    public static void update(List<List<Identifier>> sets) {
        groups = sets.stream().map(ids -> ids.stream()
                        .map(id -> BuiltInRegistries.BLOCK.get(id).map(Holder::value))
                        .flatMap(Optional::stream)
                        .filter(block -> block != Blocks.AIR).toList())
                .filter(blocks -> blocks.size() > 1).map(BlockReplacementPreferences.Group::of).toList();
        revision++;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        groups = List.of();
        revision++;
    }
}
