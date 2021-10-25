package aQute.remote.agent;

import static aQute.remote.agent.AgentServer.createResult;
import static aQute.remote.api.XResultDTO.ERROR;
import static aQute.remote.api.XResultDTO.SUCCESS;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.osgi.service.metatype.ObjectClassDefinition.ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import aQute.remote.api.XAttributeDefDTO;
import aQute.remote.api.XAttributeDefType;
import aQute.remote.api.XConfigurationDTO;
import aQute.remote.api.XObjectClassDefDTO;
import aQute.remote.api.XResultDTO;

public class XMetaTypeAdmin {

	private BundleContext		context;
	private MetaTypeService		metatype;
	private ConfigurationAdmin	configAdmin;

	public XMetaTypeAdmin(BundleContext context, Object configAdmin, Object metatype) {
		this.context = requireNonNull(context);
		this.configAdmin = (ConfigurationAdmin) configAdmin;
		this.metatype = (MetaTypeService) metatype;
	}

	public List<XConfigurationDTO> getConfigurations() {
		if (configAdmin == null || metatype == null) {
			return Collections.emptyList();
		}
		List<XConfigurationDTO> configsWithMetatype = null;
		List<XConfigurationDTO> metatypeWithoutConfigs = null;
		List<XConfigurationDTO> metatypeFactories = null;
		try {
			configsWithMetatype = findConfigsWithMetatype();
			metatypeWithoutConfigs = findMetatypeWithoutConfigs();
			metatypeFactories = findMetatypeFactories();
		} catch (IOException | InvalidSyntaxException e) {
			return Collections.emptyList();
		}
		return joinLists(configsWithMetatype, metatypeWithoutConfigs, metatypeFactories);
	}

