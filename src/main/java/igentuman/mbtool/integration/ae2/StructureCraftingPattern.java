package igentuman.mbtool.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

final class StructureCraftingPattern implements IPatternDetails {
    private final AEItemKey definition;
    private final List<GenericStack> outputs;
    private final IInput[] inputs;

    StructureCraftingPattern(AEItemKey definition, AEItemKey result, List<Input> inputs) {
        this.definition = definition;
        this.outputs = List.of(new GenericStack(result, 1));
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
    public List<GenericStack> getOutputs() {
        return outputs;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StructureCraftingPattern pattern && pattern.definition.equals(definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    static final class Input implements IInput {
        private final GenericStack[] candidates;
        private final long amount;

        Input(List<AEItemKey> candidates, long amount) {
            this.candidates = candidates.stream().map(key -> new GenericStack(key, 1)).toArray(GenericStack[]::new);
            this.amount = amount;
        }

        List<AEItemKey> keys() {
            return Arrays.stream(candidates).map(stack -> (AEItemKey) stack.what()).toList();
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
