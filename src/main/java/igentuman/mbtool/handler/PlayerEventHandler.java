package igentuman.mbtool.handler;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class PlayerEventHandler
{


    @SubscribeEvent
    public void attachBuilderDataCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayerMP) {
         //   event.addCapability(BuilderCapabilityHandler.PLAYER_BUILDER_DATA_NAME, new BuilderDataProvider((EntityPlayerMP) event.getObject()));
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void attachBuilderDataCapabilityClient(AttachCapabilitiesEvent<Entity> event) {
        if(event.getObject() instanceof EntityPlayerSP) {
       //     event.addCapability(BuilderClientCapabilityHandler.PLAYER_BUILDER_DATA_CLIENT_NAME, new BuilderClientDataProvider((EntityPlayerSP) event.getObject()));
        }
    }


}
