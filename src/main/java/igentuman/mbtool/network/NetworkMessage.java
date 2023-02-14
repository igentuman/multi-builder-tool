package igentuman.mbtool.network;

import igentuman.mbtool.ModConfig;
import igentuman.mbtool.RegistryHandler;
import igentuman.mbtool.common.item.ItemMultiBuilder;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
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
                message.buildPacket(ctx);
            });
            return null;
        }
    }

    public void buildPacket(MessageContext ctx)
    {
        build();
    }

    public static void removeExperience(int amount, EntityPlayer player){
        player.addScore(-amount);
        player.experience -= (float)amount / (float)player.xpBarCap()/10;
        player.experienceTotal -= amount;
        if(player.experience < 0) {
            player.experience = 1f + player.experience;
            player.addExperienceLevel(-1);
        }
    }

    public ItemStack getMbtool(EntityPlayer playerEntity)
    {
        ItemStack mainItem = playerEntity.getHeldItemMainhand();
        ItemStack secondItem = playerEntity.getHeldItemOffhand();
        boolean main = !mainItem.isEmpty() && mainItem.getItem() == RegistryHandler.MBTOOL;
        boolean off = !secondItem.isEmpty() && secondItem.getItem() == RegistryHandler.MBTOOL;

        if(main) {
            if(!mainItem.hasTagCompound()) {
                mainItem.setTagCompound(new NBTTagCompound());
            }
            mainItem.getTagCompound().setInteger("recipe", recipeId);
            mainItem.getTagCompound().setInteger("rotation", rotation);
            return mainItem;
        } else if(off) {
            if(!secondItem.hasTagCompound()) {
                secondItem.setTagCompound(new NBTTagCompound());
            }
            secondItem.getTagCompound().setInteger("recipe", recipeId);
            secondItem.getTagCompound().setInteger("rotation", rotation);
            return secondItem;
        }
        return null;
    }

    private boolean hasRecipe(ItemStack item)
    {
        try {
            return item.getTagCompound().hasKey("recipe");
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    public boolean build()
    {
        MultiblockRecipe recipe = MultiblockRecipes.getAvaliableRecipes().get(recipeId);
        EntityPlayer playerEntity = FMLCommonHandler.instance().getMinecraftServerInstance().
                getPlayerList().getPlayerByUUID(UUID.fromString(player));
        ItemStack mbtool = getMbtool(playerEntity);
        ItemMultiBuilder mbuilder = (ItemMultiBuilder) mbtool.getItem();

        //validate xp
        if(!playerEntity.isCreative()) {
            if (playerEntity.experienceTotal < recipe.getShapeAsBlockPosList().size() * ModConfig.general.xp_per_block) {
                playerEntity.sendMessage(new TextComponentString("Not enough XP"));
                return false;
            }
        }

        //validate energy
        if(!playerEntity.isCreative()) {
            if (mbuilder.getElectricityStored(mbtool) < recipe.getShapeAsBlockPosList().size() * ModConfig.general.energy_per_block) {
                playerEntity.sendMessage(new TextComponentString("Not enough RF"));
                return false;
            }
        }

        //validate if user has all required items
        if(!playerEntity.isCreative()) {
            for (ItemStack required : recipe.getRequiredItemStacks()) {
                int cnt = 0;
                for(int i = 0; i < playerEntity.inventory.getSizeInventory(); i++) {
                    if (playerEntity.inventory.getStackInSlot(i).isItemEqual(required) &&
                        required.getMetadata() == playerEntity.inventory.getStackInSlot(i).getMetadata()) {
                        cnt += playerEntity.inventory.getStackInSlot(i).getCount();
                    }

                }
                if(cnt < required.getCount()) {
                    playerEntity.sendMessage(new TextComponentString("Missing: " + required.getDisplayName()));
                    return false;
                }
            }
        }


        //validate if user has permission to edit each block
        for(int x = 0; x < recipe.getWidth(); x++) {
            for(int y = 0; y < recipe.getHeight(); y++) {
                for(int z = 0; z < recipe.getDepth(); z++) {
                    BlockPos livePos = pos.add(x, y, z);
                    IBlockState state = recipe.getStateAtBlockPos(new BlockPos(x, y, z));
                    boolean check = playerEntity.canPlayerEdit(livePos, EnumFacing.UP, new ItemStack(state.getBlock()));
                    check = check && playerEntity.world.getBlockState(livePos).getBlock().isReplaceable(playerEntity.world, livePos);
                    if(!check) return false;
                }
            }
        }



        //build
        for(int y = 0; y < recipe.getHeight(); y++) {
            for(int x = 0; x < recipe.getWidth(); x++) {
                for(int z = 0; z < recipe.getDepth(); z++) {
                    BlockPos livePos = pos;
                    switch (rotation) {
                        case 0:
                            livePos = pos.add(x, y, z);
                            break;
                        case 1:
                            livePos = pos.add(recipe.getDepth()-1-z,y,x);
                            break;
                        case 2:
                            livePos = pos.add(recipe.getWidth()-1-x,y,recipe.getDepth()-1-z);
                            break;
                        case 3:
                            livePos = pos.add(z,y,recipe.getWidth()-1-x);
                    }
                    boolean foundStack = false;
                    IBlockState state = recipe.getStateAtBlockPos(new BlockPos(x, y, z));
                    if(state.getBlock().equals(Blocks.AIR)) continue;
                    ItemStack stack = findStack(playerEntity, new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state)));
                    if(!stack.equals(ItemStack.EMPTY) && stack.getCount() > 0) {
                        foundStack = true;
                    }
                    stack.shrink(1);
                    if(!playerEntity.isCreative()) {
                        ((ItemMultiBuilder)mbtool.getItem()).setElectricity(mbtool, ((ItemMultiBuilder)mbtool.getItem()).getElectricityStored(mbtool)-ModConfig.general.energy_per_block);
                        removeExperience(ModConfig.general.xp_per_block, playerEntity);
                    }
                    if(!foundStack) {
                        playerEntity.sendMessage(new TextComponentString("Stopped construction on missing: " + new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state)).getDisplayName()));
                        return false;
                    }
                    playerEntity.world.setBlockState(livePos, state, 2);
                }
            }
        }
        if(!playerEntity.isCreative()) {
            playerEntity.getFoodStats().
                    setFoodSaturationLevel(playerEntity.getFoodStats().getSaturationLevel()-ModConfig.general.saturation_per_building);
        }
        return true;
    }

    protected ItemStack findStack(EntityPlayer player, ItemStack item)
    {
        for (int i = 0; i < player.inventory.getSizeInventory(); ++i)
        {
            ItemStack itemstack = player.inventory.getStackInSlot(i);

            if (itemstack.isItemEqual(item) && itemstack.getMetadata() == item.getMetadata())
            {
                return itemstack;
            }
        }
        return ItemStack.EMPTY;
    }
}
