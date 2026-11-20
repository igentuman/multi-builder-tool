package igentuman.mbtool.mixin;

import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import igentuman.mbtool.integration.ae2.CraftingJobTracker;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingCpuLogic.class, remap = false)
public abstract class CraftingCpuLogicMixin {
    @Shadow @Final
    private CraftingCPUCluster cluster;

    @Inject(method = "finishJob", at = @At("HEAD"), remap = false)
    private void mbtool$recordFinished(boolean successful, CallbackInfo callback) {
        if (cluster.getLevel() instanceof ServerLevel level) {
            CraftingJobTracker.recordFinished(level.getServer(), cluster, successful);
        }
    }
}
