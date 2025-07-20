package igentuman.mbtool.util;

import com.gregtechceu.gtceu.api.capability.compat.FeCompat;


public class GTUtils {

    public static String formatEUEnergy(int energy)
    {
        energy = energy / FE2EURatio();
        if(energy >= 1000000) {
            return TextUtils.numberFormat(energy/1000000d)+" MEU";
        }
        if(energy >= 1000) {
            return TextUtils.numberFormat(energy/1000d)+" kEU";
        }
        return TextUtils.numberFormat(energy)+" EU";
    }

    public static int convert2FE(long eu) {
        long converted = eu * FE2EURatio();
        if(converted > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if(converted < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) converted;
    }
    public static int convert2EU(int fe) {
        return (int) (fe / FE2EURatio());
    }

    public static int FE2EURatio() {
        return FeCompat.ratio(true);
    }

    public static int EU2FERatio() {
        return FeCompat.ratio(false);
    }

}
