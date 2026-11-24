package igentuman.mbtool.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import igentuman.mbtool.integration.ae2.StructureCraftingManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class CraftingCPUClusterMixin {
    @Inject(method = "submitJob", at = @At("HEAD"), cancellable = true, remap = false)
    private void mbtool$rejectDuplicate(IGrid grid, ICraftingPlan plan, IActionSource source,
                                       ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> callback) {
        CraftingCPUCluster cpu = (CraftingCPUCluster) (Object) this;
        if (cpu.getLevel() instanceof ServerLevel level && !StructureCraftingManager.canSubmit(level.getServer(), plan)) {
            callback.setReturnValue(CraftingSubmitResult.INCOMPLETE_PLAN);
        }
    }

    @Inject(method = "submitJob", at = @At("RETURN"), remap = false)
    private void mbtool$recordSubmitted(IGrid grid, ICraftingPlan plan, IActionSource source,
                                       ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> callback) {
        CraftingCPUCluster cpu = (CraftingCPUCluster) (Object) this;
        if (cpu.getLevel() instanceof ServerLevel level) {
            StructureCraftingManager.recordSubmitted(level.getServer(), plan, callback.getReturnValue(), (ICraftingCPU) cpu);
        }
    }
}
