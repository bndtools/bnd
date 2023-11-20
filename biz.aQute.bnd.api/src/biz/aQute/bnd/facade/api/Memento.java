package biz.aQute.bnd.facade.api;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A backing service can have state. When a backing service is unregistered, its
 * instance.
 * <p>
 * The return object must take care not to use domain classes. It should be
 * possible to recycle the bundle of the implementation between
 * {@link #getState()} and {@link #setState(Object)}. If the state object refers
 * to classes from this bundle, it is bound to cause class cast exceptions.
 */
@ConsumerType
public interface Memento {
	/**
	 * Get the state of the object so that the state of a new object can be
	 * resurrected with the {@link #setState(Object)}.
	 */
	Object getState();

	/**
	 * Set the state of the implementation.
	 * @param state the given state, retrieved from {@link #getState()} of a previous version
	 */
	void setState(Object state);
}
