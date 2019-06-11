package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.component.dto.ComponentDescriptionDTO;
import biz.aQute.bnd.reporter.component.dto.ReferenceDTO;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.manifest.dto.TypedAttributeValueDTO;

/**
 * This plugin allows to add all the components defined in a bundle to the
 * report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.COMPONENTS)
public class ComponentsPlugin implements ReportEntryPlugin<Jar>, Plugin {

	final private static String			IMPLEMENTATION_TAG			= "implementation";
	final private static String			SERVICE_TAG					= "service";
	final private static String			PROVIDE_TAG					= "provide";
	final private static String			PROPERTY_TAG				= "property";
	final private static String			PROPERTIES_TAG				= "properties";
	final private static String			REFERENCE_TAG				= "reference";

	final private static String			CLASS_ATTR					= "class";
	final private static String			ENABLED_ATTR				= "enabled";
	final private static String			FACTORY_ATTR				= "factory";
	final private static String			IMMEDIATE_ATTR				= "immediate";
	final private static String			CONFIGURATION_POLICY_ATTR	= "configuration-policy";
	final private static String			CONFIGURATION_PID_ATTR		= "configuration-pid";
	final private static String			ACTIVATE_ATTR				= "activate";
	final private static String			DEACTIVATE_ATTR				= "deactivate";
	final private static String			MODIFIED_ATTR				= "modified";
	final private static String			NAME_ATTR					= "name";
	final private static String			TYPE_ATTR					= "type";
	final private static String			VALUE_ATTR					= "value";
	final private static String			ENTRY_ATTR					= "entry";
	final private static String			BIND_ATTR					= "bind";
	final private static String			FIELD_ATTR					= "field";
	final private static String			FIELD_OPTION_ATTR			= "field-option";
	final private static String			CARDINALITY_ATTR			= "cardinality";
	final private static String			INTERFACE_ATTR				= "interface";
	final private static String			POLICY_ATTR					= "policy";
	final private static String			POLICY_OPTION_ATTR			= "policy-option";
	final private static String			SCOPE_ATTR					= "scope";
	final private static String			TARGET_ATTR					= "target";
	final private static String			UNBIND_ATTR					= "unbind";
	final private static String			UPDATED_ATTR				= "updated";

	private Reporter					_reporter;
	private final Map<String, String>	_properties					= new HashMap<>();

	public ComponentsPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.COMPONENTS);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Jar.class.getCanonicalName());
	}

	@Override
	public Object extract(final Jar jar, final Locale locale) throws Exception {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		final Set<String> componentPaths = new HashSet<>();
		final DocumentBuilder db = DocumentBuilderFactory.newInstance()
			.newDocumentBuilder();

		try {
			final Manifest manifest = jar.getManifest();
			if (manifest != null) {
				componentPaths.addAll(OSGiHeader.parseHeader(manifest.getMainAttributes()
					.getValue(Constants.SERVICE_COMPONENT))
					.keySet());
			}
		} catch (@SuppressWarnings("unused")
		final Exception expected) {
			// Nothing to do
		}

		final List<ComponentDescriptionDTO> comps = new LinkedList<>();
		for (final String path : componentPaths) {
			final Resource r = jar.getResource(path);
			if (r != null) {
				try (InputStream in = r.openInputStream()) {
					final ComponentDescriptionDTO comp = new ComponentDescriptionDTO();
					final Document doc = db.parse(in);
					final Element root = doc.getDocumentElement();

					final NodeList implementations = root.getElementsByTagName(IMPLEMENTATION_TAG);
					if (implementations.getLength() > 0
						&& ((Element) implementations.item(0)).hasAttribute(CLASS_ATTR)) {
						comp.implementationClass = ((Element) implementations.item(0)).getAttribute(CLASS_ATTR);
					} else {
						comp.implementationClass = "!! MISSING !!";
						_reporter.warning("the component does not declare an implementation class %s", path);
					}

					if (root.hasAttribute(NAME_ATTR)) {
						comp.name = root.getAttribute(NAME_ATTR);
					} else {
						comp.name = comp.implementationClass;
					}

					if (root.hasAttribute(ENABLED_ATTR)) {
						comp.defaultEnabled = Boolean.parseBoolean(root.getAttribute(ENABLED_ATTR));
					}

					boolean isFactory = false;
					if (root.hasAttribute(FACTORY_ATTR)) {
						comp.factory = root.getAttribute(FACTORY_ATTR);
						isFactory = true;
					}

					final NodeList services = root.getElementsByTagName(SERVICE_TAG);

					int serviceCount = 0;
					if (services.getLength() > 0) {

						final Element service = (Element) services.item(0);

						final NodeList provides = root.getElementsByTagName(PROVIDE_TAG);
						for (int i = 0; i < provides.getLength(); i++) {
							final Element provide = (Element) provides.item(i);
							if (provide.hasAttribute(INTERFACE_ATTR)) {
								comp.serviceInterfaces.add(provide.getAttribute(INTERFACE_ATTR));
								serviceCount++;
							}
						}

						if (serviceCount > 0) {
							if (service.hasAttribute(SCOPE_ATTR)) {
								comp.scope = service.getAttribute(SCOPE_ATTR);
							} else {
								comp.scope = "singleton";
							}
						}
					}

					if (root.hasAttribute(IMMEDIATE_ATTR)) {
						comp.immediate = Boolean.parseBoolean(root.getAttribute(IMMEDIATE_ATTR));
					} else {
						if (serviceCount == 0 && !isFactory) {
							comp.immediate = true;
						} else {
							comp.immediate = false;
						}
					}

					if (root.hasAttribute(CONFIGURATION_POLICY_ATTR)) {
						comp.configurationPolicy = root.getAttribute(CONFIGURATION_POLICY_ATTR);
					}

					if (root.hasAttribute(CONFIGURATION_PID_ATTR)) {
						for (final String pid : root.getAttribute(CONFIGURATION_PID_ATTR)
							.split(" ")) {
							comp.configurationPid.add(pid);
						}
					} else {
						comp.configurationPid.add(comp.name);
					}

					if (root.hasAttribute(ACTIVATE_ATTR)) {
						comp.activate = root.getAttribute(ACTIVATE_ATTR);
					}

					if (root.hasAttribute(DEACTIVATE_ATTR)) {
						comp.deactivate = root.getAttribute(DEACTIVATE_ATTR);
					}

					if (root.hasAttribute(MODIFIED_ATTR)) {
						comp.modified = root.getAttribute(MODIFIED_ATTR);
					}

					final NodeList propertiesFile = root.getElementsByTagName(PROPERTIES_TAG);

					final Set<String> propertiesName = new HashSet<>();
					for (int i = 0; i < propertiesFile.getLength(); i++) {

						final Element property = (Element) propertiesFile.item(i);
						final String pathProp = property.getAttribute(ENTRY_ATTR);

						if (pathProp.isEmpty()) {
							_reporter.warning("missing property path attribute for component %s", path);
						} else {
							final Resource rP = jar.getResource(pathProp);
							if (rP != null) {
								try (InputStream inProp = rP.openInputStream()) {
									final Properties prop = new Properties();

									prop.load(inProp);

									for (final String key : prop.stringPropertyNames()) {
										final TypedAttributeValueDTO p = new TypedAttributeValueDTO();
										p.type = "String";
										p.values.add(prop.getProperty(key));

										comp.properties.put(key, p);

										propertiesName.add(key);
									}
								}
							} else {
								_reporter.warning("cannot find property file at path %s for component %s", pathProp,
									path);
							}
						}
					}

					final NodeList properties = root.getElementsByTagName(PROPERTY_TAG);

					for (int i = 0; i < properties.getLength(); i++) {
						final TypedAttributeValueDTO p = new TypedAttributeValueDTO();

						final Element property = (Element) properties.item(i);

						String pName = null;
						if (property.hasAttribute(NAME_ATTR)) {
							pName = property.getAttribute(NAME_ATTR);
						} else {
							pName = "!! MISSING !!";
							_reporter.warning("missing property name attribute for component %s", path);
						}

						if (!propertiesName.contains(pName)) {
							if (property.hasAttribute(TYPE_ATTR)) {
								p.type = property.getAttribute(TYPE_ATTR);
							} else {
								p.type = "String";
							}

							if (property.hasAttribute(VALUE_ATTR)) {
								p.values.add(property.getAttribute(VALUE_ATTR));
							} else {
								p.multiValue = true;
								for (final String v : property.getTextContent()
									.split("\n")) {
									p.values.add(v);
								}
							}
							comp.properties.put(pName, p);
						}
					}

					final NodeList references = root.getElementsByTagName(REFERENCE_TAG);

					for (int i = 0; i < references.getLength(); i++) {

						final Element reference = (Element) references.item(i);

						final ReferenceDTO ref = new ReferenceDTO();

						if (reference.hasAttribute(BIND_ATTR)) {
							ref.bind = reference.getAttribute(BIND_ATTR);
						}

						if (reference.hasAttribute(FIELD_ATTR)) {
							ref.field = reference.getAttribute(FIELD_ATTR);
							if (reference.hasAttribute(FIELD_OPTION_ATTR)) {
								ref.fieldOption = reference.getAttribute(FIELD_OPTION_ATTR);
							} else {
								ref.fieldOption = "replace";
							}
						}

						if (reference.hasAttribute(CARDINALITY_ATTR)) {
							ref.cardinality = reference.getAttribute(CARDINALITY_ATTR);
						} else {
							ref.cardinality = "1..1";
						}

						if (reference.hasAttribute(INTERFACE_ATTR)) {
							ref.interfaceName = reference.getAttribute(INTERFACE_ATTR);
						} else {
							ref.interfaceName = "!! MISSING !!";
							_reporter.warning("missing reference interface attribute for component %s", path);
						}

						if (reference.hasAttribute(NAME_ATTR)) {
							ref.name = reference.getAttribute(NAME_ATTR);
						} else {
							ref.name = "!! MISSING !!";
							_reporter.warning("missing reference name attribute for component %s", path);
						}

						if (reference.hasAttribute(POLICY_ATTR)) {
							ref.policy = reference.getAttribute(POLICY_ATTR);
						}

						if (reference.hasAttribute(POLICY_OPTION_ATTR)) {
							ref.policyOption = reference.getAttribute(POLICY_OPTION_ATTR);
						}

						if (reference.hasAttribute(SCOPE_ATTR)) {
							ref.scope = reference.getAttribute(SCOPE_ATTR);
						}

						if (reference.hasAttribute(TARGET_ATTR)) {
							ref.target = reference.getAttribute(TARGET_ATTR);
						}

						if (reference.hasAttribute(UNBIND_ATTR)) {
							ref.unbind = reference.getAttribute(UNBIND_ATTR);
						}

						if (reference.hasAttribute(UPDATED_ATTR)) {
							ref.updated = reference.getAttribute(UPDATED_ATTR);
						}

						comp.references.add(ref);
					}
					comps.add(comp);
				}
			} else {
				if (!path.contains("*")) {
					_reporter.warning("xml component file not found at path %s", path);
				}
			}
		}
		return !comps.isEmpty() ? comps : null;
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public void setReporter(final Reporter processor) {
		_reporter = processor;
	}
}
