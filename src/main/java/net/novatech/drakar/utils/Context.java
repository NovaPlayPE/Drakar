package net.novatech.drakar.utils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;

public class Context {
	final private ArrayDeque<Task> tasks;
	final private ArrayList<Thread> workers;
	final private ArrayList<Task> activeTasks;
	private OnStartCallback onStartCallback;
	private boolean isRunning;

	public boolean isPaused = false;

	public interface OnStartCallback {
		void onStart();
	}

	public Context() {
		activeTasks = new ArrayList<>();
		tasks = new ArrayDeque<>();
		workers = new ArrayList<>();
		onStartCallback = null;
		isRunning = false;
	}

	public void addTask(Task t) {
		tasks.add(t);
		if (this.isPaused)
			this.resume();
	}

	private Task getNextTask() {
		return tasks.removeFirst();
	}

	public void start() throws TaskNotCompletedException {
		isRunning = true;
		if (onStartCallback != null)
			this.onStartCallback.onStart();
		while (!tasks.isEmpty()) {
			Task currentTask = this.getNextTask();
			if (!currentTask.isAsynchronous) {
				Exception exception = null;
				try {
					activeTasks.add(currentTask);
					currentTask.onInitialise();
					currentTask.run();
				} catch (Exception e) {
					exception = e;
				}
				currentTask.markCompleted();
				activeTasks.remove(currentTask);
				if (currentTask.onComplete(exception)) {
					throw new TaskNotCompletedException(currentTask);
				}
			} else {
				activeTasks.add(currentTask);
				Thread thread = new Thread(() -> {
					synchronized (currentTask) {
						Exception exception = null;
						try {
							currentTask.run();
							currentTask.markCompleted();
						} catch (IOException ioException) {
							exception = ioException;
						}
						if (currentTask.onComplete(exception))
							try {
								throw new TaskNotCompletedException(currentTask);
							} catch (TaskNotCompletedException e) {
								e.printStackTrace();
							} finally {
								activeTasks.remove(currentTask);
							}
					}
				});
				currentTask.setTaskThread(thread);
				workers.add(thread);
				currentTask.onInitialise();
				thread.start();
			}
		}
		this.isRunning = false;
	}

	public int getTaskLength() {
		return tasks.size();
	}

	public void pause() throws InterruptedException {
		// TODO: Test this method
		this.isPaused = true;
		for (Thread thread : workers) {
			if (thread.isAlive())
				thread.wait();
		}
	}

	public void resume() {
		// TODO: Test this method
		this.isPaused = false;
		for (Thread thread : workers) {
			thread.notify();
		}
	}

	public void stop() {
		isRunning = false;
		for (Thread thread : workers) {
			if (thread.isAlive()) {
				try {
					thread.wait();
					thread.stop();
				} catch (Exception ignore) {
				}
			}
		}
		tasks.clear();
	}

	public boolean isRunning() {
		return !isRunning;
	}

	public Task getFirstTask() {
		return this.tasks.getFirst();
	}

	public void addOnStartCallback(OnStartCallback callback) {
		this.onStartCallback = callback;
	}

	public ArrayList<Task> getActiveTasks() {
		return activeTasks;
	}
}