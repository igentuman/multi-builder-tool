package igentuman.mbtool.util;

import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ExtraTileDataProviderRegistry {
    private static List<AbstractExtraTileDataProvider> extraTileData = new ArrayList();

    public ExtraTileDataProviderRegistry() {
    }

    public static void registerExtraTileDataProviders() {
        Iterator var0 = AnnotatedInstanceUtil.getExtraTileDataProviders().iterator();

        while(var0.hasNext()) {
            AbstractExtraTileDataProvider etdp = (AbstractExtraTileDataProvider)var0.next();
            if (etdp.getName() != null) {
                extraTileData.add(etdp);
            }
        }

    }

    public static boolean hasDataProvider(TileEntity tileEntity) {
        return extraTileData.stream().anyMatch((provider) -> {
            return provider.worksWith(tileEntity);
        });
    }

    public static List<AbstractExtraTileDataProvider> getDataProviders(TileEntity tileEntity) {
        return (List)extraTileData.stream().filter((provider) -> {
            return provider.worksWith(tileEntity);
        }).collect(Collectors.toList());
    }
}