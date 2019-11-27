package aQute.bnd.plugin.jpms;

import static aQute.bnd.classfile.ModuleAttribute.ACC_OPEN;
import static aQute.bnd.osgi.Constants.ACCESS_ATTRIBUTE;
import static aQute.bnd.osgi.Constants.AUTOMATIC_MODULE_NAME;
import static aQute.bnd.osgi.Constants.DYNAMICIMPORT_PACKAGE;
import static aQute.bnd.osgi.Constants.EXPORTS_ATTRIBUTE;
import static aQute.bnd.osgi.Constants.IGNORE_ATTRIBUTE;
import static aQute.bnd.osgi.Constants.INTERNAL_EXPORT_TO_MODULES_DIRECTIVE;
import static aQute.bnd.osgi.Constants.INTERNAL_MODULE_VERSION_DIRECTIVE;
import static aQute.bnd.osgi.Constants.INTERNAL_OPEN_TO_MODULES_DIRECTIVE;
import static aQute.bnd.osgi.Constants.JPMS_MODULE_INFO;
import static aQute.bnd.osgi.Constants.JPMS_MODULE_INFO_OPTIONS;
import static aQute.bnd.osgi.Constants.MAIN_CLASS;
import static aQute.bnd.osgi.Constants.MODULES_ATTRIBUTE;
import static aQute.bnd.osgi.Constants.MODULE_INFO_CLASS;
import static aQute.bnd.osgi.Constants.OPTIONAL;
import static aQute.bnd.osgi.Constants.PROVIDE_CAPABILITY;
import static aQute.bnd.osgi.Constants.REQUIRE_CAPABILITY;
import static aQute.bnd.osgi.Constants.RESOLUTION_DIRECTIVE;
import static aQute.bnd.osgi.Constants.SERVICELOADER_NAMESPACE;
import static aQute.bnd.osgi.Constants.SERVICELOADER_REGISTER_DIRECTIVE;
import static aQute.bnd.osgi.Constants.STATIC_ATTRIBUTE;
import static aQute.bnd.osgi.Constants.SUBSTITUTE_ATTRIBUTE;
import static aQute.bnd.osgi.Constants.TRANSITIVE_ATTRIBUTE;
import static aQute.bnd.osgi.Constants.USES_DIRECTIVE;
import static aQute.bnd.osgi.Constants.VERSION_ATTRIBUTE;
import static aQute.bnd.osgi.Processor.isTrue;
import static aQute.bnd.osgi.Processor.removeDuplicateMarker;
import static aQute.lib.strings.Strings.splitAsStream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.model.EE;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.classfile.builder.ModuleInfoBuilder;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.verifier.VerifierPlugin;
import aQute.bnd.stream.MapStream;
import aQute.lib.io.ByteBufferDataOutput;
import aQute.lib.strings.Strings;

/**
 * A plugin to generate a module-info class from analyzer metadata and bundle
 * annotations.
 */
public class JPMSModuleInfoPlugin implements VerifierPlugin {

	private static final Logger		logger						= LoggerFactory.getLogger(JPMSModuleInfoPlugin.class);

	private static final Pattern	mangledModuleName			= Pattern.compile("(.*)-\\d.*");

	private static final EE			DEFAULT_MODULE_EE			= EE.JavaSE_11_0;
	private static final String		INTERNAL_MODULE_DIRECTIVE	= "-internal-module:";
	private static final String		WEB_INF						= "WEB-INF";

