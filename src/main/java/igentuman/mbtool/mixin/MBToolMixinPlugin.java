package igentuman.mbtool.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MBToolMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // Called when the mixin config is loaded
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".ftbquests.")) {
            return isModPresent("ftbquests") && isModPresent("ftblibrary");
        }
        if (mixinClassName.endsWith(".CraftingCPUClusterMixin")
                || mixinClassName.endsWith(".CraftingCpuLogicMixin")) {
            return isModPresent("ae2");
        }
        // Check if JEMM is loaded before applying MultiblockWidgetMixin
        /*if (mixinClassName.equals("igentuman.mbtool.mixin.MultiblockWidgetMixin")) {
            return ModUtil.isJEMMLoaded();
        }*/
        
        // Apply all other mixins by default
        return true;
    }

    /** ModList is not ready during mixin selection; consult the loading mod list instead. */
    private static boolean isModPresent(String modId) {
        try {
            return LoadingModList.get().getModFileById(modId) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // Called to accept target classes
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Called before mixin is applied
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Called after mixin is applied
    }
}