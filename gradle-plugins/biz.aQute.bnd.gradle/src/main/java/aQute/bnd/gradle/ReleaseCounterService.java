package aQute.bnd.gradle;

import java.util.concurrent.atomic.AtomicInteger;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * A counter helper to determine which task is the last release task.
 */
public abstract class ReleaseCounterService
        implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    private final AtomicInteger remaining = new AtomicInteger(0);

	/**
	 * Default public constructor.
	 */
	public ReleaseCounterService() {}

	public void setInitialCount(int n) {
		remaining.set(n);
	}

    /**
	 * Call during execution by each release task. Returns true exactly once:
	 * for the last release task that runs.
	 *
	 * @return <code>true</code> if this is the last task to be released
	 */
    public boolean isLastReleaseTask() {
        return remaining.decrementAndGet() == 0;
    }

	/**
	 * @return the current count of remaining.
	 */
	public int getRemaining() {
		return remaining.get();
	}

    @Override
    public void close() {
        // nothing
    }
}