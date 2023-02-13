package igentuman.mbtool.client.capability;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BuilderClientDataProvider implements ICapabilityProvider {

	private final PlayerClientBuilderData builderData;
	private EntityPlayerSP owner;

	public BuilderClientDataProvider(EntityPlayerSP owner) {
		this.owner = owner;
		builderData = BuilderClientCapabilityHandler.PLAYER_BUILDER_CLIENT_DATA.getDefaultInstance();

	}
	
	@Override
	public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == BuilderClientCapabilityHandler.PLAYER_BUILDER_CLIENT_DATA;
	}
	
	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == BuilderClientCapabilityHandler.PLAYER_BUILDER_CLIENT_DATA) {
			return BuilderClientCapabilityHandler.PLAYER_BUILDER_CLIENT_DATA.cast(builderData);
		}
		return null;
	}
}
