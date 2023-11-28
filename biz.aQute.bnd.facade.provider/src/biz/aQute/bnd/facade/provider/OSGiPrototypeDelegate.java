package biz.aQute.bnd.facade.provider;

import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.ServiceObjects;

import biz.aQute.bnd.facade.api.Binder;
import biz.aQute.bnd.facade.api.Delegate;
import biz.aQute.bnd.facade.api.Instance;
import biz.aQute.bnd.facade.api.Memento;

class OSGiPrototypeDelegate<D> implements Delegate<D> {

	final String			id;
	final ServiceObjects<D>	factory;

	class InstanceImpl implements Instance<D> {
		final D				service	= factory.getService();
		final AtomicBoolean	closed	= new AtomicBoolean(false);

		InstanceImpl(Binder<?> binder) {
			assert service != null;
			if (service instanceof Memento m) {
				m.setState(binder.getState(), binder.getFacade());
			}
		}

		@Override
		public void close() {
			if ( closed.getAndSet(true))
				return;
			factory.ungetService(service);
		}

		@Override
		public Object getState() {
			if (service instanceof Memento m) {
				return m.getState();
			} else
				return null;
		}

		@Override
		public D get() {
			if ( closed.get())
				return null;
			return service;
		}

		@Override
		public String toString() {
			return "PrototypeInstance [id=" + id + ", ref=" + factory.getServiceReference() + "]";
		}
	}

	OSGiPrototypeDelegate(String id, ServiceObjects<D> factory) {
		this.id = id;
		this.factory = factory;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Instance<D> create(Binder<D> binder) {
		return new InstanceImpl(binder);
	}
}