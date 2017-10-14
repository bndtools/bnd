package biz.aQute.bnd.reporter.plugin;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
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
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.reporter.ReportGeneratorPlugin;
import aQute.bnd.service.reporter.XmlReportPart;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ReportResult;

public class DSGeneratorPlugin implements ReportGeneratorPlugin {

	final private static String COMPONENT_TAG = "component";
	final private static String IMPLEMENTATION_TAG = "implementation";
	final private static String SERVICE_TAG = "service";
	final private static String PROVIDE_TAG = "provide";
	final private static String PROPERTY_TAG = "property";
	final private static String PROPERTIES_TAG = "properties";
	final private static String REFERENCE_TAG = "reference";

	final private static String CLASS_ATTR = "class";
	final private static String ENABLED_ATTR = "enabled";
	final private static String FACTORY_ATTR = "factory";
	final private static String IMMEDIATE_ATTR = "immediate";
	final private static String CONFIGURATION_POLICY_ATTR = "configuration-policy";
	final private static String CONFIGURATION_PID_ATTR = "configuration-pid";
	final private static String ACTIVATE_ATTR = "activate";
	final private static String DEACTIVATE_ATTR = "deactivate";
	final private static String MODIFIED_ATTR = "modified";
	final private static String NAME_ATTR = "name";
	final private static String TYPE_ATTR = "type";
	final private static String VALUE_ATTR = "value";
	final private static String ENTRY_ATTR = "entry";
	final private static String BIND_ATTR = "bind";
	final private static String FIELD_ATTR = "field";
	final private static String FIELD_OPTION_ATTR = "field-option";
	final private static String CARDINALITY_ATTR = "cardinality";
	final private static String MULT_VALUE_ATTR = "multi-value";
	final private static String INTERFACE_ATTR = "interface";
	final private static String POLICY_ATTR = "policy";
	final private static String POLICY_OPTION_ATTR = "policy-option";
	final private static String SCOPE_ATTR = "scope";
	final private static String TARGET_ATTR = "target";
	final private static String UNBIND_ATTR = "unbind";
	final private static String UPDATED_ATTR = "updated";

	@Override
	public XmlReportPart report(final Jar jar, final String locale, Reporter reporter) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		final ReportResult top = new ReportResult();

