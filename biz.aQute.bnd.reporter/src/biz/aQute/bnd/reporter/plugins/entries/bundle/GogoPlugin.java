package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Clazz.MethodParameter;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ParameterAnnotation;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.lib.xml.XML;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.gogo.dto.GogoArgumentDTO;
import biz.aQute.bnd.reporter.gogo.dto.GogoFunctionDTO;
import biz.aQute.bnd.reporter.gogo.dto.GogoMethodDTO;
import biz.aQute.bnd.reporter.gogo.dto.GogoOptionDTO;
import biz.aQute.bnd.reporter.gogo.dto.GogoScopeDTO;

/**
 * This plugin allows to add all the gogo-commands defined in a bundle to the
 * report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.GOGO_COMMANDS)
public class GogoPlugin implements ReportEntryPlugin<Jar>, Plugin {

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

		final List<GogoScopeDTO> gogoCommands = new LinkedList<>();
		final DocumentBuilder db = XML.newDocumentBuilderFactory()
			.newDocumentBuilder();

		findComponentResources(jar).forEach((path, resource) -> {
			try {
				parseGogoCommands(resource, db, jar, gogoCommands);
			} catch (Exception exception) {
				_reporter.warning("Failed to extract gogo commands from component at path %s", path, exception);
			}
		});

		for (GogoScopeDTO c : gogoCommands) {
			if (!c.functions.stream()
				.allMatch(f -> f.methods.size() > 0)) {
				c.functions = c.functions.stream()
					.filter(f -> f.methods.size() > 0)
					.collect(Collectors.toList());
			}
		}

		return !gogoCommands.isEmpty() ? gogoCommands : null;
	}

	private Map<String, Resource> findComponentResources(Jar jar) {
		try {
			Manifest manifest = jar.getManifest();
			if (manifest != null) {
				return OSGiHeader.parseHeader(manifest.getMainAttributes()
					.getValue(Constants.SERVICE_COMPONENT))
					.keySet()
					.stream()
					.map(path -> {
						Resource resource = jar.getResource(path);
							if (resource != null) {
								return new AbstractMap.SimpleEntry<>(path, resource);
							} else {
								if (!path.contains("*")) {
								_reporter.warning("Xml component file not found at path %s", path);
							}
								return null;
							}
					})
					.filter(
						resource -> resource != null)
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
			} else {
				return new HashMap<>();
			}
		} catch (@SuppressWarnings("unused")
		final Exception expected) {
			return new HashMap<>();
		}
	}

	private void parseGogoCommands(Resource componentResource, DocumentBuilder db, Jar jar,
		List<GogoScopeDTO> gogoCommands) throws Exception {
		try (InputStream in = componentResource.openInputStream()) {
			extractGogoCommands(db.parse(in)
				.getDocumentElement(), jar, gogoCommands);
		}
	}

	private void extractGogoCommands(Element root, Jar jar, List<GogoScopeDTO> gogoCommands) throws Exception {
		String implementationClassPath = extractImplementationClassPath(root);
		Map<String, String> properties = extractScopeAndFunctionProperties(root, jar);
		String scope = properties.get(SCOPE);
		List<String> functions = Arrays.asList(properties.getOrDefault(FUNCTION,
			"\n")
			.split("\n"));

		if (!functions.isEmpty()) {
			GogoScopeDTO gogoScopeDTO = gogoCommands.stream()
				.filter(dto -> scope != null ? scope.equals(dto.name)
					: dto.name == null)
				.findAny()
				.orElse(null);

			if (gogoScopeDTO == null) {
				gogoScopeDTO = new GogoScopeDTO();
				gogoScopeDTO.name = scope;

				gogoCommands.add(gogoScopeDTO);
			}

			for (final String function : functions) {
				GogoFunctionDTO gogoFunctionDTO = gogoScopeDTO.functions
					.stream()
					.filter(dto -> function.equals(dto.name))
					.findAny()
					.orElse(null);

				if (gogoFunctionDTO == null) {
					gogoFunctionDTO = new GogoFunctionDTO();
					gogoFunctionDTO.name = function;

					gogoScopeDTO.functions.add(gogoFunctionDTO);
				}

				Resource implementationClass = jar.getResource(implementationClassPath);
				try (Analyzer analyzer = new Analyzer()) {
					Clazz clazz = new Clazz(analyzer, null, implementationClass);
					clazz.parseClassFile();

					gogoFunctionDTO.methods.addAll(extractMethods(clazz, function));
				}
			}
		}
	}

	private String extractImplementationClassPath(Element root) {
		final NodeList implementations = root.getElementsByTagName(IMPLEMENTATION_TAG);
		if (implementations.getLength() > 0 && ((Element) implementations.item(0)).hasAttribute(CLASS_ATTR)) {
			return ((Element) implementations.item(0)).getAttribute(CLASS_ATTR)
				.replace(".", "/") + ".class";
		} else {
			throw new RuntimeException("The component does not declare an implementation class");
		}
	}

	private Map<String, String> extractScopeAndFunctionProperties(Element root, Jar jar) throws Exception {
		Map<String, String> properties = new HashMap<>();

		final NodeList propertyElements = root.getElementsByTagName(PROPERTY_TAG);
		for (int i = 0; i < propertyElements.getLength(); i++) {

			final Element propertyElement = (Element) propertyElements.item(i);

			if (propertyElement.hasAttribute(NAME_ATTR)) {
				String propName = propertyElement.getAttribute(NAME_ATTR);

				if (propName.equals(SCOPE) || propName.equals(FUNCTION)) {
					if (propertyElement.hasAttribute(VALUE_ATTR)) {
						properties.put(propName, propertyElement.getAttribute(VALUE_ATTR));
					} else {
						properties.put(propName, propertyElement.getTextContent());
					}
				}
			}
		}

		final NodeList propertyFiles = root.getElementsByTagName(PROPERTIES_TAG);
		for (int i = 0; i < propertyFiles.getLength(); i++) {
			final Element propertyFile = (Element) propertyFiles.item(i);

			final Resource propertyFileResource = jar.getResource(propertyFile.getAttribute(ENTRY_ATTR));
			if (propertyFileResource != null) {
				try (InputStream in = propertyFileResource.openInputStream()) {
					final Properties prop = new Properties();

					prop.load(in);

					if (prop.contains(SCOPE)) {
						properties.put(SCOPE, prop.getProperty(SCOPE));
					}

					if (prop.contains(FUNCTION)) {
						properties.put(FUNCTION, prop.getProperty(FUNCTION));
					}
				}
			} else {
				throw new RuntimeException(
					"Failed to read property file at path " + propertyFile.getAttribute(ENTRY_ATTR));
			}
		}

		return properties;
	}

	private Collection<GogoMethodDTO> extractMethods(final Clazz clazz, final String function) {
		return clazz.methods()
			.filter(method -> function.equals(method.getName()))
			.map(
				this::methodToDto)
			.collect(Collectors.toList());
	}

	private GogoMethodDTO methodToDto(MethodDef method) {
		GogoMethodDTO gogoMethodDTO = new GogoMethodDTO();
		gogoMethodDTO.description = method.annotations(Descriptors.fqnToBinary(ANNOTATION_NAME_DESCRIPTOR))
			.findFirst()
			.map(a -> a.get("value")
				.toString())
			.orElse(null);

		MethodParameter[] parameterNames = method.getParameters();
		TypeRef[] parameterTypes = method
			.getDescriptor()
			.getPrototype();
		List<ParameterAnnotation> parameterAnnotations = method.parameterAnnotations(
			"*")
			.collect(Collectors.toList());

		for (int i = 0; i < parameterTypes.length; i++) {
			boolean multiValue = parameterTypes[i].getShortName() != null && parameterTypes[i]
				.getShortName()
				.matches(".*]") ? true : false;

			String name = null;
			if (parameterNames != null && i < parameterNames.length) {
				name = parameterNames[i].getName();
			}

			int paramIndice = i;

			String description = parameterAnnotations.stream()
				.filter(a -> a.parameter() == paramIndice && ANNOTATION_NAME_DESCRIPTOR.equals(a
					.getName()
				.toString()))
				.findAny()
				.map(a -> a.get("value")
					.toString())
				.orElse(null);

			ParameterAnnotation paramAnnotation = parameterAnnotations.stream()
				.filter(a -> a.parameter() == paramIndice && ANNOTATION_NAME_PARAMETER.equals(a
					.getName()
				.toString()))
				.findAny()
				.orElse(null);

			if (paramAnnotation == null) {
				GogoArgumentDTO gogoArgumentDTO = new GogoArgumentDTO();
				gogoArgumentDTO.name = name;
				gogoArgumentDTO.description = description;
				gogoArgumentDTO.multiValue = multiValue;

				gogoMethodDTO.arguments.add(gogoArgumentDTO);
			} else {
				GogoOptionDTO gogoOptionDTO = new GogoOptionDTO();
				gogoOptionDTO.names = Stream.of((Object[]) paramAnnotation.get(
					"names"))
					.map(o -> o.toString())
					.collect(Collectors.toList());
				gogoOptionDTO.description = description;
				gogoOptionDTO.multiValue = multiValue;

				if (paramAnnotation.get("presentValue") != null) {
					gogoOptionDTO.isFlag = true;
				}

				gogoMethodDTO.options.add(gogoOptionDTO);
			}
		}

		if (gogoMethodDTO.arguments.size() == 1 && gogoMethodDTO.arguments.get(0).name == null) {
			gogoMethodDTO.arguments.get(0).name = "arg";
		}

		for (int i = 0; i < gogoMethodDTO.arguments.size(); i++) {
			if (gogoMethodDTO.arguments.get(i).name == null) {
				gogoMethodDTO.arguments.get(i).name = "arg" + (i + 1);
			}
		}

		return gogoMethodDTO;
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
