package biz.aQute.bnd.facade.api;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Represents an instance created by a Delegate. The instance must automatically
 * be closed when the Delegate is unregistered.
 *
 * @param <D>
 *                the type of the domain
 */
@ConsumerType
public interface Instance<D> {

	/**
	 * Get the backing object. This is never null.
	 */
	D get();

	/**
	 * Get the current state of the backing object. This state is passed to the
	 * {@link Delegate#create(String, Object)} when it needs to be recreated.
	 */
	@Nullable
	Object getState();

	/**
	 * Close the instance. This will free the backing object. This method can be
	 * called multiple times but will only work the first time.
	 */
	void close();
}