	@Override
	public void verify(final Analyzer analyzer) throws Exception {
		String moduleProperty = analyzer.getProperty(JPMS_MODULE_INFO);
		if (moduleProperty == null)
			return;

		Parameters provideCapabilities = new Parameters(analyzer.getJar()
			.getManifest()
			.getMainAttributes()
			.getValue(PROVIDE_CAPABILITY));
		Parameters requireCapabilities = new Parameters(analyzer.getJar()
			.getManifest()
			.getMainAttributes()
			.getValue(REQUIRE_CAPABILITY));

		if (moduleProperty.isEmpty()) {
			String name = name(analyzer);
			int access = access(requireCapabilities);
			String version = analyzer.getVersion();

			moduleProperty = String.format("%s;access=%s;version=%s", name, access, version);
		}

		Parameters moduleParameters = OSGiHeader.parseHeader(moduleProperty);
		if (moduleParameters.isEmpty())
			return;

		if (moduleParameters.size() > 1)
			throw new IllegalArgumentException("Only one -module instruction is allowed:" + moduleParameters);

		Entry<String, Attrs> moduleInstructions = moduleParameters.stream()
			.findFirst()
			.get();

		Parameters moduleInfoOptions = OSGiHeader.parseHeader(analyzer.getProperty(JPMS_MODULE_INFO_OPTIONS));

		Packages index = new Packages();

		// Index the whole class path
		for (Jar jar : analyzer.getClasspath()) {
			String moduleName = getModuleName(analyzer, jar, moduleInfoOptions);
			String moduleVersion = jar.getModuleVersion();
			Attrs attrs = new Attrs();
			if (moduleName != null) {
				attrs.put(INTERNAL_MODULE_DIRECTIVE, moduleName);
			}
			if (moduleVersion != null) {
				attrs.put(INTERNAL_MODULE_VERSION_DIRECTIVE, moduleVersion);
			}
			MapStream.of(jar.getDirectories())
				.filter((k, v) -> (v != null) && !v.isEmpty() && !k.isEmpty())
				.keys()
				.map(analyzer::getPackageRef)
				.filter(ref -> !ref.isMetaData() && !ref.getPath()
					.startsWith(WEB_INF))
				.forEach(ref -> index.put(ref, new Attrs(attrs)));
		}

		ModuleInfoBuilder builder = nameAccessAndVersion(moduleInstructions, requireCapabilities, analyzer);

		requires(moduleInstructions, analyzer, index, moduleInfoOptions, builder);
		exportPackages(analyzer, builder);
		openPackages(analyzer, builder);
		serviceLoaderProviders(provideCapabilities, analyzer, builder);
		serviceLoaderUses(requireCapabilities, analyzer, builder);
		mainClass(analyzer, builder);

		// TODO use annotations to store other header info???
		// AnnotationVisitor visitAnnotation = classWriter.visitAnnotation(...,
		// false);

		ByteBufferDataOutput bbout = new ByteBufferDataOutput();
		builder.build()
			.write(bbout);

		analyzer.getJar()
			.putResource(MODULE_INFO_CLASS, new EmbeddedResource(bbout.toByteBuffer(), analyzer.lastModified()));
	}

	private String getModuleName(Analyzer analyzer, Jar jar, Parameters moduleInfoOptions) throws Exception {
		String moduleName = jar.getModuleName();
		if (moduleName == null) {
			if (jar.getSource() != null && jar.getSource()
				.isDirectory()) {
				return null;
			}
			moduleName = jar.getName();
			Matcher matcher = mangledModuleName.matcher(moduleName);
			if (matcher.matches()) {
				moduleName = matcher.group(1);
			}
			final String name = moduleName;
			moduleName = moduleInfoOptions.stream()
				.filterValue(attrs -> name.equals(attrs.get(SUBSTITUTE_ATTRIBUTE)))
				.keys()
				.findFirst()
				.orElse(moduleName);

			if (logger.isWarnEnabled())
				logger.warn("Using module name '{}' for: {}", moduleName, jar);
		}
		return moduleName;
	}

	private int access(Parameters requireCapabilities) {
		return requireCapabilities.stream()
			.filterKey(key -> removeDuplicateMarker(key).equals(EXTENDER_NAMESPACE))
			.mapToInt((k, v) -> ACC_OPEN)
			.findAny()
			.orElse(0);
	}

	private String name(Analyzer analyzer) {
		return analyzer.getProperty(AUTOMATIC_MODULE_NAME, analyzer.getBsn());
	}

	private void exportPackages(Analyzer analyzer, ModuleInfoBuilder builder) {
		Packages contained = analyzer.getContained();

		analyzer.getExports()
			.forEach((packageRef, attrs) -> {
				Set<String> targets = Collections.emptySet();

				Attrs containedAttrs = contained.get(packageRef);
				if (containedAttrs != null && containedAttrs.containsKey(INTERNAL_EXPORT_TO_MODULES_DIRECTIVE)) {
					targets = splitAsStream(containedAttrs.get(INTERNAL_EXPORT_TO_MODULES_DIRECTIVE))
						.collect(toCollection(LinkedHashSet::new));
				}

				// TODO Do we want to handle access? I can't think of a reason.
				// Allowed: 0 | ACC_SYNTHETIC | ACC_MANDATED

				builder.exports(packageRef.getBinary(), 0, targets);
			});
	}

