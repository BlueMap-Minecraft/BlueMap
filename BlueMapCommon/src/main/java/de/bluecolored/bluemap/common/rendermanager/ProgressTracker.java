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

import java.util.Deque;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ProgressTracker {
	private static final AtomicInteger ID = new AtomicInteger(0);

	private final Timer timer;
	private Supplier<Double> progressSupplier;
	private final int averagingCount;

	private long lastTime;
	private double lastProgress;

	private final Deque<Long> timesPerProgress;

	public ProgressTracker(long updateIntervall, int averagingCount) {
		this.timer = new Timer("BlueMap-ProgressTracker-Timer-" + ID.getAndIncrement(), true);
		this.progressSupplier = () -> 0d;
		this.averagingCount = averagingCount;
		this.timesPerProgress = new LinkedList<>();

		this.timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				update();
			}
		}, updateIntervall, updateIntervall);
	}

	public synchronized void resetAndStart(Supplier<Double> progressSupplier) {
		this.progressSupplier = progressSupplier;
		this.lastTime = System.currentTimeMillis();
		this.lastProgress = progressSupplier.get();
		this.timesPerProgress.clear();
	}

	public synchronized long getAverageTimePerProgress() {
		return timesPerProgress.stream()
				.collect(Collectors.averagingLong(Long::longValue))
				.longValue();
	}

	private synchronized void update() {
		long now = System.currentTimeMillis();
		double progress = progressSupplier.get();

		long deltaTime = now - lastTime;
		double deltaProgress = progress - lastProgress;

		if (deltaProgress != 0) {
			long totalDuration = (long) (deltaTime / deltaProgress);

			timesPerProgress.addLast(totalDuration);
			while (timesPerProgress.size() > averagingCount) timesPerProgress.removeFirst();

			this.lastTime = now;
			this.lastProgress = progress;
		}
	}

	public void cancel() {
		timer.cancel();
	}

}
