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
package de.bluecolored.bluemap.common.commands.commands;

import de.bluecolored.bluecommands.annotations.Command;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.commands.Permission;
import de.bluecolored.bluemap.common.commands.TextFormat;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.*;
import de.bluecolored.bluemap.core.map.BmMap;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class StatusCommand {

    private final Plugin plugin;

    @Command("")
    @Permission("bluemap.status")
    public Component status() {
        Status status = new Status();
        return status.status();
    }

    private class Status {

        private final Map<String, BmMap> maps;

        private final List<RenderTask> renderQueue;
        private final @Nullable RenderTask currentTask;
        private final long currentTaskEta;
        private final long lastTimeBusy;

        private final boolean isRunning, isPaused, isProcessing;
        private final int workerThreadCount, pausePlayerCount;

        public Status() {
            BlueMapService blueMapService = plugin.getBlueMap();
            RenderManager renderManager = plugin.getRenderManager();

            this.maps = blueMapService.getMaps();

            this.renderQueue = renderManager.getScheduledRenderTasks();
            this.currentTask = renderManager.getCurrentRenderTask();
            this.currentTaskEta = renderManager.estimateCurrentRenderTaskTimeRemaining();
            this.lastTimeBusy = renderManager.getLastTimeBusy();

            this.isRunning = plugin.getPluginState().isRenderThreadsEnabled();
            this.isPaused = plugin.checkPausedByPlayerCount();
            this.isProcessing = isRunning && !isPaused && currentTask != null;

            this.workerThreadCount = renderManager.getWorkerThreadCount();
            this.pausePlayerCount = blueMapService.getConfig().getPluginConfig().getPlayerRenderLimit();
        }

        public Component status() {
            return paragraph("Status",
                    lines(
                            renderThreads(),
                            isProcessing ? activeTask() : null,
                            mapSummary(isProcessing)
                    )
            );
        }

        private Component renderThreads() {
            if (!isRunning) {
                return lines(
                        format("❌ render-threads are %",
                                text("stopped").color(HIGHLIGHT_COLOR)
                        ).color(NEGATIVE_COLOR),
                        details(BASE_COLOR, format(text("use % to start rendering"),
                                command("/bluemap start").color(HIGHLIGHT_COLOR)
                        ))
                );
            }

            if (isPaused) {
                return lines(
                        format("⌛ render-threads are %",
                                text("paused").color(HIGHLIGHT_COLOR)
                        ).color(INFO_COLOR),
                        details(BASE_COLOR, format(text("there are % or more players online"),
                                text(pausePlayerCount).color(HIGHLIGHT_COLOR)
                        ))
                );
            }

            if (!isProcessing && lastTimeBusy > 1000) {
                return lines(
                        format(workerThreadCount == 1 ?
                                        "✔ % render-thread is %" :
                                        "✔ % render-threads are %",
                                text(workerThreadCount).color(HIGHLIGHT_COLOR),
                                text("idle").color(HIGHLIGHT_COLOR)
                        ).color(POSITIVE_COLOR),
                        details(BASE_COLOR, format(text("last active % ago"),
                                durationFormat(Instant.ofEpochMilli(lastTimeBusy))
                        ))
                );
            }

            return lines(
                    format(workerThreadCount == 1 ?
                                    "✔ % render-thread is %" :
                                    "✔ % render-threads are %",
                            text(workerThreadCount).color(HIGHLIGHT_COLOR),
                            text(isProcessing ? "running" : "idle").color(HIGHLIGHT_COLOR)
                    ).color(POSITIVE_COLOR)
            );
        }

        private @Nullable Component activeTask() {
            if (currentTask == null) return null;

            Component infoLine = ( switch (currentTask) {
                case MapUpdateTask t -> format("⛏ map % is currently being updated",
                        formatMap(t.getMap()).color(HIGHLIGHT_COLOR)
                ).color(INFO_COLOR);
                case WorldRegionRenderTask t -> format("⛏ map % is currently being updated",
                        formatMap(t.getMap()).color(HIGHLIGHT_COLOR)
                ).color(INFO_COLOR);
                case MapPurgeTask t -> format("⛏ map % is currently being purged",
                        formatMap(t.getMap()).color(HIGHLIGHT_COLOR)
                ).color(INFO_COLOR);
                default -> format("⛏ currently running: %",
                        text(currentTask.getDescription()).color(HIGHLIGHT_COLOR)
                ).color(INFO_COLOR);
            } ).hoverEvent( HoverEvent.showText(text(currentTask.getDescription())) );

            return lines(
                    empty(),
                    infoLine,
                    details(BASE_COLOR, stripNulls(
                            format("progress: %",
                                    text(String.format("%.3f%%", currentTask.estimateProgress() * 100)).color(HIGHLIGHT_COLOR)
                            ),
                            taskETA(),
                            currentTask.getDetail()
                                    .map(Component::text)
                                    .orElse(null)
                    )),
                    empty()
            );

        }

        private @Nullable Component taskETA() {
            if (currentTask == null) return null;
            if (currentTaskEta == 0) return null;

            double progress = currentTask.estimateProgress();
            if (progress < 0.001) return null;

            String duration = duration(Duration.ofMillis(currentTaskEta));
            return format("remaining time: %",
                    text(duration).color(HIGHLIGHT_COLOR)
            );
        }

        private Component mapSummary(boolean excludeInProgress) {
            Set<BmMap> mapsUpdated = new HashSet<>(maps.values());
            Set<BmMap> mapsPending = new HashSet<>();
            Set<BmMap> mapsFrozen = new HashSet<>();

            // find pending updates
            for (RenderTask renderTask : renderQueue) {
                if (renderTask instanceof MapRenderTask mapTask) {
                    mapsPending.add(mapTask.getMap());
                    mapsUpdated.remove(mapTask.getMap());
                }
            }

            // find frozen maps
            for (BmMap map : mapsUpdated) {
                if (!plugin.getPluginState().getMapState(map).isUpdateEnabled()) {
                    mapsFrozen.add(map);
                }
            }
            mapsUpdated.removeAll(mapsFrozen);

            // exclude currently in progress
            if (excludeInProgress) {
                if (currentTask instanceof MapRenderTask mapTask) {
                    mapsPending.remove(mapTask.getMap());
                    mapsUpdated.remove(mapTask.getMap());
                    mapsFrozen.remove(mapTask.getMap());
                }
            }

            // format and return response
            List<Component> lines = new ArrayList<>();
            if (!mapsPending.isEmpty()) {
                lines.add(formatMapSummary(mapsPending,
                        "⌛ map % has pending updates",
                        "⌛ % maps have pending updates"
                ).color(INFO_COLOR));
            }
            if (!mapsUpdated.isEmpty()) {
                lines.add(formatMapSummary(mapsUpdated,
                        "✔ map % is updated",
                        "✔ % maps are updated"
                ).color(POSITIVE_COLOR));
            }
            if (!mapsFrozen.isEmpty()) {
                lines.add(formatMapSummary(mapsFrozen,
                        "❄ map % is frozen",
                        "❄ % maps are frozen"
                ).color(FROZEN_COLOR));
            }
            return lines(lines);
        }

        private Component formatMapSummary(Collection<BmMap> maps, String single, String multiple) {
            if (maps.size() == 1) {
                return format(single, formatMap(maps.iterator().next()).color(HIGHLIGHT_COLOR));
            } else if (maps.size() <= 10) {
                return format(multiple, text(maps.size())
                        .color(HIGHLIGHT_COLOR)
                        .hoverEvent(HoverEvent.showText(
                                Component.join(
                                        JoinConfiguration.separator(text(", ").color(BASE_COLOR)),
                                        maps.stream()
                                                .map(BmMap::getId)
                                                .map(id -> text(id).color(HIGHLIGHT_COLOR))
                                                .toArray(Component[]::new)
                                )
                        )));
            } else {
                return format(multiple, text(maps.size())
                        .color(HIGHLIGHT_COLOR)
                        .hoverEvent(HoverEvent.showText(
                                Component.join(
                                        JoinConfiguration.separator(text(", ").color(BASE_COLOR)),
                                        maps.stream()
                                                .limit(10)
                                                .map(BmMap::getId)
                                                .map(id -> text(id).color(HIGHLIGHT_COLOR))
                                                .toArray(Component[]::new)
                                ).append(text("...").color(BASE_COLOR))
                        )));
            }
        }

    }

}
