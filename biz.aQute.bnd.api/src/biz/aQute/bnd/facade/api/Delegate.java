package biz.aQute.bnd.facade.api;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Normally it is sufficient to register a PROTOTYPE scoped component to provide
 * the backing service for a facade. However, sometimes it is necessary to
 * manage the life cycle in more detail.
 * <p>
 * A Delegate is responsible for creating instances of the facade backing
 * object. It is associated with a unique ID. There should be only one Delegate
 * service with the given id. A Delegate service must be registered with the
 * service property {@link FacadeManager#FACADE_ID}
 * 
 * @param <D>
 *                the domain type of the delegate
 */
@ConsumerType
public interface Delegate<D> {
	/**
	 * The ID of the delegate. This ID must match the ID of the facade.
	 */
	String getId();

	/**
	 * Create a new instance of the backing object. When an instance is
	 * released, the Facade Manager will ask for its state. The state of the
	 * previous instance, maybe from a previous delegate or even Facade Manager,
	 * will be past to this method.
	 * 
	 * @param description
	 *                        a description about the caller
	 * @param state
	 *                        the current state or null
	 * @return an instance
	 */
	Instance<D> create(String description, @Nullable Object state);
}
