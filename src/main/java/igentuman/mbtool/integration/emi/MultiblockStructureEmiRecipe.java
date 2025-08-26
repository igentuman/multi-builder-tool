package igentuman.mbtool.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MultiblockStructureEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final CompoundTag structureNbt;
    private final String name;
    private final MultiblockStructure structure;
    private final List<EmiStack> outputs;
    private final List<EmiIngredient> inputs;
    public int currentLayer = 0;
    
    public MultiblockStructureEmiRecipe(ResourceLocation id, CompoundTag structureNbt, String name) {
        this.id = id;
        this.structureNbt = structureNbt;
        this.name = name;
        this.structure = new MultiblockStructure(structureNbt);
        this.currentLayer = structure.getMaxY();
        
        // Calculate required blocks
        this.outputs = new ArrayList<>();
        this.inputs = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        for (BlockPos pos : structure.getBlocks().keySet()) {
            Block block = structure.getBlocks().get(pos).getBlock();
            if (!blocks.contains(block)) {
                blocks.add(block);
                inputs.add(EmiIngredient.of(Ingredient.of(new ItemStack(block))));
                outputs.add(EmiStack.of(new ItemStack(block)));
            }
        }
        
        // Count blocks
        for (EmiStack stackItem : outputs) {
            int count = 0;
            for (BlockState blockState : structure.getBlocks().values()) {
                if (stackItem.getItemStack().is(blockState.getBlock().asItem())) {
                    count++;
                }
            }
            stackItem.setAmount(count);
        }

    }
    
    @Override
    public EmiRecipeCategory getCategory() {
        return MultiblockStructureEmiCategory.INSTANCE;
    }
    
    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }
    
    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return List.of(EmiIngredient.of(Ingredient.of(Mbtool.MBTOOL.get())));
    }
    
    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }
    
    @Override
    public int getDisplayWidth() {
        return 176;
    }
    
    @Override
    public int getDisplayHeight() {
        return 158;
    }
    
    @Override
    public void addWidgets(WidgetHolder widgets) {
        // Add structure name
        widgets.addText(Component.translatable(name), 5, 2, 0xFFFFFFFF, false);
        
        // Add ingredients as slots
        int slotIndex = 0;
        for (EmiStack output : outputs) {
            if(output.isEmpty()) continue;
            if (slotIndex < 30) { // Limit to 9 slots for display
                SlotWidget slot =widgets.addSlot(output, 5 + (slotIndex % 4) * 18, 12 + (slotIndex / 4) * 18);
                slot.recipeContext(this);
                slotIndex++;
            }
        }
        
        widgets.add(new MultiblockRenderWidget(50, 10, 136, 80, this));
    }
    
    public String getName() {
        return name;
    }
    
    public MultiblockStructure getStructure() {
        return structure;
    }
    
    public void slice() {
        if (structure.getMaxY() < currentLayer) {
            currentLayer = structure.getMaxY();
        }
        currentLayer--;
        if (currentLayer < structure.getMinY()) {
            currentLayer = structure.getMaxY();
        }
    }
    
    private void renderMultiblock(GuiGraphics graphics, MultiblockStructure structure, int maxLayer, int x, int y, int width, int height) {
        // Simplified multiblock rendering for EMI
        graphics.pose().pushPose();
        graphics.pose().translate(x + width / 2.0f, y + height / 2.0f, 100);
        float scale = 50.0f;
        graphics.pose().scale(scale, -scale, scale);
        
        graphics.pose().mulPose(new org.joml.Quaternionf().rotationX((float)Math.toRadians(15))); // Tilt down slightly
        graphics.pose().mulPose(new org.joml.Quaternionf().rotationY((float)Math.toRadians(45))); // Rotate 45 degrees for better view
        
        // Optional: Add slow rotation for visual appeal
        long time = System.currentTimeMillis();
        float angle = (time % 20000) / 20000.0f * (float)(Math.PI * 2);
        graphics.pose().mulPose(new org.joml.Quaternionf().rotationY(angle * 0.5f)); // Slower rotation
        
        renderStructureBlocks(graphics.pose(), structure, maxLayer);
        graphics.pose().popPose();
    }
    
    private void renderStructureBlocks(com.mojang.blaze3d.vertex.PoseStack stack, MultiblockStructure structure, int maxLayer) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        
        java.util.Map<BlockPos, BlockState> blocks = structure.getBlocks();
        if (blocks.isEmpty()) return;
        
        // Calculate structure dimensions for scaling
        int structureWidth = structure.getWidth();
        int structureHeight = structure.getHeight();
        int depth = structure.getDepth();
        float scale = 1.0f / Math.max(Math.max(structureWidth, structureHeight), depth);
        
        stack.scale(scale, scale, scale);
        
        // Center the structure
        float centerX = structure.getMinX() + structureWidth / 2.0f;
        float centerY = structure.getMinY() + structureHeight / 2.0f;
        float centerZ = structure.getMinZ() + depth / 2.0f;
        stack.translate(-centerX, -centerY, -centerZ);
        
        net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        
        // Render each block
        for (java.util.Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() > maxLayer) {
                continue;
            }
            BlockState state = entry.getValue();
            
            stack.pushPose();
            stack.translate(pos.getX(), pos.getY(), pos.getZ());
            
            net.neoforged.neoforge.client.model.data.ModelData modelData = net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
            
            blockRenderer.renderSingleBlock(
                    state,
                    stack,
                    bufferSource,
                    15728880,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    modelData,
                    null
            );
            
            stack.popPose();
        }
        
        bufferSource.endBatch();
    }
    
    // Custom widget for handling multiblock rendering and mouse input
    private static class MultiblockRenderWidget extends Widget {
        private final int x, y, width, height;
        private final MultiblockStructureEmiRecipe recipe;
        
        public MultiblockRenderWidget(int x, int y, int width, int height, MultiblockStructureEmiRecipe recipe) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.recipe = recipe;
        }
        
        @Override
        public Bounds getBounds() {
            return new Bounds(x, y, width, height);
        }
        
        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            recipe.renderMultiblock(graphics, recipe.structure, recipe.currentLayer, x, y, width, height);
        }
        
        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            // Right-click to slice layers
            if (button == 1) { // Right mouse button
                recipe.slice();
                return true;
            }
            return false;
        }
    }
}