package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodParameter;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.Descriptor;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ParameterAnnotation;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.gogo.dto.GogoFunctionDTO;
import biz.aQute.bnd.reporter.gogo.dto.GogoMethodDTO;
import biz.aQute.bnd.reporter.gogo.dto.GogoParameterDTO;
import biz.aQute.bnd.reporter.gogo.dto.GogoScopeDTO;

/**
 * This plugin allows to add all the gogo-commands defined in a bundle to the
 * report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.GOGO_COMMANDS)
public class GogoPlugin implements ReportEntryPlugin<Jar>, Plugin {

	// TODO: use Constants from ComponentsPlugin.java?
	final private static String			IMPLEMENTATION_TAG			= "implementation";

	final private static String			CLASS_ATTR					= "class";
	final private static String			PROPERTY_TAG				= "property";
	final private static String			PROPERTIES_TAG				= "properties";

	final private static String			NAME_ATTR					= "name";
	final private static String			ENTRY_ATTR					= "entry";

	final private static String			VALUE_ATTR					= "value";

	final private static String			SCOPE						= "osgi.command.scope";
	final private static String			FUNCTION					= "osgi.command.function";

	final private static String			ANNOTATION_NAME_DESCRIPTOR	= "org.apache.felix.service.command.Descriptor";

	final private static String			ANNOTATION_NAME_PARAMETER	= "org.apache.felix.service.command.Parameter";

	private Reporter					_reporter;
	private final Map<String, String>	_properties					= new HashMap<>();

	public GogoPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.GOGO_COMMANDS);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Jar.class.getCanonicalName());
	}

	@Override
	public List<GogoScopeDTO> extract(final Jar jar, final Locale locale) throws Exception {
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

		final List<GogoScopeDTO> gogos = new LinkedList<>();
		try (Analyzer analyzer = new Analyzer()) {

			for (final String path : componentPaths) {

				final Resource r = jar.getResource(path);
				if (r != null) {
					try (InputStream in = r.openInputStream()) {

						final Document doc = db.parse(in);
						final Element root = doc.getDocumentElement();

						String implementationClass;

						final NodeList implementations = root.getElementsByTagName(IMPLEMENTATION_TAG);
						if (implementations.getLength() > 0
							&& ((Element) implementations.item(0)).hasAttribute(CLASS_ATTR)) {
							implementationClass = ((Element) implementations.item(0)).getAttribute(CLASS_ATTR);
						} else {

							_reporter.warning("the component does not declare an implementation class %s", path);
							continue;
						}

						final NodeList propertiesFile = root.getElementsByTagName(PROPERTIES_TAG);

						final Set<String> propertiesName = new HashSet<>();
						List<String> functions = null;
						String scope = null;
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

										if (prop.contains(SCOPE)) {

											scope = prop.getProperty(SCOPE);
											propertiesName.add(SCOPE);

										}

										if (prop.contains(FUNCTION)) {
											functions = Arrays.asList(prop.getProperty(FUNCTION));
											propertiesName.add(FUNCTION);
										}
									}
								}
							}
						}

						final NodeList properties = root.getElementsByTagName(PROPERTY_TAG);

						for (int i = 0; i < properties.getLength(); i++) {

							final Element property = (Element) properties.item(i);

							String pName = null;
							if (property.hasAttribute(NAME_ATTR)) {
								pName = property.getAttribute(NAME_ATTR);
							} else {
								pName = "!! MISSING !!";
								_reporter.warning("missing property name attribute for component %s", path);
							}

							if (!pName.equals(SCOPE) && !pName.equals(FUNCTION)) {
								continue;

							}
							if (!propertiesName.contains(pName)) {

								if (property.hasAttribute(VALUE_ATTR)) {
									String val = property.getAttribute(VALUE_ATTR);

									if (SCOPE.equals(pName)) {
										scope = val;
									}
									if (FUNCTION.equals(pName)) {
										functions = Arrays.asList(val);
									}
								} else {

									final String[] vals = property.getTextContent()
										.split("\n");

									if (SCOPE.equals(pName)) {
										if (vals.length == 1) {
											scope = vals[0];
										} else {

											_reporter.warning(
												"More than one gogo scope set as property name attribute for component %s",
												path);
										}

									}
									if (FUNCTION.equals(pName)) {
										functions = Arrays.asList(vals);
									}

								}
							}

						}
						if (scope == null || functions == null || functions.isEmpty()) {

							continue;
						}
						final String fScope = scope;
						Optional<GogoScopeDTO> optional = gogos.stream()
							.filter(dto -> fScope.equals(dto.title))
							.findFirst();

						GogoScopeDTO gogoScopeDTO = null;
						if (optional.isPresent()) {
							gogoScopeDTO = optional.get();

						} else {
							gogoScopeDTO = new GogoScopeDTO();
							gogos.add(gogoScopeDTO);
							gogoScopeDTO.title = scope;
							gogoScopeDTO.functions = new ArrayList<GogoFunctionDTO>();

						}

						for (final String function : functions) {

							Optional<GogoFunctionDTO> optionalF = gogoScopeDTO.functions.stream()
								.filter(dto -> function.equals(dto.title))
								.findFirst();

							GogoFunctionDTO gogoFunctionDTO = null;

							if (optionalF.isPresent()) {
								gogoFunctionDTO = optionalF.get();
							} else {
								gogoFunctionDTO = new GogoFunctionDTO();
								gogoScopeDTO.functions.add(gogoFunctionDTO);
								gogoFunctionDTO.title = function;
								gogoFunctionDTO.methods = new ArrayList<GogoMethodDTO>();
							}
							String pathClazz = implementationClass.replace(".", "/") + ".class";
							Resource resClazz = jar.getResource(pathClazz);
							Clazz clazz = new Clazz(analyzer, null, resClazz);
							clazz.parseClassFile();
							gogoFunctionDTO.methods.addAll(createMethodDTO(clazz, function));

						}
					}
				} else {
					if (!path.contains("*")) {
						_reporter.warning("xml component file not found at path %s", path);
					}
				}

			}
		}

		return !gogos.isEmpty() ? gogos : null;
	}

	private Collection<GogoMethodDTO> createMethodDTO(final Clazz clazz, final String function) throws Exception {

		return clazz.methods()
			.filter(m -> function.equals(m.getName()))
			.map(m -> {

				GogoMethodDTO gogoMethodDTO = new GogoMethodDTO();
				gogoMethodDTO.title = m.getName();
				gogoMethodDTO.parameters = Lists.newArrayList();

				Optional<Annotation> oDescriptorAnnotation = m

					.annotations(Descriptors.fqnToBinary(ANNOTATION_NAME_DESCRIPTOR))
					.findFirst();

				if (oDescriptorAnnotation.isPresent()) {

					gogoMethodDTO.description = oDescriptorAnnotation.get()
						.get("value");
				}

				Descriptor d = m.getDescriptor();
				TypeRef[] trs = d.getPrototype();

				MethodParameter[] mps = m.getParameters();

				List<ParameterAnnotation> methodParameters = m.parameterAnnotations("*")
					.collect(Collectors.toList());

				for (int i = 0; i < trs.length; i++) {

					GogoParameterDTO gogoParameterDTO = new GogoParameterDTO();
					TypeRef tr = trs[i];

					gogoParameterDTO.type = tr.getShortName();
					if (mps != null && i < mps.length) {
						gogoParameterDTO.title = mps[i].getName();
					}

					for (ParameterAnnotation parameterAnnotation : methodParameters) {

						if (parameterAnnotation.parameter() == i) {

							if (ANNOTATION_NAME_DESCRIPTOR.equals(parameterAnnotation.getName()
								.toString())) {

								gogoParameterDTO.description = parameterAnnotation.get("value");
							} else if (ANNOTATION_NAME_PARAMETER.equals(parameterAnnotation.getName()
								.toString())) {

								gogoParameterDTO.presentValue = parameterAnnotation.get("presentValue");

								for (Entry<String, Object> entry : parameterAnnotation.entrySet()) {

									if (entry.getKey()
										.equals("absentValue")) {
										gogoParameterDTO.absentValue = (String) entry.getValue();

									} else if (entry.getKey()
										.equals("presentValue")) {
										gogoParameterDTO.absentValue = (String) entry.getValue();

									} else if (entry.getKey()
										.equals("names")) {

										gogoParameterDTO.names = Stream.of((Object[]) entry.getValue())
											.map(o -> o.toString())
											.collect(Collectors.toList());

									}
								}

							}
						}

					}
					gogoMethodDTO.parameters.add(gogoParameterDTO);

				}

				return gogoMethodDTO;
			})
			.collect(Collectors.toList());
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
