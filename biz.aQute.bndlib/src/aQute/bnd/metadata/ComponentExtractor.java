package aQute.bnd.metadata;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.metadata.dto.ComponentDescriptionDTO;
import aQute.bnd.metadata.dto.ReferenceDTO;
import aQute.bnd.metadata.dto.TypedPropertyDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

class ComponentExtractor extends MetadataExtractor {

	final private static String	IMPLEMENTATION_TAG			= "implementation";
	final private static String	SERVICE_TAG					= "service";
	final private static String	PROVIDE_TAG					= "provide";
	final private static String	PROPERTY_TAG				= "property";
	final private static String	PROPERTIES_TAG				= "properties";
	final private static String	REFERENCE_TAG				= "reference";

	final private static String	CLASS_ATTR					= "class";
	final private static String	ENABLED_ATTR				= "enabled";
	final private static String	FACTORY_ATTR				= "factory";
	final private static String	IMMEDIATE_ATTR				= "immediate";
	final private static String	CONFIGURATION_POLICY_ATTR	= "configuration-policy";
	final private static String	CONFIGURATION_PID_ATTR		= "configuration-pid";
	final private static String	ACTIVATE_ATTR				= "activate";
	final private static String	DEACTIVATE_ATTR				= "deactivate";
	final private static String	MODIFIED_ATTR				= "modified";
	final private static String	NAME_ATTR					= "name";
	final private static String	TYPE_ATTR					= "type";
	final private static String	VALUE_ATTR					= "value";
	final private static String	ENTRY_ATTR					= "entry";
	final private static String	BIND_ATTR					= "bind";
	final private static String	FIELD_ATTR					= "field";
	final private static String	FIELD_OPTION_ATTR			= "field-option";
	final private static String	CARDINALITY_ATTR			= "cardinality";
	final private static String	INTERFACE_ATTR				= "interface";
	final private static String	POLICY_ATTR					= "policy";
	final private static String	POLICY_OPTION_ATTR			= "policy-option";
	final private static String	SCOPE_ATTR					= "scope";
	final private static String	TARGET_ATTR					= "target";
	final private static String	UNBIND_ATTR					= "unbind";
	final private static String	UPDATED_ATTR				= "updated";

