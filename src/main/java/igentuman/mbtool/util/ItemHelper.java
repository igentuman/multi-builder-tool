package igentuman.mbtool.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

public class ItemHelper {
    public static ItemStack getStackFromString(String name,int meta)
    {
       return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(name)),1, meta);
    }

    public static boolean doStacksMatch(ItemStack stack1, ItemStack stack2)
    {
        if (stack1.isEmpty())
            return stack2.isEmpty();
        if (stack2.isEmpty())
            return false;
        if (stack1.getItem() != stack2.getItem())
            return false;
        if (stack1.getMetadata() == OreDictionary.WILDCARD_VALUE || stack2.getMetadata() == OreDictionary.WILDCARD_VALUE)
            return true;
        return stack1.getMetadata() == stack2.getMetadata();
    }

    public static ItemStack consumeItem(ItemStack stack, int amount)
    {
        if (stack.getCount() > amount)
        {
            stack.shrink(amount);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack consumeItem(EntityPlayer player, ItemStack stack)
    {
        return player.isCreative() ? stack : consumeItem(stack, 1);
    }

    public static ItemStack consumeItem(EntityPlayer player, ItemStack stack, int amount)
    {
        return player.isCreative() ? stack : consumeItem(stack, amount);
    }
}
