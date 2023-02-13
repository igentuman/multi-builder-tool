package igentuman.mbtool.common.capability;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class BuilderDataProvider implements ICapabilitySerializable<NBTTagCompound> {

	private final PlayerBuilderData builderData;
	private EntityPlayerMP owner;

	public BuilderDataProvider(EntityPlayerMP owner) {
		this.owner = owner;
		builderData = BuilderCapabilityHandler.PLAYER_BUILDER_DATA.getDefaultInstance();
		builderData.setPlayer(new WeakReference<>(this.owner));

	}
	
	@Override
	public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == BuilderCapabilityHandler.PLAYER_BUILDER_DATA;
	}
	
	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == BuilderCapabilityHandler.PLAYER_BUILDER_DATA) {
			return BuilderCapabilityHandler.PLAYER_BUILDER_DATA.cast(builderData);
		}
		return null;
	}
	
	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		builderData.writeNBT(nbt);
		return nbt;
	}
	
	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		builderData.readNBT(nbt);
	}
}
