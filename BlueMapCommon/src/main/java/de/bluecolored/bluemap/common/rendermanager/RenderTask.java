package de.bluecolored.bluemap.common.rendermanager;

public interface RenderTask {

	void doWork() throws Exception;

	/**
	 * Whether this task is requesting more calls to its {@link #doWork()} method.<br>
	 * This can be false because the task is finished, OR because the task got cancelled and decides to interrupt.
	 */
	boolean hasMoreWork();

	/**
	 * The estimated progress made so far, from 0 to 1.
	 */
	default double estimateProgress() {
		return 0d;
	}

	/**
	 * Requests to cancel this task. The task then self-decides what to do with this request.
	 */
	void cancel();

}
