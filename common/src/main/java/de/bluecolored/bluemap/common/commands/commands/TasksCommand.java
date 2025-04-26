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

import de.bluecolored.bluecommands.annotations.Argument;
import de.bluecolored.bluecommands.annotations.Command;
import de.bluecolored.bluemap.common.commands.Commands;
import de.bluecolored.bluemap.common.commands.Permission;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class TasksCommand {

    private final Plugin plugin;

    @Command("tasks")
    @Permission("bluemap.tasks")
    public Component taskList() {
        Map<RenderTask, Long> completedTasks = plugin.getRenderManager().getCompletedTasks();
        List<RenderTask> scheduledTasks = plugin.getRenderManager().getScheduledRenderTasks();
        RenderTask currentTask = scheduledTasks.isEmpty() ? null : scheduledTasks.removeFirst();

        List<Component> tasks = new LinkedList<>();

        if (completedTasks.size() > 3) tasks.add(text("... more done tasks ...").color(BASE_COLOR));
        completedTasks.entrySet().stream()
                .sorted(Map.Entry.<RenderTask, Long>comparingByValue().reversed())
                .limit(3)
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> format("% % %",
                        ICON_UPDATED,
                        text("(done)").decorate(TextDecoration.ITALIC),
                        entry.getKey().getDescription()
                ).color(BASE_COLOR))
                .forEach(tasks::add);

        if (currentTask != null) {
            tasks.add(format("% % % ...", ICON_IN_PROGRESS, formatTaskRef(currentTask), currentTask.getDescription())
                    .color(HIGHLIGHT_COLOR));
        }

        scheduledTasks.stream()
                .limit(6)
                .map(task -> format("% % %", ICON_PENDING, formatTaskRef(task), task.getDescription())
                        .color(INFO_COLOR))
                .forEach(tasks::add);
        if (scheduledTasks.size() > 6) tasks.add(format("... % more scheduled tasks ...", scheduledTasks.size() - 6)
                .color(BASE_COLOR));

        if (scheduledTasks.isEmpty() && currentTask == null)
            tasks.add(format("% no pending tasks, all done", ICON_UPDATED).color(POSITIVE_COLOR));

        return paragraph("Tasks", lines(tasks));
    }

    @Command("tasks cancel <task-ref>")
    @Permission("bluemap.tasks.cancel")
    public Component cancelTask(@Argument("task-ref") RenderTask renderTask) {
        boolean removed = plugin.getRenderManager().removeRenderTask(renderTask);
        if (!removed)
            return text("Task is not pending or already completed").color(NEGATIVE_COLOR);
        return text("Task cancelled").color(POSITIVE_COLOR);
    }

    private Component formatTaskRef(RenderTask renderTask) {
        return format("[%]", Commands.getRefForTask(renderTask)).color(BASE_COLOR);
    }

}