	@Override
	public void extract(BundleMetadataDTO dto, Jar jar) {

		Set<String> componentPaths = new HashSet<>();
		DocumentBuilder db = null;

		try {
			
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		} catch (ParserConfigurationException e) {

			throw new RuntimeException(e);

		}
		
		try {

			Manifest manifest = jar.getManifest();

			if (manifest != null) {

				componentPaths.addAll(
						OSGiHeader.parseHeader(manifest.getMainAttributes().getValue(Constants.SERVICE_COMPONENT))
								.keySet());
			}

		} catch (Exception expected) {

			// Nothing to do
		}

		dto.components = new LinkedList<>();

		for (String path : componentPaths) {

			try (InputStream in = jar.getResource(path).openInputStream()) {

				ComponentDescriptionDTO cdto = new ComponentDescriptionDTO();
				Document doc = db.parse(in);

				Element root = doc.getDocumentElement();

				NodeList implementations = root.getElementsByTagName(IMPLEMENTATION_TAG);

				if (implementations.getLength() > 0) {

					if (((Element) implementations.item(0)).hasAttribute(CLASS_ATTR)) {

						cdto.implementationClass = ((Element) implementations.item(0)).getAttribute(CLASS_ATTR);
					}
				}

				if (root.hasAttribute(NAME_ATTR)) {

					cdto.name = root.getAttribute(NAME_ATTR);

				} else {

					cdto.name = cdto.implementationClass;
				}

				if (root.hasAttribute(ENABLED_ATTR)) {

					cdto.defaultEnabled = Boolean.parseBoolean(root.getAttribute(ENABLED_ATTR));

				} else {

					cdto.defaultEnabled = true;
				}

				if (root.hasAttribute(FACTORY_ATTR)) {

					cdto.factory = root.getAttribute(FACTORY_ATTR);
				}

				cdto.serviceInterfaces = new LinkedList<>();

				NodeList services = root.getElementsByTagName(SERVICE_TAG);

				if (services.getLength() > 0) {

					Element service = (Element) services.item(0);

					NodeList provides = root.getElementsByTagName(PROVIDE_TAG);

					for (int i = 0; i < provides.getLength(); i++) {

						Element provide = (Element) provides.item(i);
						cdto.serviceInterfaces.add(provide.getAttribute(INTERFACE_ATTR));
					}

					if (!cdto.serviceInterfaces.isEmpty()) {

						if (service.hasAttribute(SCOPE_ATTR)) {

							cdto.scope = service.getAttribute(SCOPE_ATTR);

						} else {

							cdto.scope = "singleton";
						}
					}
				}

				if (root.hasAttribute(IMMEDIATE_ATTR)) {

					cdto.immediate = Boolean.parseBoolean(root.getAttribute(IMMEDIATE_ATTR));

				} else {

					if (cdto.factory != null || cdto.serviceInterfaces.isEmpty()) {

						cdto.immediate = true;

					} else {

						cdto.immediate = false;
					}
				}

				if (root.hasAttribute(CONFIGURATION_POLICY_ATTR)) {

					cdto.configurationPolicy = root.getAttribute(CONFIGURATION_POLICY_ATTR);

				} else {

					cdto.configurationPolicy = "optional";
				}

				cdto.configurationPid = new LinkedList<>();

				if (root.hasAttribute(CONFIGURATION_PID_ATTR)) {

					cdto.configurationPid.addAll(Arrays.asList(root.getAttribute(CONFIGURATION_PID_ATTR).split(" ")));

				} else {

					if (cdto.name != null) {

						cdto.configurationPid.add(cdto.name);
					}
				}

				if (root.hasAttribute(ACTIVATE_ATTR)) {

					cdto.activate = root.getAttribute(ACTIVATE_ATTR);
				}

				if (root.hasAttribute(DEACTIVATE_ATTR)) {

					cdto.deactivate = root.getAttribute(DEACTIVATE_ATTR);
				}

				if (root.hasAttribute(MODIFIED_ATTR)) {

					cdto.modified = root.getAttribute(MODIFIED_ATTR);
				}

				cdto.properties = new LinkedList<>();

				NodeList properties = root.getElementsByTagName(PROPERTY_TAG);

				for (int i = 0; i < properties.getLength(); i++) {

					TypedPropertyDTO prop = new TypedPropertyDTO();

					Element property = (Element) properties.item(i);

					if (property.hasAttribute(NAME_ATTR)) {

						prop.name = property.getAttribute(NAME_ATTR);
					}

					if (property.hasAttribute(TYPE_ATTR)) {

						prop.type = property.getAttribute(TYPE_ATTR);

					} else {

						prop.type = "String";
					}

					prop.values = new LinkedList<>();

					String value = property.getAttribute(VALUE_ATTR);

					if (property.hasAttribute(VALUE_ATTR)) {

						prop.multipleValues = false;
						prop.values.add(property.getAttribute(VALUE_ATTR));

					} else {

						prop.multipleValues = true;
						prop.values.addAll(Arrays.asList(property.getTextContent().split("\n")));
					}

					cdto.properties.add(prop);
				}

				NodeList propertiesFile = root.getElementsByTagName(PROPERTIES_TAG);

				for (int i = 0; i < propertiesFile.getLength(); i++) {

					Element property = (Element) propertiesFile.item(i);
					String pathProp = property.getAttribute(ENTRY_ATTR);

					try (InputStream inProp = jar.getResource(pathProp).openInputStream()) {

						Properties prop = new Properties();

						prop.load(inProp);

						for (Entry<Object,Object> entry : prop.entrySet()) {

							TypedPropertyDTO propDto = null;

							for (int j = 0; propDto == null && j < cdto.properties.size(); j++) {

								if (entry.getKey().equals(cdto.properties.get(j).name)) {

									propDto = cdto.properties.get(j);
								}
							}

							if (propDto != null) {

								cdto.properties.remove(propDto);

							} else {

								propDto = new TypedPropertyDTO();
							}

							propDto.name = (String) entry.getKey();
							propDto.type = "String";
							propDto.multipleValues = false;
							propDto.values = new LinkedList<>();

							propDto.values.add((String) entry.getValue());

							cdto.properties.add(propDto);
						}
					}
				}

				cdto.references = new LinkedList<>();

				NodeList references = root.getElementsByTagName(REFERENCE_TAG);

				for (int i = 0; i < references.getLength(); i++) {

					Element reference = (Element) references.item(i);

					ReferenceDTO rdto = new ReferenceDTO();

					if (reference.hasAttribute(BIND_ATTR)) {

						rdto.bind = reference.getAttribute(BIND_ATTR);

					}

					if (reference.hasAttribute(FIELD_ATTR)) {

						rdto.field = reference.getAttribute(FIELD_ATTR);

						if (reference.hasAttribute(FIELD_OPTION_ATTR)) {

							rdto.fieldOption = reference.getAttribute(FIELD_OPTION_ATTR);

						} else {

							rdto.fieldOption = "replace";
						}
					}

					if (reference.hasAttribute(CARDINALITY_ATTR)) {

						rdto.cardinality = reference.getAttribute(CARDINALITY_ATTR);

					} else {

						rdto.cardinality = "1..1";
					}

					if (reference.hasAttribute(INTERFACE_ATTR)) {

						rdto.interfaceName = reference.getAttribute(INTERFACE_ATTR);
					}

					if (reference.hasAttribute(NAME_ATTR)) {

						rdto.name = reference.getAttribute(NAME_ATTR);
					}

					if (reference.hasAttribute(POLICY_ATTR)) {

						rdto.policy = reference.getAttribute(POLICY_ATTR);

					} else {

						rdto.policy = "static";
					}

					if (reference.hasAttribute(POLICY_OPTION_ATTR)) {

						rdto.policyOption = reference.getAttribute(POLICY_OPTION_ATTR);

					} else {

						rdto.policyOption = "reluctant";
					}

					if (reference.hasAttribute(SCOPE_ATTR)) {

						rdto.scope = reference.getAttribute(SCOPE_ATTR);

					} else {

						rdto.scope = "bundle";
					}

					if (reference.hasAttribute(TARGET_ATTR)) {

						rdto.target = reference.getAttribute(TARGET_ATTR);
					}

					if (reference.hasAttribute(UNBIND_ATTR)) {

						rdto.unbind = reference.getAttribute(UNBIND_ATTR);
					}

					if (reference.hasAttribute(UPDATED_ATTR)) {

						rdto.updated = reference.getAttribute(UPDATED_ATTR);
					}

					cdto.references.add(rdto);
				}
				dto.components.add(cdto);

			} catch (Exception e) {

				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void verify(BundleMetadataDTO dto) throws Exception {
		dto.components = replaceNull(dto.components);

		for (ComponentDescriptionDTO e : dto.components) {

			if (e.implementationClass == null) {

				error("the component does not declare an implementation class: component index = "
						+ dto.components.indexOf(e));
			}

			if (e.name == null) {

				error("the component does not declare a name: component index = " + dto.components.indexOf(e));
			}

			e.configurationPid = replaceNull(e.configurationPid);

			if (e.configurationPid.isEmpty()) {

				error("the component does not declare pids: component index = " + dto.components.indexOf(e));
			}

			if (e.configurationPolicy == null) {

				error("the component does not declare a configuration policy: component index = "
						+ dto.components.indexOf(e));
			}

			if (e.defaultEnabled == null) {

				error("the component does not declare if it is enabled: component index = "
						+ dto.components.indexOf(e));
			}

			if (e.immediate == null) {

				error("the component does not declare if it is immediate: component index = "
						+ dto.components.indexOf(e));
			}

			e.serviceInterfaces = replaceNull(e.serviceInterfaces);

			if (!e.serviceInterfaces.isEmpty() && e.scope == null) {

				error("the component does not declare a service scope: component index = " + dto.components.indexOf(e));
			}

			e.references = replaceNull(e.references);

			for (ReferenceDTO r : e.references) {

				if (r.interfaceName == null) {

					error("the reference does not declare an interface: reference index = " + e.references.indexOf(r)
							+ ", component index = " + dto.components.indexOf(e));
				}

				if (r.name == null) {

					error("the reference does not declare a name: reference index = " + e.references.indexOf(r)
							+ ", component index = " + dto.components.indexOf(e));
				}

				if (r.cardinality == null) {

					error("the reference does not declare a cardinality: reference index = " + e.references.indexOf(r)
							+ ", component index = " + dto.components.indexOf(e));
				}

				if (r.policy == null) {

					error("the reference does not declare a policy: reference index = " + e.references.indexOf(r)
							+ ", component index = " + dto.components.indexOf(e));
				}

				if (r.policyOption == null) {

					error("the reference does not declare a policy option: reference index = " + e.references.indexOf(r)
							+ ", component index = " + dto.components.indexOf(e));
				}

				if (r.scope == null) {

					error("the reference does not declare a scope: reference index = " + e.references.indexOf(r)
							+ ", component index = " + dto.components.indexOf(e));
				}

				if (r.field != null && r.fieldOption == null) {

					error("the reference does not declare a field option: reference index = " + e.references.indexOf(r)
							+ ", component index = " + dto.components.indexOf(e));
				}
			}

			e.properties = replaceNull(e.properties);

			for (TypedPropertyDTO p : e.properties) {

				if (p.name == null) {

					error("the property does not declare a name: property index = " + e.properties.indexOf(p)
							+ ", component index = " + dto.components.indexOf(e));
				}

				if (p.multipleValues == null) {

					error("the property does not declare a cardinality: property index = " + e.properties.indexOf(p)
							+ ", component index = " + dto.components.indexOf(e));
				}

				if (p.type == null) {

					error("the property does not declare a type: property index = " + e.properties.indexOf(p)
							+ ", component index = " + dto.components.indexOf(e));
				}

				p.values = replaceNull(p.values);

				if (!p.multipleValues && p.values.size() > 1) {

					error("the property has an invalid number of values: property index = " + e.properties.indexOf(p)
							+ ", component index = " + dto.components.indexOf(e));
				}
			}
		}
	}
}