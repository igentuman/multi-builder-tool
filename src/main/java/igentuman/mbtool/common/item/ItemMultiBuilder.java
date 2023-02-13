package igentuman.mbtool.common.item;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.RegistryHandler;
import igentuman.mbtool.handler.PreviewRenderBlockEvent;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import igentuman.mbtool.util.ShaderUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

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

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void renderLast(RenderWorldLastEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();

        ItemStack mainItem = mc.player.getHeldItemMainhand();
        ItemStack secondItem = mc.player.getHeldItemOffhand();

        boolean main = !mainItem.isEmpty() && mainItem.getItem() == RegistryHandler.MBTOOL && ItemNBTHelper.hasKey(mainItem, "recipe");
        boolean off = !secondItem.isEmpty() && secondItem.getItem() == RegistryHandler.MBTOOL && ItemNBTHelper.hasKey(secondItem, "recipe");

        if(!main && !off) {
            return;
        }

        Vec3d vec = mc.player.getLookVec();
        RayTraceResult rt = mc.player.rayTrace(10, 1f);
        if(!rt.typeOfHit.equals(RayTraceResult.Type.BLOCK)) {
            return;
        }
        BlockPos hit = rt.getBlockPos();
        EnumFacing look = (Math.abs(vec.z) > Math.abs(vec.x)) ? (vec.z > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH) : (vec.x > 0 ? EnumFacing.EAST : EnumFacing.WEST);
        if (look == EnumFacing.NORTH || look == EnumFacing.SOUTH)
        {
            hit = hit.add(-xd / 2, 0, 0);
        }
        else if (look == EnumFacing.EAST || look == EnumFacing.WEST)
        {
            hit = hit.add(0, 0, -zd / 2);
        }

        if (look == EnumFacing.NORTH)
        {
            hit = hit.add(0, 0, -zd + 1);
        }
        else if (look == EnumFacing.WEST)
        {
            hit = hit.add(-xd + 1, 0, 0);
        }
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
        GlStateManager.pushMatrix();
        renderSchematic(mc.player, hit, event.getPartialTicks(), recipe);
        GlStateManager.popMatrix();
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

        GlStateManager.disableLighting();
        if (Minecraft.isAmbientOcclusionEnabled())
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        else
            GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();


        ClientUtils.mc().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        float flicker = (player.world.rand.nextInt(10) == 0) ? 0.75F : (player.world.rand.nextInt(20) == 0 ? 0.5F : 1F);
        boolean perfect = true;
        final BlockRendererDispatcher blockRender = Minecraft.getMinecraft().getBlockRendererDispatcher();

        int idx = 0;
        for (int h = 0; h < mh; h++) {
            for (int l = 0; l < ml; l++) {
                for (int w = 0; w < mw; w++) {
                    GlStateManager.pushMatrix();
                    BlockPos pos = new BlockPos(l, h, w);
                    if(!recipe.getStateAtBlockPos(pos).equals(Blocks.AIR.getDefaultState())) {
                        int xo = l;
                        int zo = w;
                        IBlockState state = recipe.getStateAtBlockPos(pos);
                        ItemStack stack = new ItemStack(state.getBlock());
                        BlockPos actualPos = hit.add(xo, h, zo);
                        IBlockState actualState = player.world.getBlockState(actualPos);

                        boolean isEmpty = player.world.getBlockState(actualPos).getBlock().isReplaceable(player.world, actualPos);
                        if(isEmpty) {
                            GlStateManager.translate(xo, h, zo);
                            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                            GlStateManager.shadeModel(GL11.GL_SMOOTH);
                            blockRender.renderBlockBrightness(state, 0.5f);
                        }

                    }
                    GlStateManager.popMatrix();

                    idx++;
                }
            }
        }
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
if(true) return;
        idx = 0;
        GlStateManager.disableDepth();

        for (int h = 0; h < mh; h++) {
            for (int l = 0; l < ml; l++) {
                for (int w = 0; w < mw; w++) {
                    BlockPos pos = new BlockPos(l, h, w);
                    GlStateManager.pushMatrix();
                    if (!recipe.getStateAtBlockPos(pos).equals(Blocks.AIR.getDefaultState())) {

                        int xo = l;
                        int zo = w;
                        BlockPos actualPos = hit.add(xo, h, zo);

                        IBlockState otherState = null;
                        IBlockState state = recipe.getStateAtBlockPos(pos);
                        ItemStack stack = new ItemStack(state.getBlock());
                        IBlockState actualState = player.world.getBlockState(actualPos);
                        boolean stateEqual = actualState.equals(state);
                        boolean otherStateEqual = otherState == null ? false : otherState.equals(state);

                        boolean isEmpty = player.world.getBlockState(actualPos).getBlock().isReplaceable(player.world, actualPos);
                        if(isEmpty && (w > 0 && w < mw-1 && l > 0 && l < ml -1 && h > 0 && h < mh-1)) {
                            continue;
                        }
                        GlStateManager.pushMatrix();
                        GlStateManager.disableTexture2D();
                        GlStateManager.enableBlend();
                        GlStateManager.disableCull();
                        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                        GlStateManager.shadeModel(GL11.GL_SMOOTH);
                        float r = 1;
                        float g = !isEmpty ? 0 : 1;
                        float b = !isEmpty ? 0 : 1;
                        float alpha = .375F * flicker;
                        GlStateManager.translate(xo + .5, h + .5, zo + .5);
                        GlStateManager.scale(1.01, 1.01, 1.01);

                        GlStateManager.glLineWidth(2f);
                        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                        buffer.pos(-.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();

                        buffer.pos(-.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();

                        buffer.pos(-.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                        buffer.pos(-.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();

                        tessellator.draw();
                        buffer.setTranslation(0, 0, 0);
                        GlStateManager.shadeModel(GL11.GL_FLAT);
                        GlStateManager.enableCull();
                        GlStateManager.disableBlend();
                        GlStateManager.enableTexture2D();
                        GlStateManager.popMatrix();
                    }
                    GlStateManager.popMatrix();

                    idx++;
                }
            }
        }
        GlStateManager.enableDepth();

    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        if(playerIn.isSneaking()) {
            playerIn.openGui(Mbtool.instance, 0, worldIn, playerIn.getPosition().getX(), playerIn.getPosition().getY(), playerIn.getPosition().getZ());
            return ActionResult.newResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }


}