	public XResultDTO createOrUpdateConfiguration(String pid, Map<String, Object> newProperties) {
		if (configAdmin == null || metatype == null) {
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
		if (configAdmin == null || metatype == null) {
			return createResult(XResultDTO.SKIPPED, "Required services are unavailable to process the request");
		}
		XResultDTO result = null;
		try {
			for (Configuration configuration : configAdmin.listConfigurations(null)) {
				if (configuration.getPid()
					.equals(pid)) {
					configuration.delete();
					result = createResult(SUCCESS, "Configuration with PID '" + pid + "' has been deleted");
				}
			}
		} catch (Exception e) {
			result = createResult(ERROR,
				"Configuration with PID '" + pid + "' cannot be deleted due to " + e.getMessage());
		}
		return result;
	}

	public XResultDTO createFactoryConfiguration(String factoryPid, Map<String, Object> newProperties) {
		if (configAdmin == null || metatype == null) {
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

	private List<XConfigurationDTO> findConfigsWithMetatype() throws IOException, InvalidSyntaxException {
		List<XConfigurationDTO> dtos = new ArrayList<>();
		for (Configuration config : configAdmin.listConfigurations(null)) {
			boolean hasMetatype = hasMetatype(context, metatype, config);
			if (hasMetatype) {
				dtos.add(toConfigDTO(config, toOCD(config)));
			}
		}
		return dtos;
	}

	private List<XConfigurationDTO> findMetatypeWithoutConfigs() throws IOException, InvalidSyntaxException {
		List<XConfigurationDTO> dtos = new ArrayList<>();
		for (Bundle bundle : context.getBundles()) {
			MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
			if (metatypeInfo == null) {
				continue;
			}
			for (String pid : metatypeInfo.getPids()) {
				boolean hasAssociatedConfiguration = checkConfigurationExistence(pid);
				if (!hasAssociatedConfiguration) {
					XObjectClassDefDTO ocd = toOcdDTO(pid, metatypeInfo, ConfigurationType.SINGLETON);
					dtos.add(toConfigDTO(null, ocd));
				}
			}
			for (String fpid : metatypeInfo.getFactoryPids()) {
				boolean hasAssociatedConfiguration = checkFactoryConfigurationExistence(fpid);
				if (!hasAssociatedConfiguration) {
					XObjectClassDefDTO ocd = toOcdDTO(fpid, metatypeInfo, ConfigurationType.FACTORY);
					dtos.add(toConfigDTO(null, ocd));
				}
			}
		}
		return dtos;
	}

	private List<XConfigurationDTO> findMetatypeFactories() {
		List<XConfigurationDTO> dtos = new ArrayList<>();
		for (Bundle bundle : context.getBundles()) {
			MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
			if (metatypeInfo == null) {
				continue;
			}
			for (String fpid : metatypeInfo.getFactoryPids()) {
				XObjectClassDefDTO ocd = toOcdDTO(fpid, metatypeInfo, ConfigurationType.FACTORY);
				XConfigurationDTO configDTO = toConfigDTO(null, ocd);
				configDTO.isFactory = true;
				dtos.add(configDTO);
			}
		}
		return dtos;
	}

	private boolean checkConfigurationExistence(String pid) throws IOException, InvalidSyntaxException {
		return configAdmin.listConfigurations("(service.pid=" + pid + ")") != null;
	}

	private boolean checkFactoryConfigurationExistence(String factoryPid) throws IOException, InvalidSyntaxException {
		return configAdmin.listConfigurations("(service.factoryPid=" + factoryPid + ")") != null;
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

	private XObjectClassDefDTO toOCD(Configuration config) {
		for (Bundle bundle : context.getBundles()) {
			MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
			if (metatypeInfo == null) {
				continue;
			}
			String configPID = config.getPid();
			String configFactoryPID = config.getFactoryPid();

			for (String pid : metatypeInfo.getPids()) {
				if (pid.equals(configPID)) {
					return toOcdDTO(configPID, metatypeInfo, ConfigurationType.SINGLETON);
				}
			}
			for (String fPid : metatypeInfo.getFactoryPids()) {
				if (fPid.equals(config.getFactoryPid())) {
					return toOcdDTO(configFactoryPID, metatypeInfo, ConfigurationType.FACTORY);
				}
			}
		}
		return null;
	}

	private XObjectClassDefDTO toOcdDTO(String ocdId, MetaTypeInformation metatypeInfo, ConfigurationType type) {
		ObjectClassDefinition ocd = metatypeInfo.getObjectClassDefinition(ocdId, null);
		XObjectClassDefDTO dto = new XObjectClassDefDTO();

		dto.id = ocd.getID();
		dto.pid = type == ConfigurationType.SINGLETON ? ocdId : null;
		dto.factoryPid = type == ConfigurationType.FACTORY ? ocdId : null;
		dto.name = ocd.getName();
		dto.description = ocd.getDescription();
		dto.descriptorLocation = metatypeInfo.getBundle()
			.getSymbolicName();
		dto.attributeDefs = Stream.of(ocd.getAttributeDefinitions(ALL))
			.map(this::toAdDTO)
			.collect(toList());

		return dto;
	}

	private XAttributeDefDTO toAdDTO(AttributeDefinition ad) {
		XAttributeDefDTO dto = new XAttributeDefDTO();

		dto.id = ad.getID();
		dto.name = ad.getName();
		dto.description = ad.getDescription();
		dto.type = defType(ad.getType(), ad.getCardinality()).ordinal();
		dto.optionValues = Optional.ofNullable(ad.getOptionLabels())
			.map(Arrays::asList)
			.orElse(null);
		dto.defaultValue = Optional.ofNullable(ad.getDefaultValue())
			.map(Arrays::asList)
			.orElse(null);

		return dto;
	}

	public static boolean hasMetatype(BundleContext context, Object metatypeService, Configuration config) {
		MetaTypeService metatype = (MetaTypeService) metatypeService;
		for (Bundle bundle : context.getBundles()) {
			MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
			if (metatypeInfo == null) {
				continue;
			}
			String pid = config.getPid();
			String factoryPID = config.getFactoryPid();
			String[] metatypePIDs = metatypeInfo.getPids();
			String[] metatypeFactoryPIDs = metatypeInfo.getFactoryPids();
			boolean pidExists = Arrays.asList(metatypePIDs)
				.contains(pid);
			boolean factoryPidExists = Optional.ofNullable(factoryPID)
				.map(fPID -> Arrays.asList(metatypeFactoryPIDs)
					.contains(factoryPID))
				.orElse(false);
			if (pidExists || factoryPidExists) {
				return true;
			}
		}
		return false;
	}

	@SafeVarargs
	private static <T> List<T> joinLists(List<T>... lists) {
		return Stream.of(lists)
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
	}

	private static XAttributeDefType defType(int defType, int cardinality) {
		switch (defType) {
			case AttributeDefinition.STRING :
				if (cardinality > 0) {
					return XAttributeDefType.STRING_ARRAY;
				}
				if (cardinality < 0) {
					return XAttributeDefType.STRING_LIST;
				}
				return XAttributeDefType.STRING;
			case AttributeDefinition.LONG :
				if (cardinality > 0) {
					return XAttributeDefType.LONG_ARRAY;
				}
				if (cardinality < 0) {
					return XAttributeDefType.LONG_LIST;
				}
				return XAttributeDefType.LONG;
			case AttributeDefinition.INTEGER :
				if (cardinality > 0) {
					return XAttributeDefType.INTEGER_ARRAY;
				}
				if (cardinality < 0) {
					return XAttributeDefType.INTEGER_LIST;
				}
				return XAttributeDefType.INTEGER;
			case AttributeDefinition.CHARACTER :
				if (cardinality > 0) {
					return XAttributeDefType.CHAR_ARRAY;
				}
				if (cardinality < 0) {
					return XAttributeDefType.CHAR_LIST;
				}
				return XAttributeDefType.CHAR;
			case AttributeDefinition.DOUBLE :
				if (cardinality > 0) {
					return XAttributeDefType.DOUBLE_ARRAY;
				}
				if (cardinality < 0) {
					return XAttributeDefType.DOUBLE_LIST;
				}
				return XAttributeDefType.DOUBLE;
			case AttributeDefinition.FLOAT :
				if (cardinality > 0) {
					return XAttributeDefType.FLOAT_ARRAY;
				}
				if (cardinality < 0) {
					return XAttributeDefType.FLOAT_LIST;
				}
				return XAttributeDefType.FLOAT;
			case AttributeDefinition.BOOLEAN :
				if (cardinality > 0) {
					return XAttributeDefType.BOOLEAN_ARRAY;
				}
				if (cardinality < 0) {
					return XAttributeDefType.BOOLEAN_LIST;
				}
				return XAttributeDefType.BOOLEAN;
			case AttributeDefinition.PASSWORD :
				return XAttributeDefType.PASSWORD;
			default :
				return XAttributeDefType.STRING;
		}
	}

	private enum ConfigurationType {
		SINGLETON,
		FACTORY
	}

}
