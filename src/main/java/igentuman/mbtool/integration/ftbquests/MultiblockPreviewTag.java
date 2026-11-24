package igentuman.mbtool.integration.ftbquests;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record MultiblockPreviewTag(String structure, int width, int height, String align, boolean fit, double rotationSpeed) {
    public static final int DEFAULT_SIZE = 180;
    public static final double DEFAULT_ROTATION_SPEED = 18.0;
    public static final double MAX_ROTATION_SPEED = 360.0;

    public MultiblockPreviewTag(String structure, int width, int height, String align, boolean fit) {
        this(structure, width, height, align, fit, DEFAULT_ROTATION_SPEED);
    }

    private static final String PREFIX = "#mbtool:";
    private static final Pattern ID = Pattern.compile("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+");

    public MultiblockPreviewTag {
        if (!Double.isFinite(rotationSpeed) || Math.abs(rotationSpeed) > MAX_ROTATION_SPEED) throw new IllegalArgumentException("Invalid rotation speed");
        if (structure == null || !ID.matcher(structure).matches()) throw new IllegalArgumentException("Invalid structure ID");
        if (width < 1 || width > 1000 || height < 1 || height > 1000) throw new IllegalArgumentException("Invalid preview size");
        if (!"left".equals(align) && !"center".equals(align) && !"right".equals(align)) throw new IllegalArgumentException("Invalid alignment");
    }

    public static Optional<MultiblockPreviewTag> parse(String line) {
        if (line == null || !line.strip().startsWith(PREFIX)) return Optional.empty();
        String[] parts = line.strip().substring(PREFIX.length()).split("\\s+");
        int width = DEFAULT_SIZE, height = DEFAULT_SIZE;
        String align = "center";
        boolean fit = false;
        double rotationSpeed = DEFAULT_ROTATION_SPEED;
        try {
            for (int i = 1; i < parts.length; i++) {
                String[] option = parts[i].split(":", 2);
                if (option.length != 2) return Optional.empty();
                switch (option[0]) {
                    case "width" -> width = Integer.parseInt(option[1]);
                    case "height" -> height = Integer.parseInt(option[1]);
                    case "rotation_speed" -> rotationSpeed = Double.parseDouble(option[1]);
                    case "align" -> align = option[1].toLowerCase(Locale.ROOT);
                    case "fit" -> {
                        if (!option[1].equals("true") && !option[1].equals("false")) return Optional.empty();
                        fit = Boolean.parseBoolean(option[1]);
                    }
                    default -> { return Optional.empty(); }
                }
            }
            return Optional.of(new MultiblockPreviewTag(parts[0], width, height, align, fit, rotationSpeed));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return PREFIX + structure + " width:" + width + " height:" + height + " align:" + align + " fit:" + fit + " rotation_speed:" + rotationSpeed;
    }
}
