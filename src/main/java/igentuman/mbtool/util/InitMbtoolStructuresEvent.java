package igentuman.mbtool.util;

import net.neoforged.bus.api.Event;

import java.util.List;

public class InitMbtoolStructuresEvent extends Event {

    public boolean isCanceled = false;
    public final List<MultiblockStructure> structures;

    public InitMbtoolStructuresEvent(List<MultiblockStructure> structures) {
        this.structures = structures;
    }
}