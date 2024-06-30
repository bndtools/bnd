
@Export
@org.osgi.annotation.versioning.Version("1.0.0")
package biz.aQute.bnd.facade.api;

/**
 * The biz.aQute.bnd.facade.api package defines a service for facades. The
 * Binder class provides an object that links a facade class to a backing
 * service. A facade class is a class that delegates all methods to the Binder,
 * each EP type, e.g. IClassPathContainer, has a corresponding facade. E.g.
 * IClassPathContainer (see bndtools.facades project).
 * <p>
 * The facade type and the backing service type might differ. For example, the
 * IncrementalProjectBuilder has many final methods. A special interface is
 * necessary that passes the IncrementalProjectBuilder as a parameter to the
 * methods that the backing service should provide. The Binder object is linked
 * via statics (yuck) to a Facade Manager.
 * <p>
 * The Facade Manager tracks delegate services.A delegate service can either be
 * a PROTOTYPE component service or implement the Delegate interface. When there
 * is a proper delegate, a Binder is bound to that delegate. Both Binder and
 * delegate have a FACADE_ID which is used to line them up. Multiple Binder
 * objects can be bound to a single delegate. Each Binder object will be bound
 * to a unique instance. The EP defines the name of the class. To define the
 * FACADE ID, the class name is appended with a ':' and the facade id. The
 * Eclipse Binder class in bndtools.facade project handles the interaction with
 * the EP subsystem.
 * <p>
 * When a Facade Manager is activated, it will set itself as manager in the
 * Binder class. This will register each Binder that was created so far and not
 * yet purged. When a delegate is registered, the Facade Manager will take care
 * that the Binder's with the same type are bound. If the service is
 * unregistered, it will unbind the Binder and close the instance. A Binder
 * keeps a weak reference to the facade object. If the facade object is garbage
 * collected, the Binder should be closed. A periodic task in the Facade Manager
 * is responsible for this. When a Binder is unbound, the backing service can
 * provide a memento if it implements the Memento interface. When a new instance
 * is created, this memento is handed over to the create function in Delegate.
 * Clearly this state should not contain references to classes from the backing
 * service's bundle since the bundle is likely restarted. However, this allows
 * backing services to transfer state between a current instance and a future
 * instance.
 */
import org.osgi.annotation.bundle.Export;
