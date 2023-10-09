package aQute.bnd.plugin.jpms;

import static aQute.bnd.classfile.ModuleAttribute.ACC_MANDATED;
import static aQute.bnd.classfile.ModuleAttribute.ACC_OPEN;
import static aQute.bnd.classfile.ModuleAttribute.ACC_SYNTHETIC;
import static aQute.bnd.osgi.Constants.ACCESS_ATTRIBUTE;
import static aQute.bnd.osgi.Constants.AUTOMATIC_MODULE_NAME;
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
import static java.util.stream.Collectors.toSet;
import static org.osgi.framework.Constants.RESOLUTION_MANDATORY;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.Manifest;
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
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.JPMSModule;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.ManifestPlugin;
import aQute.bnd.stream.MapStream;
import aQute.lib.io.ByteBufferDataOutput;
import aQute.lib.strings.Strings;

/**
 * A plugin to generate a module-info class from analyzer metadata and bundle
 * annotations.
 */
public class JPMSModuleInfoPlugin implements ManifestPlugin {

	enum Access {
		CLOSED(0),
		OPEN(ACC_OPEN),
		SYNTHETIC(ACC_SYNTHETIC),
		MANDATED(ACC_MANDATED);

		public static Set<Access> parse(String input) {
			return switch (input.toLowerCase(Locale.ROOT)) {
				case "open" ,"0x0020" , "32" -> EnumSet.of(OPEN);
				case "synthetic" , "0x1000" , "4096" -> EnumSet.of(SYNTHETIC);
				case "mandated" , "0x8000"  ,"32768" -> EnumSet.of(MANDATED);
				default -> {
					if (input.indexOf(',') > -1) {
						yield Strings.splitAsStream(input)
							.map(Access::parse)
							.flatMap(Set::stream)
							.collect(toCollection(() -> EnumSet.noneOf(Access.class)));
					}
					int parsedValue = Integer.decode(input);
					yield Arrays.stream(values())
						.filter(a -> a.getValue() == parsedValue)
						.findFirst()
						.map(EnumSet::of)
						.orElseThrow(() -> new IllegalArgumentException(input));
				}
			};
		}

		Access(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		private final int value;
	}

	private static final Logger		logger						= LoggerFactory.getLogger(JPMSModuleInfoPlugin.class);

	private static final Pattern	mangledModuleName			= Pattern.compile("(.*)-\\d.*");

	private static final EE			DEFAULT_MODULE_EE			= EE.JavaSE_11;
	private static final String		INTERNAL_MODULE_DIRECTIVE	= "-internal-module:";

