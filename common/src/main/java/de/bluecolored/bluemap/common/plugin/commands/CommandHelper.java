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
package de.bluecolored.bluemap.common.plugin.commands;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import de.bluecolored.bluemap.common.plugin.text.TextFormat;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CommandHelper {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(Locale.ROOT)
                    .withZone(ZoneId.systemDefault());

    private final Plugin plugin;
    private final Map<String, WeakReference<RenderTask>> taskRefMap;

    public CommandHelper(Plugin plugin) {
        this.plugin = plugin;
        this.taskRefMap = new HashMap<>();
    }

    public List<Text> createStatusMessage(){
        List<Text> lines = new ArrayList<>();

        RenderManager renderer = plugin.getRenderManager();
        List<RenderTask> tasks = renderer.getScheduledRenderTasks();

        lines.add(Text.of(TextColor.BLUE, "BlueMap - Status:"));

        if (renderer.isRunning()) {
            Text status;
            if (tasks.isEmpty()) {
                status = Text.of(TextColor.GRAY, "idle");
            } else {
                status = Text.of(TextColor.GREEN, "running");
            }

            status.setHoverText(Text.of("click to stop rendering"));
            status.setClickAction(Text.ClickAction.RUN_COMMAND, "/bluemap stop");

            lines.add(Text.of(TextColor.WHITE, " Render-Threads are ", status, TextColor.WHITE, "!"));

            if (tasks.isEmpty()) {
                lines.add(Text.of(TextColor.GRAY, " Last time running: ", TextColor.DARK_GRAY, formatTime(renderer.getLastTimeBusy())));
            } else {
                lines.add(Text.of(TextColor.WHITE, " Queued Tasks (" + tasks.size() + "):"));
                for (int i = 0; i < tasks.size(); i++) {
                    if (i >= 10){
                        lines.add(Text.of(TextColor.GRAY, "..."));
                        break;
                    }

                    RenderTask task = tasks.get(i);
                    lines.add(Text.of(TextColor.GRAY, "\u00A0\u00A0[" + getRefForTask(task) + "] ", TextColor.GOLD, task.getDescription()));

                    if (i == 0) {
                        task.getDetail().ifPresent(detail ->
                                lines.add(Text.of(TextColor.GRAY, "\u00A0\u00A0\u00A0Detail: ", TextColor.WHITE, detail)));

                        lines.add(Text.of(TextColor.GRAY, "\u00A0\u00A0\u00A0Progress: ", TextColor.WHITE,
                                (Math.round(task.estimateProgress() * 10000) / 100.0) + "%"));

                        long etaMs = renderer.estimateCurrentRenderTaskTimeRemaining();
                        if (etaMs > 0) {
                            Duration eta = Duration.of(etaMs, ChronoUnit.MILLIS);
                            String etaString = "%d:%02d:%02d".formatted(
                                    eta.toHours(),
                                    eta.toMinutesPart(),
                                    eta.toSecondsPart()
                            );
                            lines.add(Text.of(TextColor.GRAY, "\u00A0\u00A0\u00A0ETA: ", TextColor.WHITE, etaString));
                        }
                    }
                }
            }
        } else {
            if (plugin.checkPausedByPlayerCount()) {
                lines.add(Text.of(TextColor.WHITE, " Render-Threads are ",
                        Text.of(TextColor.GOLD, "paused")));
                lines.add(Text.of(TextColor.GRAY, TextFormat.ITALIC, "\u00A0\u00A0\u00A0(there are " + plugin.getBlueMap().getConfig().getPluginConfig().getPlayerRenderLimit() + " or more players online)"));
            } else {
                lines.add(Text.of(TextColor.WHITE, " Render-Threads are ",
                        Text.of(TextColor.RED, "stopped")
                                .setHoverText(Text.of("click to start rendering"))
                                .setClickAction(Text.ClickAction.RUN_COMMAND, "/bluemap start"),
                        TextColor.GRAY, "!"));
            }

            if (!tasks.isEmpty()) {
                lines.add(Text.of(TextColor.WHITE, " Queued Tasks (" + tasks.size() + "):"));
                for (int i = 0; i < tasks.size(); i++) {
                    if (i >= 10){
                        lines.add(Text.of(TextColor.GRAY, "..."));
                        break;
                    }

                    RenderTask task = tasks.get(i);
                    lines.add(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, task.getDescription()));
                }
            }
        }

        return lines;
    }

    public Text worldHelperHover() {
        StringJoiner joiner = new StringJoiner("\n");
        for (String worldId : plugin.getBlueMap().getWorlds().keySet()) {
            joiner.add(worldId);
        }

        return Text.of(TextFormat.UNDERLINED, "world")
                .setHoverText(Text.of(TextColor.WHITE, "Available worlds: \n", TextColor.GRAY, joiner.toString()));
    }

    public Text mapHelperHover() {
        StringJoiner joiner = new StringJoiner("\n");
        for (String mapId : plugin.getBlueMap().getMaps().keySet()) {
            joiner.add(mapId);
        }

        return Text.of(TextFormat.UNDERLINED, "map")
                .setHoverText(Text.of(TextColor.WHITE, "Available maps: \n", TextColor.GRAY, joiner.toString()));
    }

    public synchronized Optional<RenderTask> getTaskForRef(String ref) {
        return Optional.ofNullable(taskRefMap.get(ref)).map(WeakReference::get);
    }

    public synchronized Collection<String> getTaskRefs() {
        return new ArrayList<>(taskRefMap.keySet());
    }

    private synchronized String getRefForTask(RenderTask task) {
        Iterator<Map.Entry<String, WeakReference<RenderTask>>> iterator = taskRefMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, WeakReference<RenderTask>> entry = iterator.next();
            if (entry.getValue().get() == null) iterator.remove();
            if (entry.getValue().get() == task) return entry.getKey();
        }

        String newRef = safeRandomRef();

        taskRefMap.put(newRef, new WeakReference<>(task));
        return newRef;
    }

    private synchronized String safeRandomRef() {
        String ref = randomRef();
        while (taskRefMap.containsKey(ref)) ref = randomRef();
        return ref;
    }

    private String randomRef() {
        StringBuilder ref = new StringBuilder(Integer.toString(Math.abs(new Random().nextInt()), 16));
        while (ref.length() < 4) ref.insert(0, "0");
        return ref.subSequence(0, 4).toString();
    }

    public String formatTime(long timestamp) {
        if (timestamp < 0) return "-";
        return TIME_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

}
