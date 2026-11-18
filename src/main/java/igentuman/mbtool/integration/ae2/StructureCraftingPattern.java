package igentuman.mbtool.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.level.Level;

import java.util.List;

final class StructureCraftingPattern implements IPatternDetails {
    private final AEItemKey definition;
    private final GenericStack output;
    private final IInput[] inputs;

    StructureCraftingPattern(AEItemKey definition, AEItemKey result, List<Input> inputs) {
        this.definition = definition;
        this.output = new GenericStack(result, 1);
        this.inputs = inputs.toArray(IInput[]::new);
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return new GenericStack[]{output};
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StructureCraftingPattern pattern && pattern.definition.equals(definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    /** One block type. The first candidate is the exact block; the rest are configured equivalents. */
    static final class Input implements IInput {
        private final GenericStack[] candidates;
        private final long amount;

        Input(List<AEItemKey> candidates, long amount) {
            // AE2 convention: one unit per candidate template, the count lives in the multiplier.
            this.candidates = candidates.stream().map(key -> new GenericStack(key, 1)).toArray(GenericStack[]::new);
            this.amount = amount;
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return candidates;
        }

        @Override
        public long getMultiplier() {
            return amount;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            for (GenericStack candidate : candidates) {
                if (input.matches(candidate)) return true;
            }
            return false;
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
