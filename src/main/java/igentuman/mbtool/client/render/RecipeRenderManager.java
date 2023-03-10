package igentuman.mbtool.client.render;
import igentuman.mbtool.integration.jei.MbtoolRecipeCategory;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.util.ProxyWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.List;

@SideOnly(Side.CLIENT)
public class RecipeRenderManager {
    public static RecipeRenderManager instance = new RecipeRenderManager();

    private HashMap<MultiblockRecipe, RecipeRenderData> data = new HashMap<>();

    public void renderRecipe(MultiblockRecipe recipe, float partialTicks, int layers) {
        if(recipe == null) {
            return;
        }

        if(!data.containsKey(recipe)) {
            data.put(recipe, new RecipeRenderData(recipe));
        }

        RecipeRenderData renderData = data.get(recipe);
        renderData.setLayers(layers);
        if(renderData.requiresNewDisplayList(layers)) {
            renderData.initializeDisplayList();
        }

        renderData.render(partialTicks);
    }

    public static class RecipeRenderData {
        ProxyWorld proxyWorld;
        IBlockAccess blockAccess;
        int glListId = -1;
        public List<BlockPos> toRender;

        public RecipeRenderData(MultiblockRecipe recipe) {
            this.proxyWorld = new ProxyWorld();
            this.blockAccess = recipe.getBlockAccess(proxyWorld);
            proxyWorld.setFakeWorld(blockAccess);

            this.toRender = recipe.getShapeAsBlockPosList();
        }
        protected int layers;
        public boolean requiresNewDisplayList(int layers) {
            return glListId == -1 || layers != this.layers;
        }
        public void setLayers(int layers)
        {
            this.layers = layers-1;
        }
        public void render(float partialTicks) {
            GlStateManager.callList(this.glListId);

            ForgeHooksClient.setRenderLayer(BlockRenderLayer.SOLID);

            TileEntityRendererDispatcher renderer = TileEntityRendererDispatcher.instance;
            renderer.renderEngine = Minecraft.getMinecraft().renderEngine;

            for (BlockPos pos : toRender) {
                if(pos.getY() > layers) continue;
                TileEntity renderTe = proxyWorld.getTileEntity(pos);
                if(renderTe != null) {
                    renderTe.setWorld(proxyWorld);
                    renderTe.setPos(pos);

                    if(renderTe instanceof ITickable) {
                        ((ITickable) renderTe).update();
                    }

                    GlStateManager.pushMatrix();
                    GlStateManager.pushAttrib();

                    try {
                        //renderer.preDrawBatch();
                        renderer.render(renderTe, pos.getX(), pos.getY(), pos.getZ(), 0.0f);
                        //renderer.drawBatch(0);
                    } catch(Exception e) {
                    }


                    GlStateManager.popAttrib();
                    GlStateManager.popMatrix();
                }
            }

            ForgeHooksClient.setRenderLayer(null);
        }

        public void initializeDisplayList() {
            this.glListId = GLAllocation.generateDisplayLists(1);

            GlStateManager.glNewList(glListId, GL11.GL_COMPILE);

            GlStateManager.pushAttrib();
            GlStateManager.pushMatrix();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            // Aaaand render
            BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            GlStateManager.disableAlpha();
            this.renderLayer(blockrendererdispatcher, buffer, BlockRenderLayer.SOLID);
            GlStateManager.enableAlpha();
            this.renderLayer(blockrendererdispatcher, buffer, BlockRenderLayer.CUTOUT_MIPPED);
            this.renderLayer(blockrendererdispatcher, buffer, BlockRenderLayer.CUTOUT);
            GlStateManager.shadeModel(GL11.GL_FLAT);
            this.renderLayer(blockrendererdispatcher, buffer, BlockRenderLayer.TRANSLUCENT);

            tessellator.draw();

            GlStateManager.popMatrix();
            GlStateManager.popAttrib();

            GlStateManager.glEndList();
        }

        public void renderLayer(BlockRendererDispatcher blockrendererdispatcher, BufferBuilder buffer, BlockRenderLayer renderLayer) {
            for (BlockPos pos : toRender) {
                if(pos.getY() > layers) continue;

                IBlockState state = proxyWorld.getBlockState(pos);
                if (!state.getBlock().canRenderInLayer(state, renderLayer)) {
                    continue;
                }

                ForgeHooksClient.setRenderLayer(renderLayer);
                try {
                    blockrendererdispatcher.renderBlock(state, pos, blockAccess, buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ForgeHooksClient.setRenderLayer(null);
            }
        }
    }


}
