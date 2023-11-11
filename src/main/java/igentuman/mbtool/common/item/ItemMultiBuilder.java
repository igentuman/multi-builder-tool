package igentuman.mbtool.common.item;

import blusunrize.immersiveengineering.api.energy.immersiveflux.IFluxContainerItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.ModConfig;
import igentuman.mbtool.RegistryHandler;
import igentuman.mbtool.client.render.PreviewRenderer;
import igentuman.mbtool.network.ModPacketHandler;
import igentuman.mbtool.network.NetworkMessage;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import mekanism.api.energy.IEnergizedItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;


import java.util.List;

import static com.mojang.realmsclient.gui.ChatFormatting.AQUA;

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "ic2.api.item.ISpecialElectricItem", modid = "ic2"),
        @Optional.Interface(iface = "ic2.api.item.IElectricItem", modid = "ic2"),
        @Optional.Interface(iface = "mekanism.api.energy.IEnergizedItem", modid = "mekanism"),
        @Optional.Interface(iface = "blusunrize.immersiveengineering.api.energy.immersiveflux.IFluxContainerItem", modid = "immersiveengineering")
})

public class ItemMultiBuilder extends Item implements ISpecialElectricItem, IElectricItem, IEnergizedItem, IFluxContainerItem {

    private static Object itemManagerIC2;
    public int afterPlaceDelay = 0;
    public ItemMultiBuilder() {
        super();
        MinecraftForge.EVENT_BUS.register(this);
        if(Mbtool.hooks.IC2Loaded) {
            itemManagerIC2 = new IC2ElectricManager();
        }
    }

    public CreativeTabs getCreativeTab()
    {
        return CreativeTabs.TOOLS;
    }

