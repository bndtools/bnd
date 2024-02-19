package biz.aQute.ai.assistant.eclipse.ui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

public class Util {
	final Display display;

	public Util(Display display) {
		this.display = display;
	}

	final <R> void job(String name, Supplier<R> background, Consumer<R> displayThread) {
		Job job = Job.create(name, mon -> {
			R result = background.get();
			display.asyncExec(() -> {
				displayThread.accept(result);
			});
		});
		job.schedule();
	}

	public void onDisplay(Runnable callback) {
		display.asyncExec(() -> {
			callback.run();
		});
	}

	public static String formatMilliseconds(long millis) {
		if (millis < 1000) {
			return millis + " ms";
		} else if (millis < 60_000) {
			return (millis / 1000) + " s";
		} else if (millis < 3_600_000) {
			return (millis / 60_000) + " min";
		} else {
			return (millis / 3_600_000) + " h";
		}
	}
}
