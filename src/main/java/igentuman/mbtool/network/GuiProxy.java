package igentuman.mbtool.network;

import igentuman.mbtool.client.gui.GuiMbtool;
import igentuman.mbtool.common.container.ContainerMbtool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiProxy implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        switch (id) {
            case 0:
                return new ContainerMbtool(player);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        switch (id) {
            case 0:
                return new GuiMbtool(new ContainerMbtool(player));
        }
        return null;
    }
}