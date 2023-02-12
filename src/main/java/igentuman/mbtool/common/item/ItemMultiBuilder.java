package igentuman.mbtool.common.item;


import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ItemMultiBuilder extends Item {

    public ItemMultiBuilder() {
        super();
    }

    public CreativeTabs getCreativeTab()
    {
        return CreativeTabs.TOOLS;
    }

    public ItemMultiBuilder setItemName(String name)
    {
        setRegistryName(name);
        setTranslationKey(name);
        return this;
    }
}