	private void mainClass(Analyzer analyzer, ModuleInfoBuilder builder) {
		String mainClass = analyzer.getProperty(MAIN_CLASS);

		if (mainClass != null) {
			TypeRef typeRef = analyzer.getTypeRefFromFQN(mainClass);

			builder.mainClass(typeRef.getBinary());
		}
	}

	private ModuleInfoBuilder nameAccessAndVersion(Entry<String, Attrs> instruction, Parameters requireCapability,
		Analyzer analyzer) {
		Attrs attrs = instruction.getValue();

		String name = instruction.getKey();
		// Allowed: 0 | ACC_OPEN | ACC_SYNTHETIC | ACC_MANDATED
		String access = attrs.computeIfAbsent(ACCESS_ATTRIBUTE, k -> String.valueOf(access(requireCapability)));
		String version = attrs.computeIfAbsent(VERSION_ATTRIBUTE, k -> analyzer.getVersion());

		ModuleInfoBuilder builder = new ModuleInfoBuilder().module_name(name)
			.module_version(version)
			.module_flags(Integer.parseInt(access));
		return builder;
	}

	private void openPackages(Analyzer analyzer, ModuleInfoBuilder builder) {
		analyzer.getContained()
			.stream()
			.filterValue(attrs -> attrs.containsKey(INTERNAL_OPEN_TO_MODULES_DIRECTIVE))
			.forEach((packageRef, attrs) -> {
				Set<String> targets = splitAsStream(attrs.get(INTERNAL_OPEN_TO_MODULES_DIRECTIVE))
					.collect(toCollection(LinkedHashSet::new));

				// TODO Do we want to handle access? I can't think of a reason.
				// Allowed: 0 | ACC_SYNTHETIC | ACC_MANDATED

				builder.opens(packageRef.getBinary(), 0, targets);
			});
	}

