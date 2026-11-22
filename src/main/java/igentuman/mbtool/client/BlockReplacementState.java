package igentuman.mbtool.client;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.config.MbtoolClientConfig;
import igentuman.mbtool.util.BlockReplacementPreferences;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = Mbtool.MODID, value = Dist.CLIENT)
public final class BlockReplacementState {
    private static List<BlockReplacementPreferences.Group> groups = List.of();

    private BlockReplacementState() {}

    public static List<BlockReplacementPreferences.Group> groups() { return groups; }

    public static BlockReplacementPreferences snapshot() {
        return MbtoolClientConfig.blockReplacements().validated(groups);
    }

    public static void update(List<List<ResourceLocation>> sets) {
        groups = sets.stream().map(ids -> ids.stream().map(ForgeRegistries.BLOCKS::getValue)
                        .filter(Objects::nonNull).filter(block -> block != Blocks.AIR).toList())
                .filter(blocks -> blocks.size() > 1).map(BlockReplacementPreferences.Group::of).toList();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { groups = List.of(); }
}
