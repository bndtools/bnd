package aQute.miniframework;

import java.util.*;

import org.osgi.framework.*;

public class LocalServiceRegistration implements ServiceRegistration, ServiceReference {
	final Map<String, Object>	properties	= new TreeMap<String, Object>(
													String.CASE_INSENSITIVE_ORDER);
	final Dictionary			readonly	= new Dictionary() {

												public Enumeration elements() {
													return Collections.enumeration(properties
															.values());
												}

												public Object get(Object key) {
													return properties.get(key);
												}

												public boolean isEmpty() {
													return properties.isEmpty();
												}

												public Enumeration keys() {
													return Collections.enumeration(properties
															.keySet());
												}

												public Object put(Object var0, Object var1) {
													throw new UnsupportedOperationException(
															"Service properties are read only");
												}

												public Object remove(Object var0) {
													throw new UnsupportedOperationException(
															"Service properties are read only");
												}

												public int size() {
													return properties.size();
												}
											};
	final private int			id;
	final Context				bundle;
	final Map<Context, Record>	using		= new HashMap<Context, Record>();
	volatile int				ranking		= 0;
	final Object				service;
	final ServiceFactory		factory;
	final Class<?>				clazz;

	static class Record {
		Record(Context bundle) {
			this.bundle = bundle;
		}

		final Context	bundle;
		Object			service;
		int				count	= 0;

		synchronized Object get() {
			try {
				while (service == null)
					wait();
			} catch (InterruptedException e) {
			}
			count++;
			return service;
		}

		synchronized void set(Object service) {
			this.service = service;
			notifyAll();
		}
	}

	LocalServiceRegistration(Context bundle, int id, Class<?> clazz, Object service, Dictionary properties ) {
		this.id = id;
		this.bundle = bundle;
		this.service = service;
		this.clazz = clazz;
		if (service instanceof ServiceFactory)
			factory = (ServiceFactory) service;
		else
			factory = null;

		setProperties(properties);
	}

	public ServiceReference getReference() {
		return this;
	}

	public void setProperties(Dictionary properties) {
		this.properties.clear();
		for (Enumeration e = properties.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			if (key.equals(Constants.SERVICE_RANKING)) {
				ranking = Integer.parseInt((String) properties.get(key));
			}
			properties.put(key, properties.get(key));
		}
	}

	public void unregister() {
		bundle.removeRegistration(this);
		if (factory != null) {
			for (Record record : using.values()) {
				try {
					factory.ungetService(record.bundle, this, record.service);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	public int compareTo(Object reference) {
		if (this == reference)
			return 0;

		LocalServiceRegistration other = (LocalServiceRegistration) reference;
		return ranking > other.ranking ? 1 : (ranking < other.ranking ? -1 : 0);
	}

	public Bundle getBundle() {
		return bundle;
	}

	public Object getProperty(String key) {
		return properties.get(key);
	}

	public String[] getPropertyKeys() {
		return properties.keySet().toArray(new String[properties.size()]);
	}

	public synchronized Bundle[] getUsingBundles() {
		return using.keySet().toArray(new Bundle[using.size()]);
	}

	public boolean isAssignableTo(Bundle bundle, String className) {
		return true;
	}

	Object get(Context user) {
		Record record;

		synchronized (this) {
			record = using.get(user);
			if (record != null)
				return record.get();

			record = new Record(user);
			using.put(user, record);
		}
		if (factory != null)
			record.set(factory.getService(user, this));
		else
			record.set(service);

		return record.get();
	}

	synchronized boolean unget(Context user) {
		Record record;
		synchronized (this) {
			record = using.get(user);
			if (record != null) {
				if (record.count <= 1) {
					using.remove(user);
				} else
					return --record.count > 0;
			} else
				return false;
		}
		if (factory != null)
			factory.ungetService(user, this, record.service);

		return false;
	}
}
