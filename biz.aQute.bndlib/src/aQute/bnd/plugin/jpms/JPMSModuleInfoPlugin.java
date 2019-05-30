package aQute.bnd.plugin.jpms;

import static aQute.bnd.osgi.Constants.AUTOMATIC_MODULE_NAME;
import static aQute.bnd.osgi.Constants.INTERNAL_EXPORT_TO_MODULES_DIRECTIVE;
import static aQute.bnd.osgi.Constants.INTERNAL_MODULE_VERSION_DIRECTIVE;
import static aQute.bnd.osgi.Constants.INTERNAL_OPEN_TO_MODULES_DIRECTIVE;
import static aQute.bnd.osgi.Constants.JPMS_MODULE_INFO;
import static aQute.bnd.osgi.Constants.MAIN_CLASS;
import static aQute.bnd.osgi.Constants.MODULE_INFO_CLASS;
import static aQute.bnd.osgi.Constants.SERVICELOADER_NAMESPACE;
import static aQute.bnd.osgi.Constants.SERVICELOADER_REGISTER_DIRECTIVE;
import static aQute.bnd.osgi.Constants.USES_DIRECTIVE;
import static java.util.stream.Collectors.groupingBy;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
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
import aQute.bnd.classfile.writer.ModuleInfoWriter;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.service.verifier.VerifierPlugin;
import aQute.lib.env.Header;
import aQute.lib.io.ByteBufferDataOutput;
import aQute.lib.strings.Strings;

/**
 * A plugin to generate a module-info class from analyzer metadata and bundle
 * annotations.
 */
public class JPMSModuleInfoPlugin implements VerifierPlugin {

	private static final Logger		logger				= LoggerFactory.getLogger(JPMSModuleInfoPlugin.class);

	private static final Pattern	mangledModuleName			= Pattern.compile("(.*)-\\d.*");

	private static final String[]	EMPTY						= new String[0];
	private static final EE			DEFAULT_MODULE_EE			= EE.JavaSE_11_0;
	private static final String		INTERNAL_MODULE_DIRECTIVE	= "-internal-module:";

	@Override
	public void verify(final Analyzer analyzer) throws Exception {
		String moduleProperty = analyzer.getProperty(JPMS_MODULE_INFO);
		if (moduleProperty == null)
			return;

		Parameters provideCapabilities = new Parameters(analyzer.getJar()
			.getManifest()
			.getMainAttributes()
			.getValue(Constants.PROVIDE_CAPABILITY));
		Parameters requireCapabilities = new Parameters(analyzer.getJar()
			.getManifest()
			.getMainAttributes()
			.getValue(Constants.REQUIRE_CAPABILITY));

		if ("".equals(moduleProperty)) {
			String name = name(analyzer);
			int access = access(requireCapabilities);
			String version = analyzer.getVersion();

			moduleProperty = String.format("%s;access=%s;version=%s", name, access, version);
		}

		Parameters moduleParameters = OSGiHeader.parseHeader(moduleProperty);
		if (moduleParameters.size() == 0)
			return;

		if (moduleParameters.size() > 1)
			throw new IllegalArgumentException("Only one -module instruction is allowed:" + moduleParameters);

		Entry<String, Attrs> moduleInstructions = moduleParameters.entrySet()
			.iterator()
			.next();

		Packages index = new Packages();

		// Index the whole class path
		for (Jar jar : analyzer.getClasspath()) {
			String moduleName = getModuleName(analyzer, jar);
			String moduleVersion = jar.getModuleVersion();
			jar.getDirectories()
				.entrySet()
				.stream()
				.filter(entry -> (entry.getValue() != null) && !entry.getValue()
					.isEmpty()
					&& entry.getKey()
						.length() > 0
					&& !entry.getKey()
						.startsWith("META-INF")
					&& !entry.getKey()
						.startsWith("OSGI-INF")
					&& !entry.getKey()
						.startsWith("OSGI-OPT")
					&& !entry.getKey()
						.startsWith("WEB-INF"))
				.forEach(entry -> {
					PackageRef ref = analyzer.getPackageRef(entry.getKey());
					Attrs attrs = new Attrs();
					if (moduleName != null) {
						attrs.put(INTERNAL_MODULE_DIRECTIVE, moduleName);
					}
					if (moduleVersion != null) {
						attrs.put(INTERNAL_MODULE_VERSION_DIRECTIVE, moduleVersion);
					}
					index.put(ref, attrs);
				});
		}

		ModuleInfoWriter writer = nameAccessAndVersion(moduleInstructions, requireCapabilities, analyzer);

		requires(moduleInstructions, analyzer, index, writer);
		exportPackages(analyzer, writer);
		openPackages(analyzer, writer);
		serviceLoaderProviders(provideCapabilities, analyzer, writer);
		serviceLoaderUses(requireCapabilities, analyzer, writer);
		mainClass(analyzer, writer);

		// TODO use annotations to store other header info???
		// AnnotationVisitor visitAnnotation = classWriter.visitAnnotation(...,
		// false);

		ByteBufferDataOutput bbout = new ByteBufferDataOutput();
		writer.write(bbout);

		analyzer.getJar()
			.putResource(MODULE_INFO_CLASS,
				new EmbeddedResource(bbout.toByteBuffer(), analyzer.lastModified()));
	}

