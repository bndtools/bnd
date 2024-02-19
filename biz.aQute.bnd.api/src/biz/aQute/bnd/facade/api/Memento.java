package biz.aQute.bnd.facade.api;

import java.lang.ref.WeakReference;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * <p>
 * The return object must take care not to use domain classes. It should be
 * possible to recycle the bundle of the implementation between
 * {@link #getState()} and {@link #setState(Object,WeakReference)}. If the state
 * object refers to classes from this bundle, it is bound to cause class cast
 * exceptions.
 */
@ConsumerType
public interface Memento {
	/**
	 * Get the state of the object so that the state of a new object can be
	 * resurrected with the {@link #setState(Object,WeakReference)}.
	 */
	Object getState();

	/**
	 * Set the state of the implementation.
	 *
	 * @param state the given state, retrieved from {@link #getState()} of a
	 *            previous version
	 */
	void setState(Object state, WeakReference<?> facade);
}
