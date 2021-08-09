package net.novatech.drakar.utils;

public class IdleTask extends Task {

	public interface Callback {
		void run(TaskNotCompletedException e);
	}

	final private Callback callback;

	public IdleTask(Context context, Callback callback) {
		super(true, context);
		this.callback = callback;
	}

	@Override
	public void run() {
		while (getContext().getTaskLength() == 0)
			;
		try {
			getContext().start();
		} catch (TaskNotCompletedException e) {
			callback.run(e);
		}
	}

	@Override
	public boolean onComplete(Exception exception) {
		return false;
	}
}