	@Override
	public void mainSet(Analyzer analyzer, Manifest manifest) throws Exception {
		String moduleProperty = analyzer.getProperty(JPMS_MODULE_INFO);
		if (moduleProperty == null)
			return;

		Domain domain = Domain.domain(manifest);

		Parameters provideCapabilities = new Parameters(domain.get(PROVIDE_CAPABILITY));
		Parameters requireCapabilities = new Parameters(domain.get(REQUIRE_CAPABILITY));

		if (moduleProperty.isEmpty()) {
			String name = name(analyzer);
			int access = Access.OPEN.getValue();
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

		Packages contained = analyzer.getContained();
		Packages referred = analyzer.getReferred();
		Packages imports = analyzer.getImports();
		Packages exports = analyzer.getExports();
		Packages index = new Packages();


		// Index the whole class path
		for (Jar jar : analyzer.getClasspath()) {
			JPMSModule m = new JPMSModule(jar);

			String moduleName = getModuleName(analyzer, m, moduleInfoOptions);
			String moduleVersion = m.getModuleVersion()
				.orElse(null);
			Attrs attrs = new Attrs();
			if (moduleName != null) {
				attrs.put(INTERNAL_MODULE_DIRECTIVE, moduleName);
			}
			if (moduleVersion != null) {
				attrs.put(INTERNAL_MODULE_VERSION_DIRECTIVE, moduleVersion);
			}
			MapStream.of(jar.getDirectories())
				.filter((path, resources) -> (resources != null) && !resources.isEmpty() && !path.isEmpty())
				.map((k, v) -> MapStream.entry(analyzer.getPackageRef(k), imports.get(analyzer.getPackageRef(k))))
				.filterKey(PackageRef::isValidPackageName)
				.forEach((packageRef, packageAttrs) -> index.put(packageRef, new Attrs(packageAttrs, attrs)));
		}

		Set<PackageRef> bcpEntries = analyzer.getBundleClassPathTypes()
			.keySet()
			.stream()
			.map(TypeRef::getPackageRef)
			.collect(toSet());

		ModuleInfoBuilder builder = nameAccessAndVersion(moduleInstructions, requireCapabilities, analyzer);

		packages(builder, analyzer);
		requires(builder, analyzer, moduleInstructions, index, moduleInfoOptions, bcpEntries,
			domain.get(Constants.DYNAMICIMPORT_PACKAGE));
		exportPackages(builder, analyzer, exports, bcpEntries);
		openPackages(builder, contained);
		serviceLoaderProviders(builder, analyzer, provideCapabilities, bcpEntries);
		serviceLoaderUses(builder, analyzer, requireCapabilities, bcpEntries);
		mainClass(builder, analyzer);

		ByteBufferDataOutput bbout = new ByteBufferDataOutput();
		builder.build()
			.write(bbout);

		analyzer.getJar()
			.putResource(MODULE_INFO_CLASS, new EmbeddedResource(bbout.toByteBuffer(), analyzer.lastModified()));
	}

	private String getModuleName(Analyzer analyzer, JPMSModule m, Parameters moduleInfoOptions) throws Exception {
		String moduleName = m.getModuleName()
			.orElse(null);
		if (moduleName == null) {
			Jar jar = m.getJar();

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

			if (logger.isDebugEnabled())
				logger.debug("Using module name '{}' for: {}", moduleName, jar);
		}
		return JPMSModule.cleanupName(moduleName);
	}

	private String name(Analyzer analyzer) {
		return analyzer.getProperty(AUTOMATIC_MODULE_NAME, JPMSModule.cleanupName(analyzer.getBsn()));
	}


	private void packages(ModuleInfoBuilder builder, Analyzer analyzer) {
		MapStream.ofNullable(analyzer.getJar()
			.getDirectories())
			.filterKey(
				k -> !k.startsWith("META-INF") && !k.startsWith("OSGI-INF") && !k.startsWith("WEB-INF") && !k.isEmpty())
			.filterValue(Objects::nonNull)
			.filterValue(m -> //
			!m.values()
				.isEmpty())
			.keys()
			.forEach(builder::packages);
	}

	private void exportPackages(ModuleInfoBuilder builder, Analyzer analyzer, Packages contained,
		Set<PackageRef> bcpEntries) {
		MapStream.of(analyzer.getExports())
			// We can't export JPMS packages from the Bundle-ClassPath because
			// jlink will fail
			.filterKey(k -> !bcpEntries.contains(k))
			.forEach((packageRef, attrs) -> {
				Set<String> targets = Collections.emptySet();

				Attrs containedAttrs = contained.get(packageRef);
				if (containedAttrs != null && containedAttrs.containsKey(INTERNAL_EXPORT_TO_MODULES_DIRECTIVE)) {
					targets = splitAsStream(containedAttrs.get(INTERNAL_EXPORT_TO_MODULES_DIRECTIVE))
						.collect(toCollection(setFactory()));
				}
				// Allowed: 0 | ACC_SYNTHETIC | ACC_MANDATED
				builder.exports(packageRef.getBinary(), 0, targets);
			});
	}

	private void mainClass(ModuleInfoBuilder builder, Analyzer analyzer) {
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
		String access = attrs.computeIfAbsent(ACCESS_ATTRIBUTE, k -> Access.OPEN.name());
		String version = attrs.computeIfAbsent(VERSION_ATTRIBUTE, k -> analyzer.getVersion());

		ModuleInfoBuilder builder = new ModuleInfoBuilder().module_name(name)
			.module_version(version)
			.module_flags(Access.parse(access)
				.stream()
				.mapToInt(Access::getValue)
				.reduce(0, (l, r) -> l | r));
		return builder;
	}

	private void openPackages(ModuleInfoBuilder builder, Packages contained) {
		contained.stream()
			.filterValue(attrs -> attrs.containsKey(INTERNAL_OPEN_TO_MODULES_DIRECTIVE))
			.forEachOrdered((packageRef, attrs) -> {
				Set<String> targets = splitAsStream(attrs.get(INTERNAL_OPEN_TO_MODULES_DIRECTIVE))
					.collect(toCollection(setFactory()));

				// TODO Do we want to handle access? I can't think of a reason.
				// Allowed: 0 | ACC_SYNTHETIC | ACC_MANDATED
				builder.opens(packageRef.getBinary(), 0, targets);
			});
	}

	private void requires(ModuleInfoBuilder builder, Analyzer analyzer, Entry<String, Attrs> instruction,
		Packages index, Parameters moduleInfoOptions, Set<PackageRef> bcpEntries, String dynamicImports)
		throws Exception {

		String eeAttribute = instruction.getValue()
			.get(Constants.EE_ATTRIBUTE);

		EE moduleEE = (eeAttribute != null) ? Optional.of(eeAttribute)
			.map(EE::parse)
			.map(ee -> {
				if (ee.compareTo(EE.JavaSE_9) < 0) {
					if (logger.isWarnEnabled()) {
						logger.warn("The specified EE " + ee
							+ " is less than the minimum for JPMS. Reseting to a reasonable default: "
							+ DEFAULT_MODULE_EE);
					}

					return DEFAULT_MODULE_EE;
				}
				return ee;
			})
			.orElseThrow(() -> new IllegalArgumentException("unrecognize ee name: " + eeAttribute)) : DEFAULT_MODULE_EE;

		Packages exports = analyzer.getExports();
		Packages imports = analyzer.getImports();
		Packages referred = analyzer.getReferred();

		Instructions dynamicImportPackages = new Instructions(dynamicImports);

		Packages externallyReferred = new Packages(referred);
		exports.keySet()
			.forEach(externallyReferred::remove);

		Map<String, List<Entry<? extends PackageRef, ? extends Attrs>>> requiresMap = externallyReferred.stream()
			.filterKey(packageRef -> {
				if (bcpEntries.contains(packageRef)) {
					return false;
				}

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
						if (logger.isDebugEnabled() &&
						// Ignore optional imports at this stage
						MapStream.ofNullable(importAttrs)
							.noneMatch((k, v) -> k.equals(RESOLUTION_DIRECTIVE) && v.equals(RESOLUTION_OPTIONAL))) {

							logger.debug("Can't find a module name for imported package: {}", packageRef.getFQN());
						}

						return false;
					}

					attrs = (attrs != null) ? attrs : ((importAttrs != null) ? new Attrs(importAttrs) : new Attrs());
					attrs.put(INTERNAL_MODULE_DIRECTIVE, eeModuleName);
					index.put(packageRef, attrs);
				}
				if (importAttrs != null) {
					String existingResolution = attrs.get(RESOLUTION_DIRECTIVE);
					String importResolution = importAttrs.get(RESOLUTION_DIRECTIVE);

					if (RESOLUTION_OPTIONAL.equals(existingResolution)) {
						if (null == importResolution) {
							attrs.remove(RESOLUTION_DIRECTIVE);
						} else if (RESOLUTION_MANDATORY.equals(importResolution)) {
							attrs.put(RESOLUTION_DIRECTIVE, RESOLUTION_MANDATORY);
						}
					}
				}
				return true;
			})
			// group packages by module/contract
			.collect(groupingBy(entry -> index.get(entry.getKey())
				.get(INTERNAL_MODULE_DIRECTIVE), mapFactory(), toList()));

