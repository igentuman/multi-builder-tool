package igentuman.mbtool.handler;

import igentuman.mbtool.recipe.MultiblockRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PreviewRenderBlockEvent extends Event
{
	private ItemStack stack;
	private World world;
	private int index;
	private MultiblockRecipe multiblock;
	private int rotate;
	private int l;
	private int h;
	private int w;

	public PreviewRenderBlockEvent(MultiblockRecipe multiblock, int index, ItemStack stack, World world, int rotate, int l, int h, int w)
	{
		super();
		this.stack = stack;
		this.world = world;
		this.multiblock = multiblock;
		this.index = index;
		this.rotate = rotate;
		this.l = l;
		this.h = h;
		this.w = w;
	}

	public World getWorld()
	{
		return world;
	}

	public ItemStack getItemStack()
	{
		return stack;
	}

	public int getIndex()
	{
		return index;
	}

	public MultiblockRecipe getMultiblock()
	{
		return multiblock;
	}

	public void setItemStack(ItemStack itemStack)
	{
		this.stack = itemStack;
	}

	public EnumFacing getRotate()
	{
		switch (rotate)
		{
			case 0:
				return EnumFacing.EAST;
			case 1:
				return EnumFacing.NORTH;
			case 2:
				return EnumFacing.WEST;
			default:
				return EnumFacing.SOUTH;
		}
	}


	public int getL()
	{
		return l;
	}

	public int getH()
	{
		return h;
	}

	public int getW()
	{
		return w;
	}
}
