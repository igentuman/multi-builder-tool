package igentuman.mbtool;

import net.minecraftforge.common.config.Config;

@Config(modid = Mbtool.MODID)
public class ModConfig {
    public static GeneralSettings general = new GeneralSettings();

    public static class GeneralSettings {

        @Config.Name("energy_per_block")
        @Config.Comment({
                "How much energy cost to place one block (RF)"
        })
        public int energy_per_block = 10;

        @Config.Name("mbtool_energy_capacity")
        @Config.Comment({
                "RF"
        })
        public int mbtool_energy_capacity = 10000;

        @Config.Name("xp_per_block")
        @Config.Comment({
                "How much xp cost to place one block"
        })
        public int xp_per_block = 1;

        @Config.Name("saturation_per_building")
        @Config.Comment({
                "How much saturation player loose per building"
        })
        public int saturation_per_building = 1;
    }
}
