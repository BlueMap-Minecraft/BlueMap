package de.bluecolored.bluemap.common.rendermanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CombinedRenderTask<T extends RenderTask> implements RenderTask {

	private final List<T> tasks;
	private int currentTaskIndex;

	public CombinedRenderTask(Collection<T> tasks) {
		this.tasks = new ArrayList<>();
		this.tasks.addAll(tasks);
		this.currentTaskIndex = 0;
	}

	@Override
	public void doWork() throws Exception {
		T task;

		synchronized (this.tasks) {
			if (!hasMoreWork()) return;
			task = this.tasks.get(this.currentTaskIndex);

			if (!task.hasMoreWork()){
				this.currentTaskIndex++;
				return;
			}
		}

		task.doWork();
	}

	@Override
	public boolean hasMoreWork() {
		return this.currentTaskIndex < this.tasks.size();
	}

	@Override
	public double estimateProgress() {
		synchronized (this.tasks) {
			if (!hasMoreWork()) return 1;

			double total = currentTaskIndex;
			total += this.tasks.get(this.currentTaskIndex).estimateProgress();

			return total / tasks.size();
		}
	}

	@Override
	public void cancel() {
		for (T task : tasks) task.cancel();
	}

}
