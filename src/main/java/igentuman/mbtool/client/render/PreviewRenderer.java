package igentuman.mbtool.client.render;

public class PreviewRenderer {

/*

    // Interpolate alpha based on partialTicks
    private static float interpolatedAlpha = 0.5F;
    public static ItemMultiBuilder mbtool;
    public static MultiblockRecipe recipe = null;
    public static Minecraft mc = Minecraft.getMinecraft();
    public static EntityPlayer player = mc.player;
    private static int height;
    private static int length;
    private static int width;
    private static int rotation;
    private static BlockPos hit;
    private static World world = player.world;

    public static BlockPos getRayTraceHit()
    {
        Vec3d vec = player.getLookVec();
        RayTraceResult rt = player.rayTrace(14, 1f);

        if(!rt.typeOfHit.equals(RayTraceResult.Type.BLOCK)) {
            return null;
        }
        ItemStack mainItem = player.getHeldItemMainhand();
        ItemStack secondItem = player.getHeldItemOffhand();

        boolean main = !mainItem.isEmpty() && mainItem.getItem() == RegistryHandler.MBTOOL && ClientHandler.hasRecipe(mainItem);
        boolean off = !secondItem.isEmpty() && secondItem.getItem() == RegistryHandler.MBTOOL && ClientHandler.hasRecipe(secondItem);

        hit = rt.getBlockPos();
        EnumFacing look = (Math.abs(vec.z) > Math.abs(vec.x)) ? (vec.z > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH) : (vec.x > 0 ? EnumFacing.EAST : EnumFacing.WEST);

        if(!ClientHandler.hasRecipe(mainItem) && !ClientHandler.hasRecipe(secondItem)) return null;

        IBlockState state = world.getBlockState(hit);


        mbtool = null;
        if(main) {
            recipe = MultiblockRecipes.getAvaliableRecipes().get(mainItem.getTagCompound().getInteger("recipe"));
            mbtool = (ItemMultiBuilder) mainItem.getItem();
        } else if(off) {
            recipe = MultiblockRecipes.getAvaliableRecipes().get(secondItem.getTagCompound().getInteger("recipe"));
            mbtool = (ItemMultiBuilder)secondItem.getItem();
        }
        if(mbtool == null || recipe == null) return null;
        rotation = mbtool.getRotation();
        if(!recipe.allowRotate) {
            rotation = 0;
        }
        int maxSize = Math.max(recipe.getWidth(), recipe.getDepth())-1;
        switch (rt.sideHit) {
            case DOWN:
                hit = hit.add(0,-recipe.getHeight(),0);
                break;
            case UP:
                if (!state.getBlock().isReplaceable(world, hit))
                {
                    hit = hit.add(0, 1, 0);
                }
                break;
            case EAST:
                hit = hit.add(maxSize,0,0);
            break;
            case WEST:
                hit = hit.add(-maxSize,0,0);
                break;
            case NORTH:
                hit = hit.add(0,0,-maxSize);
                break;
            case SOUTH:
                hit = hit.add(0,0,maxSize);
                break;
        }
        if(rotation == 0 || rotation == 2) {
            hit = hit.add(-recipe.getWidth() / 2, 0, -recipe.getDepth() / 2);
        } else {
            hit = hit.add(-recipe.getDepth() / 2, 0, -recipe.getWidth() / 2);
        }
        if(recipe.getWidth() % 2 != 0) {
            // hit = hit.add(-1, 0, 0);
        }

        if(recipe.getDepth() % 2 != 0) {
            //hit = hit.add(0, 0, -1);
        }

        return hit;
    }

    public static boolean renderPreview(float partialTicks)
    {
        BlockPos hit = getRayTraceHit();
        if(hit == null) return false;

        GlStateManager.pushMatrix();

        height = recipe.getHeight();
        length = recipe.getDepth();
        width = recipe.getWidth();

        double px = TileEntityRendererDispatcher.staticPlayerX;
        double py = TileEntityRendererDispatcher.staticPlayerY;
        double pz = TileEntityRendererDispatcher.staticPlayerZ;

        GlStateManager.translate(hit.getX() - px, hit.getY() - py, hit.getZ() - pz);

        GlStateManager.disableLighting();
        if (Minecraft.isAmbientOcclusionEnabled())
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        else
            GlStateManager.shadeModel(GL11.GL_FLAT);



        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.enableBlend();
        renderPreviewBlocks(partialTicks);


        renderBoundaries();

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableBlend();

        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
        return true;
    }

    private static void renderBoundaries() {
        int idx = 0;
        GlStateManager.disableDepth();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int bWidth = length;
        int bLength = width;
        for (int h = 0; h < height; h++) {
            for (int l = 0; l < bLength; l++) {
                for (int w = 0; w < bWidth; w++) {
                   // BlockPos pos = new BlockPos(l, h, w);
                    GlStateManager.pushMatrix();

                    int xo = l;
                    int zo = w;
                    switch (rotation)
                    {
                        case 1:
                            zo = l;
                            xo = (bWidth - w - 1);
                            break;
                        case 2:
                            xo = (bLength - l - 1);
                            zo = (bWidth - w - 1);
                            break;
                        case 3:
                            zo = (bLength - l - 1);
                            xo = w;
                            break;
                    }
                    BlockPos actualPos = hit.add(xo, h, zo);

                    boolean isEmpty = world.getBlockState(actualPos).getBlock().isReplaceable(world, actualPos);
                    if(!isEmpty || ((w == 0 || w == bWidth-1) && (l == 0 || l == bLength-1) && (h == 0 || h == height-1))) {

                        GlStateManager.pushMatrix();
                        GlStateManager.disableTexture2D();
                        GlStateManager.enableBlend();
                        GlStateManager.disableCull();
                        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                        float r = isEmpty ? 0 : 1;
                        float g = isEmpty ? 1 : 0;
                        float b = 0;
                        float alpha = 0.2F;


                        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                        GlStateManager.glLineWidth(5f);
                        GlStateManager.translate(xo + .5, h + .5, zo + .5);
                        GlStateManager.scale(1.01, 1.01, 1.01);

                        if(!isEmpty || h == height-1) { //top face
                            buffer.pos(-.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();

                        }
                        if(!isEmpty) {
                            buffer.pos(-.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, .5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, .5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                        }

                        if(!isEmpty || h == 0) {
                            buffer.pos(-.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, -.5F, .5F).color(r, g, b, alpha).endVertex();
                            buffer.pos(-.5F, -.5F, -.5F).color(r, g, b, alpha).endVertex();
                        }
                        tessellator.draw();
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
    }
    private static float dir = 0.005f;

    private static void renderPreviewBlocks(float partialTicks) {
        BlockRendererDispatcher blockRender = mc.getBlockRendererDispatcher();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.1f);
        interpolatedAlpha = interpolatedAlpha + partialTicks*dir;
        if(interpolatedAlpha >= 0.8f) {
            dir = -0.005f;
        }
        if(interpolatedAlpha <= 0.2f) {
            dir = 0.005f;
        }
        int size = Math.max(length, width);
        int bWidth = length;
        int idx = 0;
        for (int h = 0; h < height; h++) {
            for (int l = 0; l < size; l++) {
                for (int w = 0; w < size; w++) {
                    GlStateManager.pushMatrix();
                    BlockPos pos = new BlockPos(l, h, w);
                    IBlockState state = recipe.getStateAtBlockPos(pos);
                    if(!state.equals(Blocks.AIR.getDefaultState())) {
                        int xo = l;
                        int zo = w;
                        switch (rotation)
                        {
                            case 1:
                                zo = l;
                                xo = (width - w - 1);
                                break;
                            case 2:
                                xo = (length - l - 1);
                                zo = (width - w - 1);
                                break;
                            case 3:
                                zo = (length - l - 1);
                                xo = w;
                                break;
                        }

                        BlockPos actualPos = hit.add(xo, h, zo);

                        boolean isEmpty = world.getBlockState(actualPos).getBlock().isReplaceable(world, actualPos);
                        int min= Math.min(width,length);
                        if(isEmpty) {
                            switch (rotation) {
                                case 1:
                                    xo -= size-min;
                                    break;
                                case 2:
                                    xo += size-min;
                                    zo -= size-min;
                                    break;
                                case 3:
                                    zo += size-min;
                                    break;
                            }
                            GlStateManager.translate(xo, h, zo + 1);

                            blockRender.renderBlockBrightness(state, interpolatedAlpha);
                        }
                    }
                    GlStateManager.popMatrix();
                    idx++;
                }
            }
        }
        GlStateManager.depthMask(true);

        GlStateManager.disableBlend();
    }
*/

}
