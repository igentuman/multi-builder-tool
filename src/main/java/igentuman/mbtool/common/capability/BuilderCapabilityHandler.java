package igentuman.mbtool.common.capability;

import igentuman.mbtool.Mbtool;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class BuilderCapabilityHandler {

	@CapabilityInject(PlayerBuilderData.class)
	public static Capability<PlayerBuilderData> PLAYER_BUILDER_DATA = null;
	public static final ResourceLocation PLAYER_BUILDER_DATA_NAME = new ResourceLocation(Mbtool.MODID, "player_builder_data");



	public static void register()
	{
		CapabilityManager.INSTANCE.register(PlayerBuilderData.class, new Capability.IStorage<PlayerBuilderData>()
		{
			@Override
			public NBTBase writeNBT(Capability<PlayerBuilderData> capability, PlayerBuilderData instance, EnumFacing side)
			{
				NBTTagCompound nbt = new NBTTagCompound();
				instance.writeNBT(nbt);
				return nbt;
			}

			@Override
			public void readNBT(Capability<PlayerBuilderData> capability, PlayerBuilderData instance, EnumFacing side, NBTBase nbt)
			{
				instance.readNBT((NBTTagCompound) nbt);
			}
		}, PlayerBuilderData::new);
	}
}
