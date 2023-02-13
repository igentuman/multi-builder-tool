package igentuman.mbtool.common.capability;


import net.minecraft.nbt.NBTTagCompound;

public interface IBuilderData {

	NBTTagCompound writeNBT(NBTTagCompound nbt);

	void readNBT(NBTTagCompound nbt);

}
