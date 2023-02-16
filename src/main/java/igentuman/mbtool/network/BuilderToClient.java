package igentuman.mbtool.network;

import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.Validate;

import java.nio.charset.StandardCharsets;

public class BuilderToClient implements IMessage {

    public BlockPos pos;
    public int recipeId;
    public String player;
    public boolean result;

    public BuilderToClient() {
    }

    public BuilderToClient(BlockPos pos, boolean result, int recipeId, String player) {
        this.pos = pos;
        this.recipeId = recipeId;
        this.player = player;
        this.result = result;
    }

    public void fromBytes(ByteBuf buf) {
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        this.recipeId = buf.readInt();
        this.result = buf.readBoolean();
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
        buf.writeInt(this.recipeId);
        buf.writeBoolean(this.result);
        writeUTF8String(buf, (String)player);
    }

    public static class Handler implements IMessageHandler<BuilderToClient, IMessage> {

        @Override
        public IMessage onMessage(BuilderToClient message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                playPlacedSound();
                spawnParticles(message);
            });
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    public static void playPlacedSound()
    {
        Minecraft.getMinecraft().player.playSound(SoundEvent.REGISTRY.getObject(new ResourceLocation("entity.shulker.shoot")),0.3f,1);
    }

    @SideOnly(Side.CLIENT)
    public static void spawnParticles(BuilderToClient msg)
    {
        BlockPos pos = msg.pos;
        MultiblockRecipe recipe = MultiblockRecipes.getAvaliableRecipes().get(msg.recipeId);
        float size = Math.min((float)recipe.getWidth(), (float)recipe.getDepth())/2+0.5f;
        World world = Minecraft.getMinecraft().player.world;
        world.spawnParticle(EnumParticleTypes.CRIT, pos.getX()-size, pos.getY()+.01f, pos.getZ()-size, -1.1d,1.5d, -1.1d);
        world.spawnParticle(EnumParticleTypes.CRIT, pos.getX()+size, pos.getY()+.01f, pos.getZ()+size, 1.1d,1.5d, 1.1d);
        world.spawnParticle(EnumParticleTypes.CRIT, pos.getX()-size, pos.getY()+.01f, pos.getZ()+size, -1.1d,1.5d, 1.1d);
        world.spawnParticle(EnumParticleTypes.CRIT, pos.getX()+size, pos.getY()+.01f, pos.getZ()-size, 1.1d,1.5d, -1.1d);
    }
}