    public ItemMultiBuilder setItemName(String name)
    {
        setRegistryName(name);
        setTranslationKey(name);
        return this;
    }

    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
         return EnumActionResult.FAIL;
    }

    public static String getEnergyDisplayRF(float energyVal)
    {
        String val = String.valueOf(MathHelper.floor(energyVal));

        return val + " RF";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add(AQUA + "\u00a7o" + I18n.format("tooltip.mbtool.gui_key"));
        tooltip.add(AQUA + "\u00a7o" + I18n.format("tooltip.mbtool.rotate_keys"));

        String color = "";
        float rf = this.getElectricityStored(stack);

        if (rf <= (float) this.getMaxElectricityStored(stack) / 3)
        {
            color = "\u00a74";
        } else if (rf > (float) (this.getMaxElectricityStored(stack) * 2) / 3)
        {
            color = "\u00a72";
        } else
        {
            color = "\u00a76";
        }

        tooltip.add(color + getEnergyDisplayRF(rf) + "/" + getEnergyDisplayRF(this.getMaxElectricityStored(stack)));
    }

    public float recharge(ItemStack itemStack, float energy, boolean doReceive)
    {
        float rejectedElectricity = Math.max(this.getElectricityStored(itemStack) + energy - this.getMaxElectricityStored(itemStack), 0);
        float energyToReceive = Math.round(Math.min(energy - rejectedElectricity, ((float)ModConfig.general.mbtool_energy_capacity)/10));

        if (doReceive)
        {
            this.setElectricity(itemStack, this.getElectricityStored(itemStack) + energyToReceive);
        }

        return energyToReceive;
    }

    public float discharge(ItemStack itemStack, float energy, boolean doTransfer)
    {
        float thisEnergy = this.getElectricityStored(itemStack);
        float energyToTransfer = Math.min(Math.min(thisEnergy, energy), ((float)ModConfig.general.mbtool_energy_capacity)/10);

        if (doTransfer)
        {
            this.setElectricity(itemStack, thisEnergy - energyToTransfer);
        }

        return energyToTransfer;
    }

    public int getTierGC(ItemStack itemStack)
    {
        return 1;
    }

    public void setElectricity(ItemStack itemStack, float rf)
    {
        if (itemStack.getTagCompound() == null)
        {
            itemStack.setTagCompound(new NBTTagCompound());
        }

        if(!itemStack.getTagCompound().hasKey("electricity")) {
            itemStack.getTagCompound().setInteger("electricity", 0);
        }

        int electricityStored = (int) Math.max(Math.min(rf, this.getMaxElectricityStored(itemStack)), 0);
        itemStack.getTagCompound().setInteger("electricity", electricityStored);
    }


    public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate)
    {
        return (int) (this.recharge(container, ((float)ModConfig.general.mbtool_energy_capacity)/10, !simulate));
    }

    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate)
    {
        return (int) (this.discharge(container, ((float)ModConfig.general.mbtool_energy_capacity)/10, !simulate));
    }

    public int getEnergyStored(ItemStack container)
    {
        return (int) (this.getElectricityStored(container));
    }

    public int getMaxEnergyStored(ItemStack container)
    {
        return (int) (this.getMaxElectricityStored(container));
    }

    // The following seven methods are for Mekanism compatibility

    @Optional.Method(modid = "mekanism")
    public double getEnergy(ItemStack itemStack)
    {
        return this.getElectricityStored(itemStack) * 0.1;
    }

    @Optional.Method(modid = "mekanism")
    public void setEnergy(ItemStack itemStack, double amount)
    {
        float electricityStored = Math.max(Math.min((float) ((float) amount *  0.1)+getElectricityStored(itemStack), this.getMaxElectricityStored(itemStack)), 0);
        this.setElectricity(itemStack, electricityStored);
    }

    @Optional.Method(modid = "mekanism")
    public double getMaxEnergy(ItemStack itemStack)
    {
        return this.getMaxElectricityStored(itemStack)  * 0.1;
    }

    @Optional.Method(modid = "mekanism")
    public double getMaxTransfer(ItemStack itemStack)
    {
        return ModConfig.general.mbtool_energy_capacity * 0.01;
    }

    @Optional.Method(modid = "mekanism")
    public boolean canReceive(ItemStack itemStack)
    {
        return true;
    }

    public boolean canSend(ItemStack itemStack)
    {
        return false;
    }

    public float getElectricityStored(ItemStack itemStack)
    {
        if (itemStack.getTagCompound() == null)
        {
            itemStack.setTagCompound(new NBTTagCompound());
        }
        float energyStored = 0f;
        if (itemStack.getTagCompound().hasKey("electricity"))
        {
            NBTBase obj = itemStack.getTagCompound().getTag("electricity");
            if (obj instanceof NBTTagDouble)
            {
                energyStored = (float) ((NBTTagDouble) obj).getDouble();
            } else if (obj instanceof NBTTagFloat)
            {
                energyStored = ((NBTTagFloat) obj).getFloat();
            }
            else if (obj instanceof NBTTagInt)
            {
                energyStored = ((NBTTagInt) obj).getInt();
            }
        }
        return energyStored;
    }

    public int getMaxElectricityStored(ItemStack item)
    {
        return ModConfig.general.mbtool_energy_capacity;
    }

    @Optional.Method(modid = "ic2")
    public IElectricItemManager getManager(ItemStack itemstack)
    {
        return (IElectricItemManager) itemManagerIC2;
    }

    @Optional.Method(modid = "ic2")
    public boolean canProvideEnergy(ItemStack itemStack)
    {
        return false;
    }

    @Optional.Method(modid = "ic2")
    public int getTier(ItemStack itemStack)
    {
        return 2;
    }

    @Optional.Method(modid = "ic2")
    public double getMaxCharge(ItemStack itemStack)
    {
        return (double) this.getMaxElectricityStored(itemStack) / 4;
    }

    @Optional.Method(modid = "ic2")
    public double getTransferLimit(ItemStack itemStack)
    {
        return 0;
    }

    public boolean hasRecipe(ItemStack item)
    {
        try {
           return item.getTagCompound().hasKey("recipe");
        } catch (NullPointerException ignored) {
            return false;
        }
    }


    public int getRotation()
    {
        Minecraft mc = Minecraft.getMinecraft();

        ItemStack mainItem = mc.player.getHeldItemMainhand();
        ItemStack secondItem = mc.player.getHeldItemOffhand();

        boolean main = !mainItem.isEmpty() && mainItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(mainItem);
        boolean off = !secondItem.isEmpty() && secondItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(secondItem);
        ItemStack item = ItemStack.EMPTY;
        if(main) {
            item = mainItem;
        }
        if(off) {
            item = secondItem;
        }
        if(item.equals(ItemStack.EMPTY)) {
            return 0;
        }
        int rotation = 0;
        try {
            rotation = item.getTagCompound().getInteger("rotation");
        } catch (NullPointerException ignored) {  }
        return rotation;
    }

    int keyPressDelay = 10;

    public void setRotation(int dir, ItemStack item)
    {
        if(hasRecipe(item)) {
            try {
                MultiblockRecipe recipe = MultiblockRecipes.getAvaliableRecipes().
                        get(item.getTagCompound().getInteger("recipe"));
                if(!recipe.allowRotate) {
                    item.getTagCompound().setInteger("rotation", 0);
                    return;
                }
            } catch (NullPointerException ignored) {

            }
        }
        int rot = getRotation();
        rot+=dir;
        if(dir < 0 && rot < 0) {
            rot = 3;
        }
        if(dir > 0 && rot > 3) {
            rot = 0;
        }
        item.getTagCompound().setInteger("rotation", rot);
    }



    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void handleKeypress(TickEvent.ClientTickEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.player == null) return;

        ItemStack mainItem = mc.player.getHeldItemMainhand();
        ItemStack secondItem = mc.player.getHeldItemOffhand();

        boolean main = !mainItem.isEmpty() && mainItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(mainItem);
        boolean off = !secondItem.isEmpty() && secondItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(secondItem);
        keyPressDelay--;
        if(afterPlaceDelay > 0) {
            afterPlaceDelay--;
        }
        if((!main && !off) || keyPressDelay > 0) {
            return;
        }

        if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            if(main) setRotation(-1, mainItem);
            if(off) setRotation(-1, secondItem);
            keyPressDelay=10;
        }

        if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            if(main) setRotation(1, mainItem);
            if(off) setRotation(1, secondItem);
            keyPressDelay=10;
        }
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        if(playerIn.isSneaking()) {
            playerIn.openGui(Mbtool.instance, 0, worldIn, playerIn.getPosition().getX(), playerIn.getPosition().getY(), playerIn.getPosition().getZ());
            return ActionResult.newResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
        }
        if(keyPressDelay > 0) return ActionResult.newResult(EnumActionResult.FAIL, playerIn.getHeldItem(handIn));
        BlockPos hit = PreviewRenderer.getRayTraceHit();
        if(hit == null) return super.onItemRightClick(worldIn, playerIn, handIn);
        Minecraft mc = Minecraft.getMinecraft();

        ItemStack mainItem = mc.player.getHeldItemMainhand();
        ItemStack secondItem = mc.player.getHeldItemOffhand();

        boolean main = !mainItem.isEmpty() && mainItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(mainItem);
        boolean off = !secondItem.isEmpty() && secondItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(secondItem);
        int recipeid;
        try {
            if (main) {
                recipeid = mainItem.getTagCompound().getInteger("recipe");
            } else {
                recipeid = secondItem.getTagCompound().getInteger("recipe");
            }
        } catch (NullPointerException ignored) {
            return super.onItemRightClick(worldIn, playerIn, handIn);
        }
        keyPressDelay = 10;
        afterPlaceDelay = 40;
        ModPacketHandler.instance.sendToServer(new NetworkMessage(hit, getRotation(), recipeid, playerIn.getUniqueID().toString()));

        return super.onItemRightClick(worldIn, playerIn, handIn);
    }


}
