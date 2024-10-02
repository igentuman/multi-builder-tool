package igentuman.mbtool.client.handler;

import igentuman.mbtool.client.render.PreviewRenderer;
import igentuman.mbtool.common.item.ItemMultiBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ClientHandler
{
	public static boolean hasRecipe(ItemStack item)
	{
		try {
			return item.getTagCompound().hasKey("recipe");
		} catch (NullPointerException ignored) {
			return false;
		}
	}

	public void renderRays(float partialTicks)
	{
		EntityPlayer player = Minecraft.getMinecraft().player;

		double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
		Vec3d playerPos = new Vec3d(playerX, playerY + player.getEyeHeight(), playerZ);
		Vec3d lookVec = player.getLook(partialTicks);
		Vec3d endPos = playerPos.add(lookVec.scale(20D));
		RayTraceResult rayTraceResult = player.world.rayTraceBlocks(playerPos, endPos);
		if (rayTraceResult != null && rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos blockPos = rayTraceResult.getBlockPos();
			renderRay(playerPos, new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5), Color.RED, partialTicks);
		}
	}

	public void renderRay(Vec3d start, Vec3d end, Color color, float partialTicks) {
		GlStateManager.pushMatrix();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.glLineWidth(4.0F);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		bufferBuilder.pos(start.x, start.y, start.z).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
		bufferBuilder.pos(end.x, end.y, end.z).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
		tessellator.draw();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void renderLast(RenderWorldLastEvent event)
	{
		Minecraft mc = Minecraft.getMinecraft();
		ItemStack mainItem = mc.player.getHeldItemMainhand();
		ItemStack secondItem = mc.player.getHeldItemOffhand();

		boolean main = !mainItem.isEmpty() &&
				mainItem.getItem() instanceof ItemMultiBuilder &&
				hasRecipe(mainItem);
		boolean off = !secondItem.isEmpty() &&
				secondItem.getItem() instanceof ItemMultiBuilder  &&
				hasRecipe(secondItem);

		if(!main && !off) {
			return;
		}
		if(main && ((ItemMultiBuilder)mainItem.getItem()).afterPlaceDelay > 0) return;
		if(off && ((ItemMultiBuilder)secondItem.getItem()).afterPlaceDelay > 0) return;
		renderRays(event.getPartialTicks());
		PreviewRenderer.renderPreview(event.getPartialTicks());
	}
}