	private void requires(Entry<String, Attrs> instruction, Analyzer analyzer, Packages index,
		Parameters moduleInfoOptions, ModuleInfoBuilder builder) throws Exception {

		String eeAttribute = instruction.getValue()
			.get(Constants.EE_ATTRIBUTE);

		EE moduleEE = (eeAttribute != null) ? EE.valueOf(eeAttribute) : DEFAULT_MODULE_EE;

		Packages exports = analyzer.getExports();
		Packages imports = analyzer.getImports();
		Packages referred = analyzer.getReferred();

		Instructions dynamicImportPackages = new Instructions(new Parameters(analyzer.getJar()
			.getManifest()
			.getMainAttributes()
			.getValue(DYNAMICIMPORT_PACKAGE)));

		Packages externallyReferred = new Packages(referred);
		exports.keySet()
			.forEach(externallyReferred::remove);

		Map<String, List<Entry<? extends PackageRef, ? extends Attrs>>> requiresMap = externallyReferred.stream()
			.filterKey(packageRef -> {
				Attrs attrs = index.get(packageRef);
				Attrs importAttrs = imports.get(packageRef);

				if (attrs == null || !attrs.containsKey(INTERNAL_MODULE_DIRECTIVE)) {
					String eeModuleName = moduleEE.getModules()
						.stream()
						.filterValue(a -> a.getTyped(Attrs.LIST_STRING, EXPORTS_ATTRIBUTE)
							.contains(packageRef.getFQN()))
						.keys()
						.findAny()
						.orElse(null);
					if (eeModuleName == null) {
						if (logger.isWarnEnabled())
							logger.warn("Can't find a module name for imported package: {}", packageRef.getFQN());

						return false;
					}

					attrs = Attrs.create(INTERNAL_MODULE_DIRECTIVE, eeModuleName);
					index.put(packageRef, attrs);
				}
				if (importAttrs != null) {
					attrs.mergeWith(importAttrs, false);
				}
				return true;
			})
			// group packages by module/contract
			.collect(groupingBy(entry -> index.get(entry.getKey())
				.get(INTERNAL_MODULE_DIRECTIVE)));

		String manuallyRequiredModules = instruction.getValue()
			.get(MODULES_ATTRIBUTE);

		if (manuallyRequiredModules != null) {
			splitAsStream(manuallyRequiredModules)
				.forEach(moduleToAdd -> {
					requiresMap.computeIfAbsent(moduleToAdd, key -> emptyList());
				});
		}

		MapStream.of(requiresMap)
			.sortedByKey()
			.mapValue(referencedPackages -> MapStream.of(referencedPackages)
				.keys()
				.collect(toList()))
			.forEach((moduleName, referencedModulePackages) -> {
				Attrs moduleMappingAttrs = Optional.ofNullable(moduleInfoOptions.get(moduleName))
					.orElseGet(Attrs::new);

				if (isTrue(moduleMappingAttrs.get(IGNORE_ATTRIBUTE))) {
					return;
				}

				// An import results in `transitive` requires where there is an
				// `Export-Package` that has a `uses` constraint on the imported
				// package.
				boolean isTransitive = Optional.ofNullable(moduleMappingAttrs.get(TRANSITIVE_ATTRIBUTE))
					.map(Processor::isTrue)
					.orElseGet(() -> exports.values()
						.stream()
						.map(a -> a.get(USES_DIRECTIVE))
						.flatMap(Strings::splitAsStream)
						.map(analyzer::getPackageRef)
						.anyMatch(referencedModulePackages::contains));

				// TODO modules can fall under the follow categories:
				// a) JDK modules (whose packages are not _yet_ imported)
				// b) all packages referenced are `resolution:=optional` or
				// `Dynamic-ImportPackage`
				// c) statically referenced classes (which do not incur an
				// import like bundle annotations)
				// b) and c) result in static requires
				boolean isStatic = Optional.ofNullable(moduleMappingAttrs.get(STATIC_ATTRIBUTE))
					.map(Processor::isTrue)
					.orElseGet(() -> referencedModulePackages.isEmpty() || referencedModulePackages.stream()
						.allMatch(p -> {
							Attrs attrs = index.get(p);

							if (OPTIONAL.equals(attrs.get(RESOLUTION_DIRECTIVE))) {
								return true;
							} else if (!dynamicImportPackages.isEmpty() && dynamicImportPackages.matches(p.getFQN())) {
								return true;
							}

							return false;
						}));

				// Allowed: 0 | ACC_TRANSITIVE | ACC_STATIC_PHASE |
				// ACC_SYNTHETIC | ACC_MANDATED
				int access = (isTransitive ? ModuleAttribute.Require.ACC_TRANSITIVE : 0)
					| (isStatic ? ModuleAttribute.Require.ACC_STATIC_PHASE : 0);

				// TODO collect module version. Do we want module version? It is
				// not checked at runtime.

				builder.requires(moduleName, access, null);
			});
	}

	private void serviceLoaderProviders(Parameters provideCapabilities, Analyzer analyzer, ModuleInfoBuilder builder) {
		provideCapabilities.stream()
			.filterKey(namespace -> removeDuplicateMarker(namespace).equals(SERVICELOADER_NAMESPACE))
			// We need the `register:` directive to be present for this to work.
			.filterValue(attrs -> attrs.containsKey(SERVICELOADER_REGISTER_DIRECTIVE))
			.values()
			.collect(groupingBy(attrs -> analyzer.getTypeRefFromFQN(attrs.get(SERVICELOADER_NAMESPACE))))
			.entrySet()
			.forEach(entry -> {
				TypeRef typeRef = entry.getKey();
				Set<String> impls = entry.getValue()
					.stream()
					.map(attrs -> attrs.get(SERVICELOADER_REGISTER_DIRECTIVE))
					.map(impl -> analyzer.getTypeRefFromFQN(impl)
						.getBinary())
					.collect(toCollection(LinkedHashSet::new));
				builder.provides(typeRef.getBinary(), impls);
			});
	}

	private void serviceLoaderUses(Parameters requireCapabilities, Analyzer analyzer, ModuleInfoBuilder builder) {
		requireCapabilities.stream()
			.filterKey(key -> removeDuplicateMarker(key).equals(SERVICELOADER_NAMESPACE))
			.values()
			.forEach(attrs -> {
				TypeRef typeRef = analyzer.getTypeRefFromFQN(attrs.get(SERVICELOADER_NAMESPACE));
				builder.uses(typeRef.getBinary());
			});
	}

	static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Set<Object> seen = ConcurrentHashMap.newKeySet();

		return t -> seen.add(keyExtractor.apply(t));
	}
}
