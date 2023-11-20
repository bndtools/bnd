package biz.aQute.bnd.facade.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The Facade Manager manages facades that delegate to a backing service. The
 * facades are constant objects that are maintained by ignorant code that has no
 * clue about dynamics. In OSGi, however, the backing service can come and go,
 * and event the Facade Manger is not required to be constant.
 * <p>
 * A Binder is created by the facade object. This binder, tracks the Facade
 * Manager and registers when found.
 */
@ProviderType
public interface FacadeManager {

	/**
	 * The key for a service property. This is the unique id of a backing
	 * service. Only one service should be registered with this id at any moment
	 * in time. The value must be a String.
	 */
	String FACADE_ID = "facade.id";

	/**
	 * Register a binder. For a given binder, this should happen only once per Facade Manager.
	 *  
	 * @param binder the binder to register
	 * @return a closeable that will unregister this binder when closed.
	 */
	AutoCloseable register(Binder<?> binder);
}
