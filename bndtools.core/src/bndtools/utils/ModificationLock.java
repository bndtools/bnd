package bndtools.utils;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

/**
 * <p>
 * This class is intended for use by UI event listeners that need to execute
 * tasks <b>only</b> when fired as a result of user interaction and not as a
 * result of programmatic changes. For example a {@link ModifyListener} on a
 * {@link Text} field might wish to trigger a validation check when the user
 * types in the field but not when {@link Text#setText(String)} is invoked.
 * Usually this is necessary when the programmatic modification is itself made
 * in response to an event, potentially leading to infinite event loops.
 * </p>
 * <p>
 * <b>NB:</b> This code is not intended for multi-threaded use, but merely for
 * detecting modification operations occurring earlier in the call-stack.
 * </p>
 *
 * @author Neil Bartlett
 */
public class ModificationLock {

	private final AtomicInteger modifierCount = new AtomicInteger(0);

	/**
	 * Perform a programmatic change. Changes are re-entrant, we can always
	 * excecute a new programmatic change inside an existing one.
	 *
	 * @param runnable
	 */
	public void modifyOperation(Runnable runnable) {
		try {
			modifierCount.incrementAndGet();
			runnable.run();
		} finally {
			modifierCount.decrementAndGet();
		}
	}

	/**
	 * @return Whether a modification operation is ongoing.
	 */
	public boolean isUnderModification() {
		return modifierCount.get() > 0;
	}

	/**
	 * Perform an action only if no programmatic changes are in progress.
	 *
	 * @param runnable
	 */
	public void ifNotModifying(Runnable runnable) {
		if (modifierCount.get() == 0) {
			runnable.run();
		}
	}
}
