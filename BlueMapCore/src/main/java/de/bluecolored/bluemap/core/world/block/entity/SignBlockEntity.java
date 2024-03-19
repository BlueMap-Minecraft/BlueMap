package de.bluecolored.bluemap.core.world.block.entity;

import java.util.List;
import java.util.Map;

public class SignBlockEntity extends BlockEntity {
    private final TextData frontText;
    private final TextData backText;

    @SuppressWarnings("unchecked")
    protected SignBlockEntity(Map<String, Object> data) {
        super(data);

        // Versions before 1.20 used a different format
        if (data.containsKey("front_text")) {
            this.frontText = new TextData((Map<String, Object>) data.getOrDefault("front_text", Map.of()));
            this.backText = new TextData((Map<String, Object>) data.getOrDefault("back_text", Map.of()));
        } else {
            this.frontText = new TextData(
                    (byte) data.getOrDefault("GlowingText", (byte) 0) == 1,
                    (String) data.getOrDefault("Color", ""),
                    List.of(
                            (String) data.getOrDefault("Text1", ""),
                            (String) data.getOrDefault("Text2", ""),
                            (String) data.getOrDefault("Text3", ""),
                            (String) data.getOrDefault("Text4", "")
                    )
            );

            this.backText = new TextData(false, "", List.of());
        }
    }

    public TextData getFrontText() {
        return frontText;
    }

    public TextData getBackText() {
        return backText;
    }

    @Override
    public String toString() {
        return "SignBlockEntity{" +
                "frontText=" + frontText +
                ", backText=" + backText +
                "} " + super.toString();
    }

    public static class TextData {
        private final boolean hasGlowingText;
        private final String color;
        private final List<String> messages;

        @SuppressWarnings("unchecked")
        private TextData(Map<String, Object> data) {
            this.hasGlowingText = (byte) data.getOrDefault("has_glowing_text", (byte) 0) == 1;
            this.color = (String) data.getOrDefault("color", "");
            this.messages = (List<String>) data.getOrDefault("messages", List.of());
        }

        public TextData(boolean hasGlowingText, String color, List<String> messages) {
            this.hasGlowingText = hasGlowingText;
            this.color = color;
            this.messages = messages;
        }

        public boolean isHasGlowingText() {
            return hasGlowingText;
        }

        public String getColor() {
            return color;
        }

        public List<String> getMessages() {
            return messages;
        }

        @Override
        public String toString() {
            return "TextData{" +
                    "hasGlowingText=" + hasGlowingText +
                    ", color='" + color + '\'' +
                    ", messages=" + messages +
                    '}';
        }
    }
}
