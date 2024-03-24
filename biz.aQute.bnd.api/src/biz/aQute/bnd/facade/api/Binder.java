package biz.aQute.bnd.facade.api;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A Binder establishes a connection between a <em>facade</em> object used in a
 * non-OSGi environment and a delegate implemented as a <em>backing
 * service</em>. Any call in the facade is delegated to the delegate service.
 * Since the delegate is a service, the service can be absent. The Binder will
 * wait a {@link #setTimeout(long)} time before it will throw an exception.
 * <p>
 * The facade must a {@link FacadeManager#FACADE_ID} during construction. This
 * id is assumed to be registered as a service by the delegate backing service.
 * The Facade Manager service will track the different parties and inject the
 * binder when the service is available.
 * <p>
 * The primary use case for Binders is in the context of Eclipse Extension
 * Points. In this subsystem, objects are created dynamically without contextual
 * information, and their lifecycle cannot be controlled directly. The concept
 * behind Binders is to create a facade class for such extension points. The
 * facade creates a Binder, which in turn manages the OSGi-related aspects. It
 * locates the appropriate backing service through the Facade Manager. Given the
 * highly dynamic nature of this environment, the Binder handles the absence of
 * a backing service by delaying delegation if a backing service is not
 * immediately available. See @see Facade {@link FacadeManager}
 * <p>
 * The Binder uses statics to be able to interface with the brain dead world out
 * there.
 *
 * @param <D> the domain type
 */
@ProviderType
public class Binder<D> implements Supplier<D>, Consumer<D>, AutoCloseable {

	static final List<Binder<?>>	binders		= new ArrayList<>();
	static FacadeManager			facadeManager;
	static int						purgeCount	= 100;

	/**
	 * Set the facade manager
	 */
	public static void setFacadeManager(FacadeManager fm) {
		List<Binder<?>> snapshot;
		synchronized (binders) {
			facadeManager = fm;
			snapshot = new ArrayList<>(binders);
		}

		if (fm != null) {
			snapshot.forEach(b -> b.register(fm));
		} else {
			snapshot.forEach(Binder::release);
		}
	}

	/**
	 * Purge stale binders
	 */
	public static void purge() {
		List<Binder<?>> snapshot;
		synchronized (binders) {
			snapshot = new ArrayList<>(binders);
		}
		for (Binder<?> b : snapshot) {
			if (b.hasGone()) {
				b.close();
			}
		}
	}

	/**
	 * Create a binder and register it locally. If a Facaade Manager is set,
	 * register there.
	 *
	 * @param facade the object acting as a facade, only a weak reference will
	 *            be maintained
	 * @param domainType the actual domain type
	 * @param id the service object facade id, see
	 *            {@link FacadeManager#FACADE_ID}
	 * @param attributes optional attributes of the facade
	 */
	public static <T> Binder<T> create(Object facade, Class<?> domainType, String id, @Nullable
	Map<String, String> attributes) {

		Binder<T> binder = new Binder<>(facade, domainType, id, attributes);

		synchronized (binders) {
			binders.add(binder);
			if (facadeManager != null) {
				binder.register(facadeManager);
			} else {
				binder.release();
			}
		}

		return binder;
	}

	final Map<String, String>				attributes;
	final WeakReference<?>					facade;
	final String							id;
	final Object							lock			= new Object();
	final AtomicReference<AutoCloseable>	registration	= new AtomicReference<>();
	final Class<?>							domainType;

	long									timeout			= TimeUnit.SECONDS.toNanos(30);
	D										current;
	Object									state;
	int										cycles;
	boolean									closed;

	Binder(Object facade, Class<?> domainType, String id, @Nullable
	Map<String, String> attributes) {
		this.domainType = domainType;
		this.id = id;
		this.attributes = attributes == null ? Collections.emptyMap() : new HashMap<>(attributes);
		this.facade = new WeakReference<>(facade);
	}

	void register(FacadeManager facadeManager) {
		AutoCloseable register = facadeManager.register(this);
		AutoCloseable prev = registration.getAndSet(register);
		assert prev == null;
		cycles++;
	}

	void release() {
		synchronized (lock) {
			this.current = null;
			close(registration.getAndSet(null));
			lock.notifyAll();
			cycles++;
		}
	}

	@Override
	public void close() {
		release();
		synchronized (lock) {
			if (closed)
				return;
			closed = true;
		}
		synchronized (binders) {
			binders.remove(this);
		}
	}

	/**
	 * Check if this Binder's facade has been gc'ed.
	 */

	public boolean hasGone() {
		return facade.get() == null;
	}

	/**
	 * Get the backing service object. Since it is possible that there is not
	 * yet such an object, this will wait a {@link #timeout} number of
	 * nanoseconds before giving up.
	 */
	@Override
	public D get() {

		synchronized (lock) {
			if (closed)
				throw new IllegalStateException("the binder with id " + id + " has been closed for " + attributes);

			if (current != null)
				return current;

			try {
				long start = System.nanoTime();

				while (current == null) {
					if ((System.nanoTime() - start) > timeout) {
						throw new IllegalStateException("no backing service " + id + " can be found for " + attributes);
					}
					lock.wait(1_000);
					if (closed)
						throw new IllegalStateException(
							"the binder with id " + id + " has been closed for " + attributes);
				}
				return current;
			} catch (InterruptedException e) {
				Thread.currentThread()
					.interrupt();
				throw new IllegalStateException(
					"interrupted while waiting for backing service " + id + " for " + attributes);
			}
		}
	}

	/**
	 * Provide the backing service without waiting
	 */
	public D peek() {
		synchronized (lock) {
			return current;
		}
	}

	/**
	 * Set the backing service to a new value
	 */
	@Override
	public void accept(D t) {
		synchronized (lock) {
			if (!closed)
				this.current = t;
			lock.notifyAll();
		}
	}

	/**
	 * Return the ID of the backing service
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get the attributes of the facade object
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * Set the timeout to wait for the backing service to be injected.
	 *
	 * @param timeout timeout in milliseconds
	 */
	public void setTimeout(long timeout) {
		this.timeout = TimeUnit.MILLISECONDS.toNanos(timeout);
	}

	/**
	 * Get the saved state of this Binder
	 */

	public Object getState() {
		return state;
	}

	/**
	 * Get the the state to save
	 */
	public void setState(Object state) {
		this.state = state;

	}

	@Override
	public String toString() {
		return "Binder[id=" + id + "]";
	}

	private static void close(AutoCloseable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	public WeakReference<?> getFacade() {
		return facade;
	}

	public long getTimeout() {
		return timeout;
	}

	public Class<?> getDomainType() {
		return domainType;
	}

}
