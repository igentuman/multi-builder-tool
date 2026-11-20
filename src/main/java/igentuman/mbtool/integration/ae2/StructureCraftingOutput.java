package igentuman.mbtool.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;

final class StructureCraftingOutput {
    private StructureCraftingOutput() {}

    static long deliver(CraftingCpuLogic cpu, AEKey key, long amount) {
        if (amount <= 0 || !key.matches(cpu.getFinalJobOutput())) return 0;
        long waiting = cpu.getWaitingFor(key);
        long offered = Math.min(amount, waiting);
        if (offered <= 0) return 0;

        cpu.insert(key, offered, Actionable.MODULATE);
        return Math.min(offered, Math.max(0, waiting - cpu.getWaitingFor(key)));
    }

    static void removeStoredTokens(IGrid grid, AEItemKey token) {
        var service = grid.getStorageService();
        var stored = service.getCachedInventory().findFuzzy(token, FuzzyMode.IGNORE_ALL).stream()
                .filter(entry -> entry.getLongValue() > 0)
                .map(entry -> new GenericStack(entry.getKey(), entry.getLongValue()))
                .toList();
        for (GenericStack stack : stored) {
            service.getInventory().extract(stack.what(), stack.amount(), Actionable.MODULATE, IActionSource.empty());
        }
    }
}
