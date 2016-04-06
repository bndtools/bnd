package biz.aQute.configadmin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * <p>
 * This is a simplified ConfigurationAdmin implementation that is intended for
 * statically configuring an OSGi runtime during startup. It should be used as
 * follows:
 * </p>
 * <ul>
 * <li>Load the configuration and instantiate a set of
 * {@link StaticConfiguration} objects.</li>
 * <li>Instantiate a {@code
 * StaticConfigurationAdmin}.</li>
 * <li>Call {@link #start()}. This will register the object as a
 * {@link ConfigurationAdmin} service.</li>
 * <li>After all bundles are started, call {@link #updatedManagedServices()}.
 * This sends the {@code updated} signal to all registered
 * {@link ManagedService} and {@link ManagedServiceFactory} services.</li>
 * </ul>
 * <p>
 * Note that since the implementation is intended only for <em>static</em>
 * configuration, it does not support any methods for creating, modifying or
 * deleting configs. Any attempt to call these methods (including
 * {@link #getConfiguration(String)} with a not-already-existing PID) will throw
 * a {@link SecurityException}.
 * </p>
 * 
 * @author Neil Bartlett
 */
public class StaticConfigurationAdmin implements ConfigurationAdmin {

	private final BundleContext						context;
	private final Map<String,StaticConfiguration>	configsMap	= new LinkedHashMap<String,StaticConfiguration>();

	private ServiceRegistration						serviceReg;
	private PidTracker<ManagedService>				msTracker;
	private PidTracker<ManagedServiceFactory>		msfTracker;

	public StaticConfigurationAdmin(BundleContext context, Collection<StaticConfiguration> configs) {
		this.context = context;
		for (StaticConfiguration config : configs) {
			String pid = config.getPid();

			if (configsMap.containsKey(pid))
				throw new IllegalArgumentException("Duplicate configuration pid: " + pid);
			configsMap.put(pid, config);
		}
	}

	public void start() {
		serviceReg = context.registerService(ConfigurationAdmin.class.getName(), this, null);

		msTracker = new PidTracker<ManagedService>(context, ManagedService.class);
		msTracker.open();

		msfTracker = new PidTracker<ManagedServiceFactory>(context, ManagedServiceFactory.class);
		msfTracker.open();
	}

	/**
	 * Sends the configuration to all currently registered ManagedService and
	 * ManagedServiceFactory services. Should be called after all bundles have
	 * been started.
	 */
	public List<ConfigurationException> updatedManagedServices() {
		List<ConfigurationException> exceptions = new LinkedList<ConfigurationException>();

		for (Configuration config : configsMap.values()) {
			String factoryPid = config.getFactoryPid();
			if (factoryPid != null) {
				ManagedServiceFactory msf = msfTracker.findPid(factoryPid);
				if (msf != null) {
					try {
						msf.updated(factoryPid, config.getProperties());
					} catch (ConfigurationException e) {
						exceptions.add(e);
					}
				}
			} else {
				ManagedService ms = msTracker.findPid(config.getPid());
				if (ms != null) {
					try {
						ms.updated(config.getProperties());
					} catch (ConfigurationException e) {
						exceptions.add(e);
					}
				}
			}
		}

		return exceptions;
	}

	public void stop() {
		msfTracker.close();
		msfTracker = null;

		msTracker.close();
		msTracker = null;

		serviceReg.unregister();
		serviceReg = null;
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
		throw new SecurityException("Cannot create configurations");
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
		throw new SecurityException("Cannot create configurations");
	}

	@Override
	public Configuration getConfiguration(String pid, String location) throws IOException {
		StaticConfiguration config = configsMap.get(pid);
		if (config == null)
			throw new SecurityException("Cannot create configurations");

		return config;
	}

	@Override
	public Configuration getConfiguration(String pid) throws IOException {
		return getConfiguration(pid, null);
	}

	@Override
	public Configuration[] listConfigurations(String filterStr) throws IOException, InvalidSyntaxException {
		Filter filter = filterStr != null ? FrameworkUtil.createFilter(filterStr) : null;

		List<StaticConfiguration> result = new ArrayList<StaticConfiguration>(configsMap.size());
		for (StaticConfiguration config : configsMap.values()) {
			if (filter == null || filter.match(config.getProperties()))
				result.add(config);
		}

		return result.toArray(new Configuration[0]);
	}

}