		final Set<String> componentPaths = new HashSet<>();
		DocumentBuilder db = null;

		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}

		try {
			final Manifest manifest = jar.getManifest();
			if (manifest != null) {
				componentPaths.addAll(OSGiHeader
						.parseHeader(manifest.getMainAttributes().getValue(Constants.SERVICE_COMPONENT)).keySet());
			}
		} catch (final Exception expected) {
			// Nothing to do
		}

		for (final String path : componentPaths) {
			Resource r = jar.getResource(path);
			if (r != null) {
				try (InputStream in = r.openInputStream()) {
					final Tag cTag = new Tag(COMPONENT_TAG);
					final Document doc = db.parse(in);
					final Element root = doc.getDocumentElement();

					final NodeList implementations = root.getElementsByTagName(IMPLEMENTATION_TAG);
					String implementation = null;
					if (implementations.getLength() > 0
							&& ((Element) implementations.item(0)).hasAttribute(CLASS_ATTR)) {
						implementation = ((Element) implementations.item(0)).getAttribute(CLASS_ATTR);
						cTag.addContent(new Tag(IMPLEMENTATION_TAG, implementation));
					} else {
						cTag.addContent(new Tag(IMPLEMENTATION_TAG, "!! MISSING !!"));
						reporter.warning("DS plugin: the component does not declare an implementation class: " + path);
					}

					String name = null;
					if (root.hasAttribute(NAME_ATTR)) {
						name = root.getAttribute(NAME_ATTR);
						cTag.addContent(new Tag(NAME_ATTR, name));
					} else {
						name = implementation;
						cTag.addContent(new Tag(NAME_ATTR, implementation));
					}

					if (root.hasAttribute(ENABLED_ATTR)) {
						cTag.addContent(new Tag(ENABLED_ATTR, root.getAttribute(ENABLED_ATTR)));
					} else {
						cTag.addContent(new Tag(ENABLED_ATTR, true));
					}

					boolean isFactory = false;
					if (root.hasAttribute(FACTORY_ATTR)) {
						cTag.addContent(new Tag(FACTORY_ATTR, root.getAttribute(FACTORY_ATTR)));
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
								cTag.addContent(new Tag(INTERFACE_ATTR, provide.getAttribute(INTERFACE_ATTR)));
								serviceCount++;
							}
						}

						if (serviceCount > 0) {
							if (service.hasAttribute(SCOPE_ATTR)) {
								cTag.addContent(new Tag(SCOPE_ATTR, service.getAttribute(SCOPE_ATTR)));
							} else {
								cTag.addContent(new Tag(SCOPE_ATTR, "singleton"));
							}
						}
					}

					if (root.hasAttribute(IMMEDIATE_ATTR)) {
						cTag.addContent(new Tag(IMMEDIATE_ATTR, root.getAttribute(IMMEDIATE_ATTR)));
					} else {
						if (!isFactory || serviceCount == 0) {
							cTag.addContent(new Tag(IMMEDIATE_ATTR, true));
						} else {
							cTag.addContent(new Tag(IMMEDIATE_ATTR, false));
						}
					}

					if (root.hasAttribute(CONFIGURATION_POLICY_ATTR)) {
						cTag.addContent(
								new Tag(CONFIGURATION_POLICY_ATTR, root.getAttribute(CONFIGURATION_POLICY_ATTR)));
					} else {
						cTag.addContent(new Tag(CONFIGURATION_POLICY_ATTR, "optional"));
					}

					if (root.hasAttribute(CONFIGURATION_PID_ATTR)) {
						for (final String pid : root.getAttribute(CONFIGURATION_PID_ATTR).split(" ")) {
							cTag.addContent(new Tag(CONFIGURATION_PID_ATTR, pid));
						}
					} else {
						cTag.addContent(new Tag(CONFIGURATION_PID_ATTR, name));
					}

					if (root.hasAttribute(ACTIVATE_ATTR)) {
						cTag.addContent(new Tag(ACTIVATE_ATTR, root.getAttribute(ACTIVATE_ATTR)));
					}

					if (root.hasAttribute(DEACTIVATE_ATTR)) {
						cTag.addContent(new Tag(DEACTIVATE_ATTR, root.getAttribute(DEACTIVATE_ATTR)));
					}

					if (root.hasAttribute(MODIFIED_ATTR)) {
						cTag.addContent(new Tag(MODIFIED_ATTR, root.getAttribute(MODIFIED_ATTR)));
					}

					final NodeList propertiesFile = root.getElementsByTagName(PROPERTIES_TAG);

					final Set<String> propertiesName = new HashSet<>();
					for (int i = 0; i < propertiesFile.getLength(); i++) {

						final Element property = (Element) propertiesFile.item(i);
						final String pathProp = property.getAttribute(ENTRY_ATTR);

						if (pathProp.isEmpty()) {
							reporter.warning("DS plugin: missing property path attribute for component: " + path);
						} else {
							Resource rP = jar.getResource(pathProp);
							if (rP != null) {
								try (InputStream inProp = rP.openInputStream()) {
									final Properties prop = new Properties();

									prop.load(inProp);

									for (final Entry<Object, Object> entry : prop.entrySet()) {
										final Tag pTag = new Tag(PROPERTY_TAG);

										pTag.addContent(new Tag(NAME_ATTR, (String) entry.getKey()));
										pTag.addContent(new Tag(MULT_VALUE_ATTR, false));
										pTag.addContent(new Tag(TYPE_ATTR, "String"));
										pTag.addContent(new Tag(VALUE_ATTR, (String) entry.getValue()));

										cTag.addContent(pTag);

										propertiesName.add((String) entry.getKey());
									}
								}
							} else {
								reporter.warning("DS plugin: cannot find property file at path " + pathProp
										+ " for component: " + path);
							}
						}
					}

					final NodeList properties = root.getElementsByTagName(PROPERTY_TAG);

					for (int i = 0; i < properties.getLength(); i++) {
						final Tag pTag = new Tag(PROPERTY_TAG);

						final Element property = (Element) properties.item(i);

						String pName = null;
						if (property.hasAttribute(NAME_ATTR)) {
							pName = property.getAttribute(NAME_ATTR);
						} else {
							pName = "!! MISSING !!";
							reporter.warning("DS plugin: missing property name attribute for component: " + path);
						}

						if (!propertiesName.contains(pName)) {
							pTag.addContent(new Tag(NAME_ATTR, pName));

							if (property.hasAttribute(TYPE_ATTR)) {
								pTag.addContent(new Tag(TYPE_ATTR, property.getAttribute(TYPE_ATTR)));
							} else {
								pTag.addContent(new Tag(TYPE_ATTR, "String"));
							}

							if (property.hasAttribute(VALUE_ATTR)) {
								pTag.addContent(new Tag(MULT_VALUE_ATTR, false));
								pTag.addContent(new Tag(VALUE_ATTR, property.getAttribute(VALUE_ATTR)));
							} else {
								pTag.addContent(new Tag(MULT_VALUE_ATTR, true));
								for (final String v : property.getTextContent().split("\n")) {
									pTag.addContent(new Tag(VALUE_ATTR, v));
								}
							}

							cTag.addContent(pTag);
						}
					}

					final NodeList references = root.getElementsByTagName(REFERENCE_TAG);

					for (int i = 0; i < references.getLength(); i++) {

						final Element reference = (Element) references.item(i);

						final Tag rTag = new Tag(REFERENCE_TAG);

						if (reference.hasAttribute(BIND_ATTR)) {
							rTag.addContent(new Tag(BIND_ATTR, reference.getAttribute(BIND_ATTR)));
						}

						if (reference.hasAttribute(FIELD_ATTR)) {
							rTag.addContent(new Tag(FIELD_ATTR, reference.getAttribute(FIELD_ATTR)));

							if (reference.hasAttribute(FIELD_OPTION_ATTR)) {
								rTag.addContent(new Tag(FIELD_OPTION_ATTR, reference.getAttribute(FIELD_OPTION_ATTR)));
							} else {
								rTag.addContent(new Tag(FIELD_OPTION_ATTR, "replace"));
							}
						}

						if (reference.hasAttribute(CARDINALITY_ATTR)) {
							rTag.addContent(new Tag(CARDINALITY_ATTR, reference.getAttribute(CARDINALITY_ATTR)));
						} else {
							rTag.addContent(new Tag(CARDINALITY_ATTR, "1..1"));
						}

						if (reference.hasAttribute(INTERFACE_ATTR)) {
							rTag.addContent(new Tag(INTERFACE_ATTR, reference.getAttribute(INTERFACE_ATTR)));
						} else {
							rTag.addContent(new Tag(INTERFACE_ATTR, "!! MISSING !!"));
							reporter.warning("DS plugin: missing reference interface attribute for component: " + path);
						}

						if (reference.hasAttribute(NAME_ATTR)) {
							rTag.addContent(new Tag(NAME_ATTR, reference.getAttribute(NAME_ATTR)));
						} else {
							rTag.addContent(new Tag(NAME_ATTR, "!! MISSING !!"));
							reporter.warning("DS plugin: missing reference name attribute for component: " + path);
						}

						if (reference.hasAttribute(POLICY_ATTR)) {
							rTag.addContent(new Tag(POLICY_ATTR, reference.getAttribute(POLICY_ATTR)));
						} else {
							rTag.addContent(new Tag(POLICY_ATTR, "static"));
						}

						if (reference.hasAttribute(POLICY_OPTION_ATTR)) {
							rTag.addContent(new Tag(POLICY_OPTION_ATTR, reference.getAttribute(POLICY_OPTION_ATTR)));
						} else {
							rTag.addContent(new Tag(POLICY_OPTION_ATTR, "reluctant"));
						}

						if (reference.hasAttribute(SCOPE_ATTR)) {
							rTag.addContent(new Tag(SCOPE_ATTR, reference.getAttribute(SCOPE_ATTR)));
						} else {
							rTag.addContent(new Tag(SCOPE_ATTR, "bundle"));
						}

						if (reference.hasAttribute(TARGET_ATTR)) {
							rTag.addContent(new Tag(TARGET_ATTR, reference.getAttribute(TARGET_ATTR)));
						}

						if (reference.hasAttribute(UNBIND_ATTR)) {
							rTag.addContent(new Tag(UNBIND_ATTR, reference.getAttribute(UNBIND_ATTR)));
						}

						if (reference.hasAttribute(UPDATED_ATTR)) {
							rTag.addContent(new Tag(UPDATED_ATTR, reference.getAttribute(UPDATED_ATTR)));
						}

						cTag.addContent(rTag);
					}
					top.add(cTag);

				} catch (final Exception e) {

					throw new RuntimeException(e);
				}
			} else {
				if (!path.contains("*")) {
					reporter.warning("DS plugin: xml component file not found at path " + path);
				}
			}
		}

		return top;
	}
}
