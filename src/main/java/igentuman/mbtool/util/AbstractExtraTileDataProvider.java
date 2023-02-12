package igentuman.mbtool.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public abstract class AbstractExtraTileDataProvider {
    public AbstractExtraTileDataProvider() {
    }

    public abstract boolean worksWith(TileEntity var1);

    public abstract NBTTagCompound writeExtraData(TileEntity var1);

    public abstract void readExtraData(TileEntity var1, NBTTagCompound var2);

    public abstract String getName();
}