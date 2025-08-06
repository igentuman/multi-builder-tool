package igentuman.mbtool.util;

import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public class InitMbtoolStructuresEvent extends Event {

    @Override
    public boolean isCancelable() {
        return true;
    }

    public final List<MultiblockStructure> structures;

    public InitMbtoolStructuresEvent(List<MultiblockStructure> structures) {
        this.structures = structures;
    }
}