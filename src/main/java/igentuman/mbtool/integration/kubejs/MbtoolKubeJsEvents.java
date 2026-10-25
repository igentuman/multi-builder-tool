package igentuman.mbtool.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import igentuman.mbtool.util.InitMbtoolStructuresEvent;
import igentuman.mbtool.util.MultiblockStructure;

import java.util.List;

public class MbtoolKubeJsEvents {
    public static final EventGroup GROUP = EventGroup.of("MbtoolKJSEvents");
    public static final EventHandler ON_LOAD_STRUCTURES = GROUP.server("InitMbtoolStructures", () -> InitMbtoolStructures.class);

    public static void onInitMbtoolStructures(InitMbtoolStructuresEvent event) {
        ON_LOAD_STRUCTURES.post(ScriptType.SERVER, new InitMbtoolStructures(event.structures));
    }

    public static class InitMbtoolStructures implements KubeEvent {

        /** Mutable structures for this reload, shared with the native event. */
        public final List<MultiblockStructure> structures;

        public InitMbtoolStructures(List<MultiblockStructure> structures) {
            this.structures = structures;
        }
    }
}
