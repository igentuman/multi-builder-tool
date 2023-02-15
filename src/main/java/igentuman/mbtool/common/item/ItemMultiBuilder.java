package igentuman.mbtool.common.item;

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
import mekanism.api.EnumColor;
import mekanism.api.energy.IEnergizedItem;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import org.lwjgl.opengl.GL11;

import java.util.List;

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "ic2.api.item.ISpecialElectricItem", modid = "ic2"),
        @Optional.Interface(iface = "ic2.api.item.IElectricItem", modid = "ic2"),
        @Optional.Interface(iface = "mekanism.api.energy.IEnergizedItem", modid = "mekanism")
})
public class ItemMultiBuilder extends Item implements ISpecialElectricItem, IElectricItem, IEnergizedItem {

    private static Object itemManagerIC2;

    public ItemMultiBuilder() {
        super();
        MinecraftForge.EVENT_BUS.register(this);
        setMaxDamage(ModConfig.general.mbtool_energy_capacity);
        this.setNoRepair();
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

    int xd = 0;
    int zd = 0;

    public static String getEnergyDisplayRF(float energyVal)
    {
        String val = String.valueOf(MathHelper.floor(energyVal));

        return val + " RF";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add(EnumColor.AQUA + "\u00a7o" + I18n.format("tooltip.mbtool.gui_key"));
        tooltip.add(EnumColor.AQUA + "\u00a7o" + I18n.format("tooltip.mbtool.rotate_keys"));

        String color = "";
        float rf = this.getElectricityStored(stack);

        if (rf <= this.getMaxElectricityStored(stack) / 3)
        {
            color = "\u00a74";
        } else if (rf > this.getMaxElectricityStored(stack) * 2 / 3)
        {
            color = "\u00a72";
        } else
        {
            color = "\u00a76";
        }

        tooltip.add(color + getEnergyDisplayRF(rf) + "/" + getEnergyDisplayRF(this.getMaxElectricityStored(stack)));

    }
    /**
     * Makes sure the item is uncharged when it is crafted and not charged.
     * Change this if you do not want this to happen!
     */
    @Override
    public void onCreated(ItemStack itemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
        this.setElectricity(itemStack, 0);
    }

    public float recharge(ItemStack itemStack, float energy, boolean doReceive)
    {
        float rejectedElectricity = Math.max(this.getElectricityStored(itemStack) + energy - this.getMaxElectricityStored(itemStack), 0);
        float energyToReceive = energy - rejectedElectricity;
        if (energyToReceive > ModConfig.general.mbtool_energy_capacity/10)
        {
            rejectedElectricity += energyToReceive - ModConfig.general.mbtool_energy_capacity/10;
            energyToReceive  =ModConfig.general.mbtool_energy_capacity/10;
        }

        if (doReceive)
        {
            this.setElectricity(itemStack, this.getElectricityStored(itemStack) + energyToReceive);
        }

        return energyToReceive;
    }

    public float discharge(ItemStack itemStack, float energy, boolean doTransfer)
    {
        float thisEnergy = this.getElectricityStored(itemStack);
        float energyToTransfer = Math.min(Math.min(thisEnergy, energy), ModConfig.general.mbtool_energy_capacity/10);

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

        float electricityStored = Math.max(Math.min(rf, this.getMaxElectricityStored(itemStack)), 0);
        if (rf > 0F || itemStack.getTagCompound().hasKey("electricity"))
        {
            itemStack.getTagCompound().setFloat("electricity", electricityStored);
        }

        itemStack.setItemDamage(ModConfig.general.mbtool_energy_capacity - (int) (electricityStored / this.getMaxElectricityStored(itemStack) * ModConfig.general.mbtool_energy_capacity));
    }


    public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate)
    {
        return (int) (this.recharge(container, ModConfig.general.mbtool_energy_capacity/10, !simulate));
    }

    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate)
    {
        return (int) (this.discharge(container, ModConfig.general.mbtool_energy_capacity/10, !simulate));
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
        this.setElectricity(itemStack, (float) ((float) amount *  0.1));
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
                energyStored = ((NBTTagDouble) obj).getFloat();
            } else if (obj instanceof NBTTagFloat)
            {
                energyStored = ((NBTTagFloat) obj).getFloat();
            }
        } else
        {
            if (itemStack.getItemDamage() == ModConfig.general.mbtool_energy_capacity)
                return 0F;

            energyStored = this.getMaxElectricityStored(itemStack) * (ModConfig.general.mbtool_energy_capacity - itemStack.getItemDamage()) / ModConfig.general.mbtool_energy_capacity;
            itemStack.getTagCompound().setFloat("electricity", energyStored);
        }

        /** Sets the damage as a percentage to render the bar properly. */
        itemStack.setItemDamage(ModConfig.general.mbtool_energy_capacity - (int) (energyStored / this.getMaxElectricityStored(itemStack) * ModConfig.general.mbtool_energy_capacity));
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
        return this.getMaxElectricityStored(itemStack) / 4;
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
        if(main) {
            recipeid = mainItem.getTagCompound().getInteger("recipe");
        } else {
            recipeid = secondItem.getTagCompound().getInteger("recipe");
        }
        keyPressDelay = 10;
        ModPacketHandler.instance.sendToServer(new NetworkMessage(hit, getRotation(), recipeid, playerIn.getUniqueID().toString()));

        return super.onItemRightClick(worldIn, playerIn, handIn);
    }


}
