/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.commands;

import de.bluecolored.bluemap.core.map.BmMap;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static net.kyori.adventure.text.Component.text;

@UtilityClass
public class TextFormat {

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\n");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%");

    private static final String DETAILS_ITEM   = "├ ";
    private static final String DETAILS_LINE   = "│ ";
    private static final String DETAILS_END    = "└ ";
    private static final String DETAILS_INDENT = "\u00a0 ";

    private static final TemporalUnit[] DURATION_UNITS = new TemporalUnit[] {
            ChronoUnit.DAYS,
            ChronoUnit.HOURS,
            ChronoUnit.MINUTES,
            ChronoUnit.SECONDS
    };

    public static final TextColor BASE_COLOR = TextColor.color(0xaaaaaa);
    public static final TextColor HIGHLIGHT_COLOR = TextColor.color(0xffffff);
    public static final TextColor TITLE_COLOR = TextColor.color(0x4488ff);
    public static final TextColor POSITIVE_COLOR = TextColor.color(0x88ff88);
    public static final TextColor NEGATIVE_COLOR = TextColor.color(0xff8888);
    public static final TextColor INFO_COLOR = TextColor.color(0xffff88);
    public static final TextColor WARNING_COLOR = TextColor.color(0xff8844);
    public static final TextColor FROZEN_COLOR = TextColor.color(0xaaccff);

    public static final Component ICON_UPDATED = Component.text("✔").color(POSITIVE_COLOR);
    public static final Component ICON_FROZEN = Component.text("❄").color(FROZEN_COLOR);
    public static final Component ICON_PENDING = Component.text("⌛").color(INFO_COLOR);
    public static final Component ICON_IN_PROGRESS = Component.text("⛏").color(INFO_COLOR);
    public static final Component ICON_STOPPED = Component.text("❌").color(NEGATIVE_COLOR);

    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String DISCORD_LINK = "https://discord.gg/zmkyJa3";
    public static final String WIKI_LINK = "https://bluemap.bluecolored.de/wiki/";
    public static final String WIKI_LINK_RENDER_MASKS = "https://bluemap.bluecolored.de/wiki/customization/Masks.html";

    public static Component paragraph(TextColor color, Component title, Component content) {
        return lines(
                format("BlueMap % >", title)
                        .color(color)
                        .decorate(TextDecoration.BOLD),
                indent(content, Component.space())
        );
    }

    public static Component paragraph(String title, Component content) {
        return paragraph(TITLE_COLOR, text(title)
                .color(HIGHLIGHT_COLOR)
                .decoration(TextDecoration.BOLD, false),
                content
        );
    }

    public static Component details(TextColor color, Collection<Component> components) {
        return details(color, components.toArray(Component[]::new));
    }

    public static Component details(TextColor color, Component... components) {
        if (components.length == 0) return Component.empty();

        Component end = Component.text(DETAILS_END, color);
        Component indent = Component.text(DETAILS_INDENT, color);
        if (components.length == 1) return indent(components[0], end, indent);

        Component item = Component.text(DETAILS_ITEM, color);
        Component line = Component.text(DETAILS_LINE, color);
        Component result = Component.empty();
        for (int i = 0; i < components.length - 1; i++) {
            result = result
                    .append(indent(components[i], item, line))
                    .append(Component.newline());
        }

        return result
                .append(indent(components[components.length - 1], end, indent));
    }

    public static Component format(String format, Object... placeholders) {
        return format(Component.text(format), Arrays.asList(placeholders));
    }

    public static Component format(Component format, Object... placeholders) {
        return format(format, Arrays.asList(placeholders));
    }

    public static Component format(Component format, Iterable<Object> placeholders) {
        Iterator<Object> iterator = placeholders.iterator();
        return format.replaceText(TextReplacementConfig.builder()
                .match(PLACEHOLDER_PATTERN)
                .replacement(match -> iterator.hasNext() ? toComponent(iterator.next()) : null)
                .build());
    }

    public static Component item(String key, Object value) {
        return item(key, text(value.toString()).color(HIGHLIGHT_COLOR));
    }

    public static Component item(String key, ComponentLike value) {
        return format("%: %", key, value).color(BASE_COLOR);
    }

    public static Component toComponent(Object object) {
        return switch (object) {
            case ComponentLike cl -> cl.asComponent();
            case null -> Component.text("null");
            default -> Component.text(object.toString());
        };
    }

    public static Component indent(Component component, Component indent) {
        return indent(component, indent, indent);
    }

    public static Component indent(Component component, Component firstIndent, Component indent) {
        Component newlineIndent = Component.newline().append(indent);
        return firstIndent.append(component.replaceText(TextReplacementConfig.builder()
                .match(NEWLINE_PATTERN)
                .replacement(newlineIndent)
                .build()));
    }

    public static Component lines(Iterable<@Nullable Component> lines) {
        return Component.join(
                JoinConfiguration.newlines(),
                StreamSupport.stream(lines.spliterator(), true)
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    public static Component lines(@Nullable Component... lines) {
        return lines(Arrays.asList(lines));
    }

    public static Component command(String command) {
        return text(command)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(format("Click to run %", text(command).color(HIGHLIGHT_COLOR)).color(BASE_COLOR));
    }

    public static int lineCount(Component component) {
        int lines = 1;
        for (Component c : component.iterable(ComponentIteratorType.BREADTH_FIRST)) {
            if (c instanceof TextComponent text && text.content().contains("\n"))
                lines++;
        }
        return lines;
    }

    public static Component[] stripNulls(Component... elements) {
        return Arrays.stream(elements)
                .filter(Objects::nonNull)
                .toArray(Component[]::new);
    }

    public static String duration(Instant since) {
        return duration(Duration.between(since, Instant.now()));
    }

    public static String duration(Duration duration) {
        long durationMillis = duration.toMillis();
        double value = durationMillis;
        TemporalUnit unit = ChronoUnit.MILLIS;

        for (TemporalUnit etaUnit : DURATION_UNITS) {
            unit = etaUnit;
            value = (double) durationMillis / unit.getDuration().toMillis();
            if (value > 1.0) break;
        }

        if (value < 2 && unit != ChronoUnit.SECONDS) return String.format("%.1f %s",
                value,
                unit.toString().toLowerCase(Locale.ROOT)
        );
        return String.format("%.0f %s",
                value,
                unit.toString().toLowerCase(Locale.ROOT)
        );
    }

    public static Component durationFormat(Instant since) {
        LocalDateTime time = LocalDateTime.ofInstant(since, ZoneId.systemDefault());
        return text(duration(since))
                .color(HIGHLIGHT_COLOR)
                .hoverEvent(text(DATE_TIME_FORMAT.format(time)));
    }

    public static Component formatMap(BmMap map) {
        return text(map.getId())
                .hoverEvent(HoverEvent.showText(text(map.getName())));
    }

}
