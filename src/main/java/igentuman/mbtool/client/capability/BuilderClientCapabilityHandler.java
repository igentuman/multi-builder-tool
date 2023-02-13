package igentuman.mbtool.client.capability;

import igentuman.mbtool.Mbtool;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BuilderClientCapabilityHandler {

	@CapabilityInject(PlayerClientBuilderData.class)
	public static Capability<PlayerClientBuilderData> PLAYER_BUILDER_CLIENT_DATA = null;
	public static final ResourceLocation PLAYER_BUILDER_DATA_CLIENT_NAME = new ResourceLocation(Mbtool.MODID, "player_client_builder_data");

	@SubscribeEvent
	public void attachBuilderDataCapability(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayerSP) {
			event.addCapability(PLAYER_BUILDER_DATA_CLIENT_NAME, new BuilderClientDataProvider((EntityPlayerSP) event.getObject()));
		}
	}

	public static void register()
	{
		CapabilityManager.INSTANCE.register(PlayerClientBuilderData.class, new Capability.IStorage<PlayerClientBuilderData>()
		{
			@Override
			public NBTBase writeNBT(Capability<PlayerClientBuilderData> capability, PlayerClientBuilderData instance, EnumFacing side)
			{
				NBTTagCompound nbt = new NBTTagCompound();
				instance.writeNBT(nbt);
				return nbt;
			}

			@Override
			public void readNBT(Capability<PlayerClientBuilderData> capability, PlayerClientBuilderData instance, EnumFacing side, NBTBase nbt)
			{
				instance.readNBT((NBTTagCompound) nbt);
			}
		}, PlayerClientBuilderData::new);
	}
}
