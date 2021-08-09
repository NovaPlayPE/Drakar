package net.novatech.drakar.utils;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public abstract class Task {
	private boolean taskCompleted = false;
	final private Context context;
	public boolean isAsynchronous;
	private Thread taskThread;
	public UUID ID;

	public Task(boolean isAsynchronous, Context context) {
		ID = UUID.randomUUID();
		this.isAsynchronous = isAsynchronous;
		this.context = context;
		this.taskThread = null;
	}

	public abstract void run() throws IOException;

	public abstract boolean onComplete(Exception exception);

	public void onInitialise() {
	}

	public void markCompleted() {
		taskCompleted = true;
	}

	public boolean isCompleted() {
		return taskCompleted;
	}

	public void markNotCompleted() {
		taskCompleted = false;
	}

	public Context getContext() {
		return context;
	}

	public void setTaskThread(Thread taskThread) {
		this.taskThread = taskThread;
	}

	public Thread getTaskThread() {
		return this.taskThread;
	}

}