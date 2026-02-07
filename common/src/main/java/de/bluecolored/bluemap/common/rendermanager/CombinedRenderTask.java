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
package de.bluecolored.bluemap.common.rendermanager;

import java.util.*;

public class CombinedRenderTask<T extends RenderTask> implements RenderTask {

    private final String description;
    protected final List<T> tasks;
    private int currentTaskIndex;

    public CombinedRenderTask(String description, Collection<T> tasks) {
        this.description = description;
        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));

        this.currentTaskIndex = 0;
    }

    /**
     * Returns the immutable list of combined subtasks.
     */
    public List<T> getTasks() {
        return tasks;
    }

    @Override
    public void doWork() throws Exception {
        T task;

        synchronized (this) {
            if (!hasMoreWork())
                return;
            task = this.tasks.get(this.currentTaskIndex);

            if (!task.hasMoreWork()) {
                this.currentTaskIndex++;
                return;
            }
        }

        task.doWork();
    }

    @Override
    public synchronized boolean hasMoreWork() {
        return this.currentTaskIndex < this.tasks.size();
    }

    @Override
    public double estimateProgress() {
        int currentTask = this.currentTaskIndex;
        if (currentTask >= this.tasks.size())
            return 1;

        double total = currentTask;
        total += this.tasks.get(currentTask).estimateProgress();

        return total / tasks.size();
    }

    @Override
    public void cancel() {
        for (T task : tasks)
            task.cancel();
    }

    @Override
    public boolean contains(RenderTask task) {
        if (this.equals(task))
            return true;

        if (task instanceof CombinedRenderTask<?> combinedTask) {
            for (RenderTask subTask : combinedTask.tasks) {
                if (!this.contains(subTask))
                    return false;
            }
            return true;
        }

        for (RenderTask subTask : this.tasks) {
            if (subTask.contains(task))
                return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Optional<String> getDetail() {
        if (this.currentTaskIndex >= this.tasks.size())
            return Optional.empty();
        return Optional.ofNullable(this.tasks.get(this.currentTaskIndex).getDescription());
    }

}