		String manuallyRequiredModules = instruction.getValue()
			.get(MODULES_ATTRIBUTE);

		if (manuallyRequiredModules != null) {
			splitAsStream(manuallyRequiredModules)
				.forEachOrdered(moduleToAdd -> requiresMap.computeIfAbsent(moduleToAdd, key -> emptyList()));
		}

		MapStream.of(requiresMap)
			.sortedByKey()
			.mapValue(referencedPackages -> MapStream.of(referencedPackages)
				.keys()
				.collect(toList()))
			.forEachOrdered((moduleName, referencedModulePackages) -> {
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

	private void serviceLoaderProviders(ModuleInfoBuilder builder, Analyzer analyzer, Parameters provideCapabilities,
		Set<PackageRef> bcpEntries) {
		provideCapabilities.stream()
			.filterKey(namespace -> removeDuplicateMarker(namespace).equals(SERVICELOADER_NAMESPACE))
			// We need the `register:` directive to be present for this to work.
			.filterValue(attrs -> attrs.containsKey(SERVICELOADER_REGISTER_DIRECTIVE))
			.values()
			.collect(groupingBy(attrs -> analyzer.getTypeRefFromFQN(attrs.get(SERVICELOADER_NAMESPACE)),
				mapFactory(), toList()))
			.forEach((typeRef, attrsList) -> {

				// We can't provide JPMS services from packages on the
				// Bundle-ClassPath because jlink will fail
				if (bcpEntries.contains(typeRef.getPackageRef())) {
					return;
				}

				Set<String> impls = attrsList.stream()
					.map(attrs -> attrs.get(SERVICELOADER_REGISTER_DIRECTIVE))
					.map(impl -> analyzer.getTypeRefFromFQN(impl))
					.filter(implTypeRef -> !bcpEntries.contains(implTypeRef.getPackageRef()))
					.map(TypeRef::getBinary)
					.collect(toCollection(setFactory()));

				if (!impls.isEmpty()) {
					builder.provides(typeRef.getBinary(), impls);
				}
			});
	}

	private void serviceLoaderUses(ModuleInfoBuilder builder, Analyzer analyzer, Parameters requireCapabilities,
		Set<PackageRef> bcpEntries) {
		requireCapabilities.stream()
			.filterKey(key -> removeDuplicateMarker(key).equals(SERVICELOADER_NAMESPACE))
			.values()
			.forEachOrdered(attrs -> {
				TypeRef typeRef = analyzer.getTypeRefFromFQN(attrs.get(SERVICELOADER_NAMESPACE));

				if (!bcpEntries.contains(typeRef.getPackageRef())) {
					builder.uses(typeRef.getBinary());
				}
			});
	}

	private static <T> Supplier<Set<T>> setFactory() {
		return LinkedHashSet::new;
	}

	private static <K, V> Supplier<Map<K, V>> mapFactory() {
		return LinkedHashMap::new;
	}
}
