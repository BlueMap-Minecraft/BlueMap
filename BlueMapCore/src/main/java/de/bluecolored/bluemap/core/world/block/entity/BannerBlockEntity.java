package de.bluecolored.bluemap.core.world.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BannerBlockEntity extends BlockEntity {
    private final List<Pattern> patterns = new ArrayList<>();

    private BannerBlockEntity(Map<String, Object> data) {
        super(data);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> patterns = (List<Map<String, Object>>) data.getOrDefault("Patterns", List.of());

        for (Map<String, Object> compound : patterns) {
            this.patterns.add(new Pattern(compound));
        }
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    @Override
    public String toString() {
        return "BannerBlockEntity{" +
                "patterns=" + patterns +
                "} " + super.toString();
    }

    public static class Pattern {
        private final String code;
        private final Color color;

        private Pattern(Map<String, Object> data) {
            this.code = (String) data.get("Pattern");
            this.color = Color.values()[(int) data.get("Color")];
        }

        public String getCode() {
            return code;
        }

        public Color getColor() {
            return color;
        }

        @Override
        public String toString() {
            return "Pattern{" +
                    "code='" + code + '\'' +
                    ", color=" + color +
                    '}';
        }
    }

    public enum Color {
        WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY, LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN,
        RED, BLACK
    }
}
