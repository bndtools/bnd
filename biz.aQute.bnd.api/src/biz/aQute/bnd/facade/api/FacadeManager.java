package biz.aQute.bnd.facade.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The Facade Manager manages Binder objects that connect a facade (a normal
 * object) to a delegate, dynamic OSGi service. The Binder mediates between a
 * facade that delegates all its methods to a delegate. Since the delegate is
 * dynamic, this Facade Manager will inform the Binder of the presence of the
 * service so delegation can be postponed until the service is available.
 * <p>
 * Since the facade is a normal Java object, there is no explicit life cycle.
 * This Facade Manager will periodically poll the Binder objects to detect when
 * the facade object is garbage collected.
 * <p>
 * This is a singleton service, not other instance is allowed.
 */
@ProviderType
public interface FacadeManager {

	/**
	 * The key for a service property. This is the unique id of a backing
	 * service. At most one service should be registered with this id at any
	 * moment in time. The value must be a String.
	 */
	String FACADE_ID = "facade.id";

	/**
	 * Register a binder. For a given Binder, this must happen only once per
	 * registered Facade Manager.
	 *
	 * @param binder the binder to register
	 * @return a closeable that will unregister this binder is closed from the
	 *         facade.
	 */
	AutoCloseable register(Binder<?> binder);
}
