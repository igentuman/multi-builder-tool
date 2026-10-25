package igentuman.mbtool.integration.jei;

import giselle.jei_mekanism_multiblocks.client.JEI_MekanismMultiblocks_Client;

import java.lang.reflect.Field;

public final class JemmMouseState {

    private static final Field PRESSED_FIELD = findPressedField();

    private JemmMouseState() {
    }

    public static boolean isPressed() {
        try {
            return PRESSED_FIELD.getBoolean(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read JEI Mekanism Multiblocks mouse state", e);
        }
    }

    private static Field findPressedField() {
        Class<?> clientClass = JEI_MekanismMultiblocks_Client.class;
        try {
            return clientClass.getField("MOUSE_PRESSED");
        } catch (NoSuchFieldException e) {
            try {
                return clientClass.getField("PRESSED");
            } catch (NoSuchFieldException oldVersionMissing) {
                throw new IllegalStateException("JEI Mekanism Multiblocks has no known mouse state field", oldVersionMissing);
            }
        }
    }
}
