package igentuman.mbtool.common;

import igentuman.mbtool.ISidedProxy;
import igentuman.mbtool.recipe.MultiblockRecipes;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy implements ISidedProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {

    }

    @Override
    public void init(FMLInitializationEvent event)
    {
        MultiblockRecipes.init();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {

    }
}