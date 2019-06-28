package aQute.bnd.runtime.facade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.bnd.runtime.api.SnapshotProvider;

public class ConfigurationFacade implements SnapshotProvider {

	final BundleContext context;

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
	}

	private ConfigurationsDTO getConfigurationDTO() throws Exception {
		ConfigurationsDTO dto = new ConfigurationsDTO();
		ConfigurationAdmin cmAdmin = getConfigurationAdmin();
		if (cmAdmin == null) {
			dto.errors.add("Cannot find configuration admin");
		} else {

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

	private ConfigurationAdmin getConfigurationAdmin() {
		ServiceReference<ConfigurationAdmin> ref = context.getServiceReference(ConfigurationAdmin.class);
		if (ref == null) {
			return null;
		}
		return context.getService(ref);
	}

	@Override
	public Object getSnapshot() throws Exception {
		return getConfigurationDTO();
	}

	@Override
	public void close() throws IOException {}

}
