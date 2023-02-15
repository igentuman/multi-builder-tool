package igentuman.mbtool.common.container;

import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import org.jetbrains.annotations.NotNull;

public class ContainerMbtool extends Container {
    public EntityPlayer player;
    public ContainerMbtool(EntityPlayer player) {
        this.player = player;
        mbtool = player.getHeldItem(EnumHand.MAIN_HAND);
    }
    public ItemStack mbtool;

    public int getEnergyStored()
    {
        return 10;
    }

    public void setCurrentRecipe(int id)
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("recipe", id);
        try {
            MultiblockRecipe recipe = MultiblockRecipes.getAvaliableRecipes().get(id);
            if(!recipe.allowRotate) {
                nbt.setInteger("rotate",0);
            }
        } catch (NullPointerException ignored) {

        }
        mbtool.setTagCompound(nbt);
    }

    public int getCurrentRecipe()
    {
        int recipeId = 0;
        if(mbtool.getTagCompound() == null) return recipeId;
        recipeId = mbtool.getTagCompound().getInteger("recipe");
        return recipeId;
    }

    public String getCurrentRecipeName()
    {
       return MultiblockRecipes.getAvaliableRecipes().get(getCurrentRecipe()).getName();
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer playerIn) {
        return true;
    }

}
