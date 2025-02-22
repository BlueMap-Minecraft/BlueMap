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

import de.bluecolored.bluemap.core.logger.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class RenderManager {
    private static final AtomicInteger nextRenderManagerIndex = new AtomicInteger(0);

    private final int id;
    private volatile boolean running;

    private long lastTimeBusy;

    private final AtomicInteger nextWorkerThreadIndex;
    private final Collection<WorkerThread> workerThreads;
    private final AtomicInteger busyCount;

    private ProgressTracker progressTracker;
    private volatile boolean newTask;

    private final LinkedList<RenderTask> renderTasks;
    private final Map<RenderTask, Long> completedTasks;

    public RenderManager() {
        this.id = nextRenderManagerIndex.getAndIncrement();
        this.nextWorkerThreadIndex = new AtomicInteger(0);

        this.running = false;
        this.workerThreads = new ConcurrentLinkedDeque<>();
        this.busyCount = new AtomicInteger(0);

        this.lastTimeBusy = -1;

        this.progressTracker = null;
        this.newTask = true;

        this.renderTasks = new LinkedList<>();
        this.completedTasks = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<RenderTask, Long> eldest) {
                return size() > 10;
            }
        };
    }

    public void start(int threadCount) throws IllegalStateException {
        if (threadCount <= 0) throw new IllegalArgumentException("threadCount has to be 1 or more!");

        synchronized (this.workerThreads) {
            if (isRunning()) throw new IllegalStateException("RenderManager is already running!");
            this.workerThreads.clear();
            this.busyCount.set(0);

            if (progressTracker != null) progressTracker.cancel();
            progressTracker = new ProgressTracker(5000, 12); // 5-sec steps over one minute
            this.newTask = true;

            this.running = true;

            for (int i = 0; i < threadCount; i++) {
                WorkerThread worker = new WorkerThread();
                this.workerThreads.add(worker);
                worker.start();
            }
        }
    }

    public void stop() {
        synchronized (this.workerThreads) {
            this.running = false;
            for (WorkerThread worker : workerThreads) worker.interrupt();
            if (progressTracker != null) progressTracker.cancel();
        }
    }

    public boolean isRunning() {
        synchronized (this.workerThreads) {
            for (WorkerThread worker : workerThreads) {
                if (worker.isAlive()) return true;
            }

            return false;
        }
    }

    public void awaitIdle() throws InterruptedException {
        awaitIdle(false);
    }

    public void awaitIdle(boolean log) throws InterruptedException {
        synchronized (this.renderTasks) {
            while (!this.renderTasks.isEmpty()) {
                this.renderTasks.wait(5000);

                if (log) {
                    RenderTask task = this.getCurrentRenderTask();
                    if (task != null) {
                        Logger.global.logInfo("Waiting for task '" + task.getDescription() + "' to stop.. (" +
                                (Math.round(task.estimateProgress() * 10000) / 100.0) + "%)");
                    }
                }

            }
        }
    }

    public void awaitShutdown() throws InterruptedException {
        synchronized (this.workerThreads) {
            while (isRunning())
                this.workerThreads.wait(10000);
        }
    }

    public boolean scheduleRenderTask(RenderTask task) {
        synchronized (this.renderTasks) {
            if (containsRenderTask(task)) return false;

            removeTasksThatAreContainedIn(task);
            renderTasks.addLast(task);
            renderTasks.notifyAll();
            return true;
        }
    }

    public boolean scheduleRenderTaskNext(RenderTask task) {
        synchronized (this.renderTasks) {
            if (renderTasks.size() <= 1) return scheduleRenderTask(task);
            if (containsRenderTask(task)) return false;

            removeTasksThatAreContainedIn(task);
            renderTasks.add(1, task);
            renderTasks.notifyAll();
            return true;
        }
    }

    public int scheduleRenderTasksNext(RenderTask... tasks) {
        synchronized (this.renderTasks) {
            int count = 0;
            for (int i = tasks.length - 1; i >= 0; i--) {
                if (scheduleRenderTaskNext(tasks[i])) count++;
            }
            return count;
        }
    }

    public void reorderRenderTasks(Comparator<RenderTask> taskComparator) {
        synchronized (this.renderTasks) {
            if (renderTasks.size() <= 2) return;

            RenderTask currentTask = renderTasks.removeFirst();
            renderTasks.sort(taskComparator);
            renderTasks.addFirst(currentTask);
        }
    }

    public boolean removeRenderTask(RenderTask task) {
        synchronized (this.renderTasks) {
            if (this.renderTasks.isEmpty()) return false;

            // cancel the task if it is currently processed
            RenderTask first = renderTasks.getFirst();
            if (first.equals(task)) {
                first.cancel();
                return true;
            }

            // else remove it
            return renderTasks.remove(task);
        }
    }

    public void removeRenderTasksIf(Predicate<RenderTask> removeCondition) {
        synchronized (this.renderTasks) {
            if (this.renderTasks.isEmpty()) return;

            RenderTask first = renderTasks.removeFirst();
            if (removeCondition.test(first)) first.cancel();
            renderTasks.removeIf(removeCondition);
            renderTasks.addFirst(first);
        }
    }

    public void removeAllRenderTasks() {
        synchronized (this.renderTasks) {
            if (this.renderTasks.isEmpty()) return;

            RenderTask first = renderTasks.removeFirst();
            first.cancel();
            renderTasks.clear();
            renderTasks.addFirst(first);
        }
    }

    public long estimateCurrentRenderTaskTimeRemaining() {
        if (progressTracker == null) return 0;

        synchronized (this.renderTasks) {
            RenderTask task = getCurrentRenderTask();
            if (task == null) return 0;

            double progress = task.estimateProgress();
            long timePerProgress = progressTracker.getAverageTimePerProgress();
            return (long) ((1 - progress) * timePerProgress);
        }
    }

    public RenderTask getCurrentRenderTask() {
        synchronized (this.renderTasks) {
            if (this.renderTasks.isEmpty()) return null;
            return this.renderTasks.getFirst();
        }
    }

    public List<RenderTask> getScheduledRenderTasks() {
        synchronized (this.renderTasks) {
            return new ArrayList<>(this.renderTasks);
        }
    }

    public int getScheduledRenderTaskCount() {
        return this.renderTasks.size();
    }

    public boolean containsRenderTask(RenderTask task) {
        synchronized (this.renderTasks) {
            // checking all scheduled renderTasks except the first one, since that is already being processed
            Iterator<RenderTask> iterator = renderTasks.iterator();
            if (!iterator.hasNext()) return false;
            iterator.next(); // skip first

            while(iterator.hasNext()) {
                if (iterator.next().contains(task)) return true;
            }

            return false;
        }
    }

    public int getWorkerThreadCount() {
        return workerThreads.size();
    }

    public long getLastTimeBusy() {
        return lastTimeBusy;
    }

    public Map<RenderTask, Long> getCompletedTasks() {
        return Map.copyOf(completedTasks);
    }

    private void removeTasksThatAreContainedIn(RenderTask containingTask) {
        synchronized (this.renderTasks) {
            if (renderTasks.size() < 2) return;
            RenderTask first = renderTasks.removeFirst();
            if (containingTask.contains(first)) first.cancel();
            renderTasks.removeIf(containingTask::contains);
            renderTasks.addFirst(first);
        }
    }

    private void doWork() throws Exception {
        RenderTask task;

        synchronized (this.renderTasks) {
            while (this.renderTasks.isEmpty())
                this.renderTasks.wait(10000);

            task = this.renderTasks.getFirst();
            if (this.newTask) {
                this.newTask = false;
                this.progressTracker.resetAndStart(task::estimateProgress);
            }

            // the following is making sure every render-thread is done working on this task (no thread is "busy")
            // before continuing working on the next RenderTask
            if (!task.hasMoreWork()) {
                if (busyCount.get() <= 0) {
                    this.completedTasks.put(
                            this.renderTasks.removeFirst(),
                            System.currentTimeMillis()
                    );
                    this.renderTasks.notifyAll();

                    this.newTask = true;

                    busyCount.set(0);
                } else {
                    this.renderTasks.wait(10000);
                }

                return;
            }

            this.busyCount.incrementAndGet();
            this.lastTimeBusy = System.currentTimeMillis();
        }

        try {
            task.doWork();
        } finally {
            synchronized (renderTasks) {
                int busyCount = this.busyCount.decrementAndGet();
                if (busyCount > 0) this.lastTimeBusy = System.currentTimeMillis();
                this.renderTasks.notifyAll();
            }
        }
    }

    public class WorkerThread extends Thread {

        private final int id;

        private WorkerThread() {
            this.id = RenderManager.this.nextWorkerThreadIndex.getAndIncrement();
            this.setName("RenderManager-" + RenderManager.this.id + "-" + this.id);
        }

        @Override
        @SuppressWarnings("BusyWait")
        public void run() {
            try {
                while (RenderManager.this.running) {
                    try {
                        RenderManager.this.doWork();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        Logger.global.logError(
                                "RenderManager(" + RenderManager.this.id + "): WorkerThread(" + this.id +
                                "): Exception while doing some work!", e);

                        try {
                            // on error, wait a few seconds before resurrecting this render-thread
                            // if something goes wrong, this prevents running into the same error on all render-threads
                            // with full-speed over and over again :D
                            Thread.sleep(10000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            } finally {
                synchronized (RenderManager.this.workerThreads) {
                    RenderManager.this.workerThreads.remove(this);
                    RenderManager.this.workerThreads.notifyAll();
                }
            }
        }

    }

}
