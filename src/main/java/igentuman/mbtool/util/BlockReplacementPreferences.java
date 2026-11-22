package igentuman.mbtool.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** A request snapshot. Only the server's configured groups can authorize a replacement. */
public record BlockReplacementPreferences(Map<String, ResourceLocation> selections) {
    public static final int MAX_SETS = 1024;
    public static final int MAX_MEMBERS = 256;
    public static final BlockReplacementPreferences EMPTY = new BlockReplacementPreferences(Map.of());

    public BlockReplacementPreferences {
        selections = Map.copyOf(selections);
    }

    public record Group(String id, List<Block> blocks) {
        public Group {
            blocks = List.copyOf(blocks);
        }

        public static Group of(Collection<Block> blocks) {
            List<Block> distinct = new ArrayList<>(new LinkedHashSet<>(blocks));
            String key = distinct.stream().map(ForgeRegistries.BLOCKS::getKey)
                    .map(Objects::requireNonNull).map(ResourceLocation::toString).sorted()
                    .reduce((a, b) -> a + "," + b).orElse("");
            return new Group(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString(), distinct);
        }
    }

    public BlockReplacementPreferences validated(List<Group> groups) {
        Map<String, ResourceLocation> valid = new LinkedHashMap<>();
        for (Group group : groups) {
            ResourceLocation selected = selections.get(group.id());
            if (selected == null) continue;
            for (Block block : group.blocks()) {
                if (block.asItem() != Items.AIR && selected.equals(ForgeRegistries.BLOCKS.getKey(block))) {
                    valid.put(group.id(), selected);
                    break;
                }
            }
        }
        return new BlockReplacementPreferences(valid);
    }

    /** Overlapping groups use the first configured group with a valid selection. */
    public Block preferred(Block original, List<Group> groups) {
        for (Group group : groups) {
            ResourceLocation selected = selections.get(group.id());
            if (selected == null || !group.blocks().contains(original)) continue;
            for (Block block : group.blocks()) {
                if (block.asItem() != Items.AIR && selected.equals(ForgeRegistries.BLOCKS.getKey(block))) return block;
            }
        }
        return null;
    }

    public void write(FriendlyByteBuf buffer) {
        if (selections.size() > MAX_SETS) throw new IllegalArgumentException("Too many block preferences");
        buffer.writeVarInt(selections.size());
        selections.forEach((group, block) -> {
            buffer.writeUtf(group, 36);
            buffer.writeUtf(block.toString(), 256);
        });
    }

    public static BlockReplacementPreferences read(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_SETS) throw new IllegalArgumentException("Too many block preferences");
        Map<String, ResourceLocation> selections = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String group = buffer.readUtf(36);
            ResourceLocation block = ResourceLocation.tryParse(buffer.readUtf(256));
            if (block != null) selections.put(group, block);
        }
        return new BlockReplacementPreferences(selections);
    }
}
