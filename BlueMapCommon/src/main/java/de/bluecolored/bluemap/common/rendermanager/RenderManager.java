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

public class RenderManager {
	private static final AtomicInteger nextRenderManagerIndex = new AtomicInteger(0);

	private final int id;
	private volatile boolean running;

	private final AtomicInteger nextWorkerThreadIndex;
	private final Collection<WorkerThread> workerThreads;
	private final AtomicInteger busyCount;

	private final LinkedList<RenderTask> renderTasks;
	private final Set<RenderTask> renderTaskSet;

	public RenderManager() {
		this.id = nextRenderManagerIndex.getAndIncrement();
		this.nextWorkerThreadIndex = new AtomicInteger(0);

		this.running = false;
		this.workerThreads = new ConcurrentLinkedDeque<>();
		this.busyCount = new AtomicInteger(0);

		this.renderTasks = new LinkedList<>();
		this.renderTaskSet = new HashSet<>();
	}

	public void start(int threadCount) throws IllegalStateException {
		if (threadCount <= 0) throw new IllegalArgumentException("threadCount has to be 1 or more!");

		synchronized (this.workerThreads) {
			if (isRunning()) throw new IllegalStateException("RenderManager is already running!");
			this.workerThreads.clear();
			this.busyCount.set(0);

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
		}
	}

	public boolean isIdle() {
		return busyCount.get() == 0;
	}

	public boolean isRunning() {
		synchronized (this.workerThreads) {
			for (WorkerThread worker : workerThreads) {
				if (worker.isAlive()) return true;
			}

			return false;
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
			if (renderTaskSet.add(task)) {
				renderTasks.addLast(task);
				renderTasks.notifyAll();
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean scheduleRenderTaskNext(RenderTask task) {
		synchronized (this.renderTasks) {
			if (renderTasks.size() <= 1) return scheduleRenderTask(task);

			if (renderTaskSet.add(task)) {
				renderTasks.add(1, task);
				renderTasks.notifyAll();
				return true;
			} else {
				return false;
			}
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

	public boolean removeTask(RenderTask task) {
		synchronized (this.renderTasks) {
			if (this.renderTasks.isEmpty()) return false;

			// cancel the task if it is currently processed
			RenderTask first = renderTasks.getFirst();
			if (first.equals(task)) {
				first.cancel();
				return true;
			}

			// else remove it
			return renderTaskSet.remove(task) && renderTasks.remove(task);
		}
	}

	public void removeAllTasks() {
		synchronized (this.renderTasks) {
			if (this.renderTasks.isEmpty()) return;

			RenderTask first = renderTasks.removeFirst();
			first.cancel();
			renderTasks.clear();
			renderTasks.addFirst(first);
		}
	}

	public List<RenderTask> getScheduledRenderTasks() {
		return Collections.unmodifiableList(renderTasks);
	}

	public int getWorkerThreadCount() {
		return workerThreads.size();
	}

	private void doWork() throws Exception {
		RenderTask task;

		synchronized (this.renderTasks) {
			while (this.renderTasks.isEmpty())
				this.renderTasks.wait(10000);

			task = this.renderTasks.getFirst();

			// the following is making sure every render-thread is done working on this task (no thread is "busy")
			// before continuing working on the next RenderTask
			if (!task.hasMoreWork()) {
				if (busyCount.get() <= 0) {
					this.renderTaskSet.remove(this.renderTasks.removeFirst());
					busyCount.set(0);
				} else {
					this.renderTasks.wait(10000);
				}

				return;
			}

			this.busyCount.incrementAndGet();
		}

		try {
			task.doWork();
		} finally {
			synchronized (renderTasks) {
				this.busyCount.decrementAndGet();
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
