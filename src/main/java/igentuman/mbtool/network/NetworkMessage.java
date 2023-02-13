package igentuman.mbtool.network;

import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.commons.lang3.Validate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class NetworkMessage implements IMessage {

    public BlockPos pos;
    public int rotation;
    public int recipeId;
    public String player;

    public NetworkMessage() {
    }

    public NetworkMessage(BlockPos pos, int rotation, int recipeId, String player) {
        this.pos = pos;
        this.rotation = rotation;
        this.recipeId = recipeId;
        this.player = player;
    }

    public void fromBytes(ByteBuf buf) {
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        this.rotation = buf.readInt();
        this.recipeId = buf.readInt();
        this.player = readUTF8String(buf);
    }

    public static void writeUTF8String(ByteBuf to, String string) {
        byte[] utf8Bytes = string.getBytes(StandardCharsets.UTF_8);
        Validate.isTrue(varIntByteCount(utf8Bytes.length) < 3, "The string is too long for this encoding.", new Object[0]);
        writeVarInt(to, utf8Bytes.length, 2);
        to.writeBytes(utf8Bytes);
    }

    public static void writeVarInt(ByteBuf to, int toWrite, int maxSize) {
        Validate.isTrue(varIntByteCount(toWrite) <= maxSize, "Integer is too big for %d bytes", (long)maxSize);

        while((toWrite & -128) != 0) {
            to.writeByte(toWrite & 127 | 128);
            toWrite >>>= 7;
        }

        to.writeByte(toWrite);
    }

    public static int varIntByteCount(int toCount) {
        return (toCount & -128) == 0 ? 1 : ((toCount & -16384) == 0 ? 2 : ((toCount & -2097152) == 0 ? 3 : ((toCount & -268435456) == 0 ? 4 : 5)));
    }

    public static String readUTF8String(ByteBuf from) {
        int len = readVarInt(from, 2);
        String str = from.toString(from.readerIndex(), len, StandardCharsets.UTF_8);
        from.readerIndex(from.readerIndex() + len);
        return str;
    }

    public static int readVarInt(ByteBuf buf, int maxSize) {
        Validate.isTrue(maxSize < 6 && maxSize > 0, "Varint length is between 1 and 5, not %d", (long)maxSize);
        int i = 0;
        int j = 0;

        byte b0;
        do {
            b0 = buf.readByte();
            i |= (b0 & 127) << j++ * 7;
            if (j > maxSize) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b0 & 128) == 128);

        return i;
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.pos.getX());
        buf.writeInt(this.pos.getY());
        buf.writeInt(this.pos.getZ());
        buf.writeInt(this.rotation);
        buf.writeInt(this.recipeId);
        writeUTF8String(buf, (String)player);
    }

    public static class Handler implements IMessageHandler<NetworkMessage, IMessage> {

        @Override
        public IMessage onMessage(NetworkMessage message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                message.handleProcessUpdatePacket(ctx);
            });
            return null;
        }
    }

    public void handleProcessUpdatePacket(MessageContext ctx)
    {
        build();
    }


    public boolean build()
    {
        MultiblockRecipe recipe = MultiblockRecipes.getAvaliableRecipes().get(recipeId);
        EntityPlayer playerEntity = FMLCommonHandler.instance().getMinecraftServerInstance().
                getPlayerList().getPlayerByUUID(UUID.fromString(player));

        for(int x = 0; x < recipe.getWidth(); x++) {
            for(int y = 0; y < recipe.getHeight(); y++) {
                for(int z = 0; z < recipe.getDepth(); z++) {
                    BlockPos livePos = pos.add(x, y, z);
                    IBlockState state = recipe.getStateAtBlockPos(new BlockPos(x, y, z));
                    boolean check = playerEntity.canPlayerEdit(livePos, EnumFacing.UP, new ItemStack(state.getBlock()));
                    if(!check) return false;
                }
            }
        }

        for(int x = 0; x < recipe.getWidth(); x++) {
            for(int y = 0; y < recipe.getHeight(); y++) {
                for(int z = 0; z < recipe.getDepth(); z++) {
                    BlockPos livePos = pos.add(x, y, z);
                    IBlockState state = recipe.getStateAtBlockPos(new BlockPos(x, y, z));
                    playerEntity.world.setBlockState(livePos, state, 2);
                }
            }
        }
        return true;
    }
}