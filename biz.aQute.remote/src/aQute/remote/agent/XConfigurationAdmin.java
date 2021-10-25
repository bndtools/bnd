package aQute.remote.agent;

import static aQute.remote.agent.AgentServer.createResult;
import static aQute.remote.api.XResultDTO.ERROR;
import static aQute.remote.api.XResultDTO.SUCCESS;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.remote.api.XConfigurationDTO;
import aQute.remote.api.XObjectClassDefDTO;
import aQute.remote.api.XResultDTO;

public class XConfigurationAdmin {

	private BundleContext		context;
	private Object				metatype;
	private ConfigurationAdmin	configAdmin;

	public XConfigurationAdmin(BundleContext context, Object configAdmin, Object metatype) {
		this.context = requireNonNull(context);
		this.metatype = metatype;
		this.configAdmin = (ConfigurationAdmin) configAdmin;
	}

	public List<XConfigurationDTO> getConfigurations() {
		if (configAdmin == null) {
			return Collections.emptyList();
		}
		List<XConfigurationDTO> configsWithoutMetatype = null;
		try {
			configsWithoutMetatype = findConfigsWithoutMetatype();
		} catch (IOException | InvalidSyntaxException e) {
			return Collections.emptyList();
		}
		return configsWithoutMetatype;
	}

	public XResultDTO createOrUpdateConfiguration(String pid, Map<String, Object> newProperties) {
		if (configAdmin == null) {
			return createResult(XResultDTO.SKIPPED, "Required services are unavailable to process the request");
		}
		XResultDTO result = null;
		try {
			String action = null;
			Configuration configuration = configAdmin.getConfiguration(pid, "?");
			if (configuration.getProperties() == null) { // new configuration
				action = "created";
			} else {
				action = "updated";
			}
			configuration.update(new Hashtable<>(newProperties));
			result = createResult(SUCCESS, "Configuration with PID '" + pid + "' has been " + action);
		} catch (Exception e) {
			result = createResult(ERROR,
				"Configuration with PID '" + pid + "' cannot be processed due to " + e.getMessage());
		}
		return result;
	}

	public XResultDTO deleteConfiguration(String pid) {
		if (configAdmin == null) {
			return createResult(XResultDTO.SKIPPED, "Required services are unavailable to process the request");
		}
		XResultDTO result = null;
		try {
			Configuration[] configs = configAdmin.listConfigurations(null);
			if (configs == null) {
				return createResult(SUCCESS, "Configuration with PID '" + pid + "' cannot be found");
			}
			for (Configuration configuration : configs) {
				if (configuration.getPid()
					.equals(pid)) {
					configuration.delete();
					result = createResult(SUCCESS, "Configuration with PID '" + pid + "' has been deleted");
				}
			}
			if (result == null) {
				result = createResult(SUCCESS, "Configuration with PID '" + pid + "' cannot be found");
			}
		} catch (Exception e) {
			result = createResult(ERROR,
				"Configuration with PID '" + pid + "' cannot be deleted due to " + e.getMessage());
		}
		return result;
	}

	public XResultDTO createFactoryConfiguration(String factoryPid, Map<String, Object> newProperties) {
		if (configAdmin == null) {
			return createResult(XResultDTO.SKIPPED, "Required services are unavailable to process the request");
		}
		XResultDTO result = null;
		try {
			Configuration configuration = configAdmin.getFactoryConfiguration(factoryPid, "?");
			configuration.update(new Hashtable<>(newProperties));
			result = createResult(SUCCESS, "Configuration with factory PID '" + factoryPid + " ' has been created");
		} catch (Exception e) {
			result = createResult(ERROR,
				"Configuration with factory PID '" + factoryPid + "' cannot be created due to " + e.getMessage());
		}
		return result;
	}

	private List<XConfigurationDTO> findConfigsWithoutMetatype() throws IOException, InvalidSyntaxException {
		List<XConfigurationDTO> dtos = new ArrayList<>();
		Configuration[] configs = configAdmin.listConfigurations(null);
		if (configs == null) {
			return dtos;
		}
		for (Configuration config : configs) {
			boolean hasMetatype = metatype == null ? false : XMetaTypeAdmin.hasMetatype(context, metatype, config);
			if (!hasMetatype) {
				dtos.add(toConfigDTO(config, null));
			}
		}
		return dtos;
	}

	private XConfigurationDTO toConfigDTO(Configuration configuration, XObjectClassDefDTO ocd) {
		XConfigurationDTO dto = new XConfigurationDTO();

		dto.ocd = ocd;
		dto.pid = Optional.ofNullable(configuration)
			.map(Configuration::getPid)
			.orElse(null);
		dto.factoryPid = Optional.ofNullable(configuration)
			.map(Configuration::getFactoryPid)
			.orElse(null);
		dto.properties = Optional.ofNullable(configuration)
			.map(c -> AgentServer.valueOf(configuration.getProperties()))
			.orElse(null);
		dto.location = Optional.ofNullable(configuration)
			.map(Configuration::getBundleLocation)
			.orElse(null);

		return dto;
	}

}
