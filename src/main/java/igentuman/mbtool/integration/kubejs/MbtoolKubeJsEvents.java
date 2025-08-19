package igentuman.mbtool.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.script.ScriptType;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.InitMbtoolStructuresEvent;

import java.util.List;

public class MbtoolKubeJsEvents {
    public static final EventGroup GROUP = EventGroup.of("MbtoolKJSEvents");
    public static final EventHandler ON_LOAD_STRUCTURES = GROUP.server("InitMbtoolStructures", () -> InitMbtoolStructures.class);

    public static void onInitMbtoolStructures(InitMbtoolStructuresEvent event) {
        InitMbtoolStructures evenjs = new InitMbtoolStructures(event.structures);
        EventResult result = ON_LOAD_STRUCTURES.post(ScriptType.SERVER, evenjs);
        event.isCanceled = result.interruptDefault() || result.interruptFalse() || result.interruptTrue();
    }

    public static class InitMbtoolStructures implements KubeEvent {

        public List<MultiblockStructure> structures;

        public InitMbtoolStructures(List<MultiblockStructure> structures) {
            this.structures = structures;
        }
    }
}
