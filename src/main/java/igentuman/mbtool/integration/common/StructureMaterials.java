package igentuman.mbtool.integration.common;

import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StructureMaterials {
    private StructureMaterials() {}

    public static List<ItemStack> of(MultiblockStructure structure) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (BlockState state : structure.getBlocks().values()) {
            if (state.is(Blocks.AIR)) continue;
            Item item = state.getBlock().asItem();
            if (item == Items.AIR) continue; // blocks without an item form (fire, fluids, ...)
            counts.merge(item, 1, Integer::sum);
        }
        List<ItemStack> result = new ArrayList<>(counts.size());
        counts.forEach((item, count) -> result.add(new ItemStack(item, count)));
        return result;
    }
}