	private String getModuleName(Analyzer analyzer, Jar jar) throws Exception {
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
			if (logger.isWarnEnabled())
				logger.warn("Using generated module name '{}' for: {}", moduleName, jar);
		}
		return moduleName;
	}

	private int access(Parameters requireCapabilities) {
		return requireCapabilities.entrySet()
			.stream()
			.filter(entry -> Header.removeDuplicateMarker(entry.getKey())
				.equals(EXTENDER_NAMESPACE))
			.mapToInt(entry -> ModuleAttribute.ACC_OPEN)
			.findAny()
			.orElse(0);
	}

	private String name(Analyzer analyzer) {
		return analyzer.getProperty(AUTOMATIC_MODULE_NAME, analyzer.getBsn());
	}

	private void exportPackages(Analyzer analyzer, ModuleInfoWriter writer) {
		Packages contained = analyzer.getContained();

		analyzer.getExports()
			.forEach((packageRef, attrs) -> {
				String[] targets = EMPTY;

				Attrs containedAttrs = contained.get(packageRef);
				if (containedAttrs != null && containedAttrs.containsKey(INTERNAL_EXPORT_TO_MODULES_DIRECTIVE)) {
					targets = Strings.splitAsStream(containedAttrs.get(INTERNAL_EXPORT_TO_MODULES_DIRECTIVE))
						.toArray(String[]::new);
				}

				// TODO Do we want to handle access? I can't think of a reason.
				// Allowed: 0 | ACC_SYNTHETIC | ACC_MANDATED

				writer.exports(packageRef.getBinary(), 0, targets);
			});
	}

	private void mainClass(Analyzer analyzer, ModuleInfoWriter writer) {
		String mainClass = analyzer.getProperty(MAIN_CLASS);

		if (mainClass != null) {
			TypeRef typeRef = analyzer.getTypeRefFromFQN(mainClass);

			writer.mainClass(typeRef.getBinary());
		}
	}

	private ModuleInfoWriter nameAccessAndVersion(Entry<String, Attrs> instruction, Parameters requireCapability,
		Analyzer analyzer) {
		Attrs attrs = instruction.getValue();

		String name = instruction.getKey();
		// Allowed: 0 | ACC_OPEN | ACC_SYNTHETIC | ACC_MANDATED
		String access = attrs.computeIfAbsent("access", k -> String.valueOf(access(requireCapability)));
		String version = attrs.computeIfAbsent("version", k -> analyzer.getVersion());

		return new ModuleInfoWriter(Clazz.JAVA.OpenJDK9.getMajor(), name, version,
			Integer.parseInt(access) == ModuleAttribute.ACC_OPEN);
	}

	private void openPackages(Analyzer analyzer, ModuleInfoWriter writer) {
		analyzer.getContained()
			.entrySet()
			.stream()
			.filter(entry -> entry.getValue()
				.containsKey(INTERNAL_OPEN_TO_MODULES_DIRECTIVE))
			.forEach(entry -> {
				PackageRef packageRef = entry.getKey();

				String[] targets = Strings.splitAsStream(entry.getValue()
					.get(INTERNAL_OPEN_TO_MODULES_DIRECTIVE))
					.toArray(String[]::new);

				// TODO Do we want to handle access? I can't think of a reason.
				// Allowed: 0 | ACC_SYNTHETIC | ACC_MANDATED

				writer.opens(packageRef.getBinary(), 0, targets);
			});
	}

	private void requires(Entry<String, Attrs> instruction, Analyzer analyzer, Packages index,
		ModuleInfoWriter writer) {

		String eeAttribute = instruction.getValue()
			.get("ee");

		EE moduleEE = (eeAttribute != null) ? EE.valueOf(eeAttribute) : DEFAULT_MODULE_EE;

		Packages exports = analyzer.getExports();
		Packages imports = analyzer.getImports();
		Packages referred = analyzer.getReferred();

		Packages externallyReferred = new Packages(referred);
		exports.forEach((pack, a) -> externallyReferred.remove(pack));

		Set<String> requiredModules = new HashSet<>();

		String manuallyRequiredModules = instruction.getValue()
			.get("modules");

		if (manuallyRequiredModules != null) {
			Strings.splitAsStream(manuallyRequiredModules)
				.forEach(moduleToAdd -> {
					writer.requires(moduleToAdd, 0, null);
					requiredModules.add(moduleToAdd);
				});
		}

		externallyReferred.entrySet()
			.stream()
			.filter(entry -> {
				PackageRef packageRef = entry.getKey();
				Attrs attrs = index.get(packageRef);

				if (attrs == null || !attrs.containsKey(INTERNAL_MODULE_DIRECTIVE)) {
					String eeModuleName = moduleEE.getModules()
						.entrySet()
						.stream()
						.filter(it -> it.getValue()
							.getTyped(Attrs.LIST_STRING, "exports")
							.contains(packageRef.getFQN()))
						.map(Entry::getKey)
						.findAny()
						.orElse(null);

					if (eeModuleName != null) {
						index.put(packageRef, Attrs.create(INTERNAL_MODULE_DIRECTIVE, eeModuleName));
						return true;
					}

					if (logger.isWarnEnabled())
						logger.warn("Can't find a module name for imported package: {}", packageRef.getFQN());

					return false;
				}
				return true;
			})
			// group packages by module/contract
			.collect(groupingBy(entry -> index.get(entry.getKey())
				.get(INTERNAL_MODULE_DIRECTIVE)))
			.entrySet()
			.forEach(entry -> {
				String moduleName = entry.getKey();

				if (requiredModules.contains(moduleName)) {
					return;
				}

				List<Entry<PackageRef, Attrs>> referencedPackages = entry.getValue();

				// An import results in `transitive` requires where there is an
				// `Export-Package` that has a `uses` constraint on the imported
				// package.
				boolean isTransitive = exports.entrySet()
					.stream()
					.map(export -> export.getValue()
						.get(USES_DIRECTIVE))
					.flatMap(Strings::splitAsStream)
					.map(use -> analyzer.getPackageRef(Descriptors.fqnToBinary(use)))
					.anyMatch(packageRef -> referencedPackages.stream()
						.anyMatch(e -> e.getKey()
							.equals(packageRef)));

				// TODO modules can fall under the follow categories:
				// a) JDK modules (whose packages are not _yet_ imported)
				// b) all packages referenced are `resolution:=optional` or
				// `Dynamic-ImportPackage`
				// c) statically referenced classes (which do not incur an
				// import like bundle annotations)
				// b) and c) result in static requires
				boolean isStatic = false;

				// Allowed: 0 | ACC_TRANSITIVE | ACC_STATIC_PHASE |
				// ACC_SYNTHETIC | ACC_MANDATED
				int access = (isTransitive ? ModuleAttribute.Require.ACC_TRANSITIVE : 0)
					| (isStatic ? ModuleAttribute.Require.ACC_STATIC_PHASE : 0);

				// TODO collect module version. Do we want module version? It is
				// not checked at runtime.

				writer.requires(moduleName, access, null);

				requiredModules.add(moduleName);
			});
	}

	private void serviceLoaderProviders(Parameters provideCapabilities, Analyzer analyzer,
		ModuleInfoWriter writer) {
		provideCapabilities.entrySet()
			.stream()
			.filter(entry -> Header.removeDuplicateMarker(entry.getKey())
				.equals(SERVICELOADER_NAMESPACE))
			// We need the `register:` directive to be present for this to work.
			.filter(entry -> entry.getValue()
				.containsKey(SERVICELOADER_REGISTER_DIRECTIVE))
			.map(Entry::getValue)
			.collect(groupingBy(attrs -> analyzer.getTypeRefFromFQN(attrs.get(SERVICELOADER_NAMESPACE))))
			.entrySet()
			.forEach(entry -> {
				TypeRef typeRef = entry.getKey();
				String[] impls = entry.getValue()
					.stream()
					.map(attrs -> attrs.get(SERVICELOADER_REGISTER_DIRECTIVE))
					.map(impl -> analyzer.getTypeRefFromFQN(impl)
						.getBinary())
					.toArray(String[]::new);

				writer.provides(typeRef.getBinary(), impls);
			});
	}

	private void serviceLoaderUses(Parameters requireCapabilities, Analyzer analyzer, ModuleInfoWriter writer) {
		requireCapabilities.entrySet()
			.stream()
			.filter(entry -> Header.removeDuplicateMarker(entry.getKey())
				.equals(SERVICELOADER_NAMESPACE))
			.map(Entry::getValue)
			.forEach(attrs -> {
				TypeRef typeRef = analyzer.getTypeRefFromFQN(attrs.get(SERVICELOADER_NAMESPACE));

				writer.uses(typeRef.getBinary());
			});
	}

	static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Set<Object> seen = ConcurrentHashMap.newKeySet();

		return t -> seen.add(keyExtractor.apply(t));
	}
}
