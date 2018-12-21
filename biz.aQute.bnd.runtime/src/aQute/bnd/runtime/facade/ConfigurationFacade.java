package aQute.bnd.runtime.facade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.runtime.api.SnapshotProvider;

public class ConfigurationFacade implements SnapshotProvider {

	final BundleContext												context;
	final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>	cmAdminTracker;

	public static class ConfigurationsDTO extends DTO {
		public Map<String, ConfigurationDTO>	configurations	= new TreeMap<>();
		public List<String>						errors			= new ArrayList<>();
	}

	public static class ConfigurationDTO extends DTO {
		public String				pid;
		public String				factoryPid;
		public String				bundleLocation;
		public long					changeCount;
		public Map<String, Object>	properties	= new TreeMap<>();
	}

	public ConfigurationFacade(BundleContext context) {
		this.context = context;
		this.cmAdminTracker = new ServiceTracker<>(context, ConfigurationAdmin.class, null);
		this.cmAdminTracker.open();
	}

	private ConfigurationsDTO getConfigurationDTO() throws Exception {
		ConfigurationsDTO dto = new ConfigurationsDTO();
		ConfigurationAdmin cmAdmin = cmAdminTracker.getService();
		if (cmAdmin == null) {
			dto.errors.add("Cannot find configuration admin");
		} else {

			if (cmAdminTracker.size() != 1) {
				dto.errors.add("Multiple Configuration Admin services (only using first) "
					+ Arrays.toString(cmAdminTracker.getServiceReferences()));
			}

			Configuration[] list = cmAdmin.listConfigurations(null);

			if (list != null) {
				for (Configuration c : list) {
					ConfigurationDTO cdto = new ConfigurationDTO();
					cdto.pid = c.getPid();
					cdto.factoryPid = c.getFactoryPid();
					cdto.changeCount = c.getChangeCount();
					cdto.bundleLocation = c.getBundleLocation();
					for (Enumeration<String> e = c.getProperties()
						.keys(); e.hasMoreElements();) {
						String key = e.nextElement();
						Object value = c.getProperties()
							.get(key);
						if (value != null) {
							// TODO check types
							cdto.properties.put(key, value);
						}
					}
					dto.configurations.put(c.getPid(), cdto);
				}
			}
		}
		return dto;
	}

	@Override
	public Object getSnapshot() throws Exception {
		return getConfigurationDTO();
	}

	@Override
	public void close() throws IOException {
		cmAdminTracker.close();
	}

}
