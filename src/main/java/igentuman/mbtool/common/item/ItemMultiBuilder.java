package igentuman.mbtool.common.item;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.RegistryHandler;
import igentuman.mbtool.network.ModPacketHandler;
import igentuman.mbtool.network.NetworkMessage;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import mekanism.api.EnumColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import org.lwjgl.opengl.GL11;

import java.util.List;

public class ItemMultiBuilder extends Item {

    public ItemMultiBuilder() {
        super();
        MinecraftForge.EVENT_BUS.register(this);
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
         return EnumActionResult.SUCCESS;
    }



    int xd = 0;
    int zd = 0;

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add(EnumColor.AQUA + "\u00a7o" + I18n.format("tooltip.mbtool.gui_key"));
        tooltip.add(EnumColor.AQUA + "\u00a7o" + I18n.format("tooltip.mbtool.rotate_keys"));
    }

    private boolean hasRecipe(ItemStack item)
    {
        try {
           return item.getTagCompound().hasKey("recipe");
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    public BlockPos getRayTraceHit()
    {
        Minecraft mc = Minecraft.getMinecraft();

        Vec3d vec = mc.player.getLookVec();
        RayTraceResult rt = mc.player.rayTrace(10, 1f);
        if(!rt.typeOfHit.equals(RayTraceResult.Type.BLOCK)) {
            return null;
        }
        ItemStack mainItem = mc.player.getHeldItemMainhand();
        ItemStack secondItem = mc.player.getHeldItemOffhand();

        boolean main = !mainItem.isEmpty() && mainItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(mainItem);
        boolean off = !secondItem.isEmpty() && secondItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(secondItem);

        BlockPos hit = rt.getBlockPos();
        EnumFacing look = (Math.abs(vec.z) > Math.abs(vec.x)) ? (vec.z > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH) : (vec.x > 0 ? EnumFacing.EAST : EnumFacing.WEST);

        IBlockState state = mc.player.world.getBlockState(hit);
        if (!state.getBlock().isReplaceable(mc.player.world, hit))
        {
            hit = hit.add(0, 1, 0);
        }
        MultiblockRecipe recipe;
        if(main) {
            recipe = MultiblockRecipes.getAvaliableRecipes().get(mainItem.getTagCompound().getInteger("recipe"));
        } else {
            recipe = MultiblockRecipes.getAvaliableRecipes().get(secondItem.getTagCompound().getInteger("recipe"));
        }
        int rotation = getRotation();
        hit = hit.add(-recipe.getWidth()/2, 0, -recipe.getDepth()/2+1);

        if(recipe.getWidth() % 2 != 0) {
            // hit = hit.add(-1, 0, 0);
        }

        if(recipe.getDepth() % 2 != 0) {
            //hit = hit.add(0, 0, -1);
        }
        return hit;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void renderLast(RenderWorldLastEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();

        ItemStack mainItem = mc.player.getHeldItemMainhand();
        ItemStack secondItem = mc.player.getHeldItemOffhand();

        boolean main = !mainItem.isEmpty() && mainItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(mainItem);
        boolean off = !secondItem.isEmpty() && secondItem.getItem() == RegistryHandler.MBTOOL && hasRecipe(secondItem);

        if(!main && !off) {
            return;
        }



        BlockPos hit = getRayTraceHit();
        if(hit == null) return;
        MultiblockRecipe recipe;
        if(main) {
            recipe = MultiblockRecipes.getAvaliableRecipes().get(mainItem.getTagCompound().getInteger("recipe"));
        } else {
            recipe = MultiblockRecipes.getAvaliableRecipes().get(secondItem.getTagCompound().getInteger("recipe"));
        }
        GlStateManager.pushMatrix();
        renderSchematic(mc.player, hit, event.getPartialTicks(), recipe);
        GlStateManager.popMatrix();
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
        keyPressDelay=10;
        if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            if(main) setRotation(-1, mainItem);
            if(off) setRotation(-1, secondItem);
        }

        if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            if(main) setRotation(1, mainItem);
            if(off) setRotation(1, secondItem);
        }
    }

    public void renderSchematic(EntityPlayer player, BlockPos hit, float partialTicks, MultiblockRecipe recipe)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if(recipe == null) return;

        int mh = recipe.getHeight();
        int ml = recipe.getDepth();
        int mw = recipe.getWidth();

        double px = TileEntityRendererDispatcher.staticPlayerX;
        double py = TileEntityRendererDispatcher.staticPlayerY;
        double pz = TileEntityRendererDispatcher.staticPlayerZ;
        GlStateManager.translate(hit.getX() - px, hit.getY() - py, hit.getZ() - pz);
        float centerX = (float) Math.floor(-mw >> 1);
        float centerZ = (float) Math.floor(-ml >> 1);
        //GlStateManager.translate(centerX+2, 0, centerZ);
        GlStateManager.disableLighting();
        if (Minecraft.isAmbientOcclusionEnabled())
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        else
            GlStateManager.shadeModel(GL11.GL_FLAT);

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        BlockRendererDispatcher blockRender = mc.getBlockRendererDispatcher();

        int idx = 0;
        for (int h = 0; h < mh; h++) {
            for (int l = 0; l < ml; l++) {
                for (int w = 0; w < mw; w++) {
                    GlStateManager.pushMatrix();
                    BlockPos pos = new BlockPos(l, h, w);
                    if(!recipe.getStateAtBlockPos(pos).equals(Blocks.AIR.getDefaultState())) {
                        int xo = l;
                        int zo = w;
                        int rotation = getRotation();
                        switch (rotation)
                        {
                            case 1:
                                zo = l;
                                xo = (mw - w - 1);
                                break;
                            case 2:
                                xo = (ml - l - 1);
                                zo = (mw - w - 1);
                                break;
                            case 3:
                                zo = (ml - l - 1);
                                xo = w;
                                break;
                        }



                        IBlockState state = recipe.getStateAtBlockPos(pos);
                        ItemStack stack = new ItemStack(state.getBlock());
                        BlockPos actualPos = hit.add(xo, h, zo);
                        IBlockState actualState = player.world.getBlockState(actualPos);

                        boolean isEmpty = player.world.getBlockState(actualPos).getBlock().isReplaceable(player.world, actualPos);
                        if(isEmpty) {
                            GlStateManager.translate(xo, h, zo);
                            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                            blockRender.renderBlockBrightness(state, 0.2f);
                        }

                    }
                    GlStateManager.popMatrix();

                    idx++;
                }
            }
        }
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();

    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        if(playerIn.isSneaking()) {
            playerIn.openGui(Mbtool.instance, 0, worldIn, playerIn.getPosition().getX(), playerIn.getPosition().getY(), playerIn.getPosition().getZ());
            return ActionResult.newResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
        }
        BlockPos hit = getRayTraceHit();
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
        ModPacketHandler.instance.sendToServer(new NetworkMessage(hit, getRotation(), recipeid, playerIn.getUniqueID().toString()));

        return super.onItemRightClick(worldIn, playerIn, handIn);
    }


}
