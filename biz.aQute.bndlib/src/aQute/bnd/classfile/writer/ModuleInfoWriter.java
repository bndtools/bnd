package aQute.bnd.classfile.writer;

import static java.util.Objects.requireNonNull;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ModuleAttribute;

public class ModuleInfoWriter implements DataOutputWriter {
	private final int									major_version;
	private final String								module_name;
	private final int									module_flags;
	private final String								module_version;
	private final Map<String, ModuleAttribute.Require>	requires	= new LinkedHashMap<>();
	private final Map<String, ModuleAttribute.Export>	exports		= new LinkedHashMap<>();
	private final Map<String, ModuleAttribute.Open>		opens		= new LinkedHashMap<>();
	private final Set<String>							uses		= new LinkedHashSet<>();
	private final Map<String, ModuleAttribute.Provide>	provides	= new LinkedHashMap<>();
	private String										mainClass;
	private final Set<String>							packages	= new LinkedHashSet<>();

	public ModuleInfoWriter(int major_version, String module_name, String module_version, boolean open) {
		this.major_version = major_version;
		this.module_name = module_name;
		module_flags = open ? ModuleAttribute.ACC_OPEN : 0;
		this.module_version = module_version;
		requires("java.base", ModuleAttribute.Require.ACC_MANDATED, null);
	}

	public ModuleInfoWriter requires(String moduleName, int flags, String moduleVersion) {
		requireNonNull(moduleName);
		ModuleAttribute.Require require = new ModuleAttribute.Require(moduleName, flags, moduleVersion);
		requires.put(moduleName, require);
		return this;
	}

	public ModuleInfoWriter exports(String binaryPackageName, int flags, Set<String> toModules) {
		requireNonNull(binaryPackageName);
		toModules.forEach(Objects::requireNonNull);
		ModuleAttribute.Export export = new ModuleAttribute.Export(binaryPackageName, flags,
			toModules.toArray(new String[0]));
		exports.put(binaryPackageName, export);
		packages(binaryPackageName);
		return this;
	}

	public ModuleInfoWriter exports(String binaryPackageName, int flags, String toModule) {
		return exports(binaryPackageName, flags, Collections.singleton(toModule));
	}

	public ModuleInfoWriter exports(String binaryPackageName, int flags, String... toModules) {
		return exports(binaryPackageName, flags, new HashSet<>(Arrays.asList(toModules)));
	}

	public ModuleInfoWriter opens(String binaryPackageName, int flags, Set<String> toModules) {
		requireNonNull(binaryPackageName);
		toModules.forEach(Objects::requireNonNull);
		ModuleAttribute.Open open = new ModuleAttribute.Open(binaryPackageName, flags,
			toModules.toArray(new String[0]));
		opens.put(binaryPackageName, open);
		packages(binaryPackageName);
		return this;
	}

	public ModuleInfoWriter opens(String binaryPackageName, int flags, String toModule) {
		return opens(binaryPackageName, flags, Collections.singleton(toModule));
	}

	public ModuleInfoWriter opens(String binaryPackageName, int flags, String... toModules) {
		return opens(binaryPackageName, flags, new HashSet<>(Arrays.asList(toModules)));
	}

	public ModuleInfoWriter uses(Set<String> binaryClassNames) {
		binaryClassNames.forEach(Objects::requireNonNull);
		uses.addAll(binaryClassNames);
		return this;
	}

	public ModuleInfoWriter uses(String... binaryClassNames) {
		return uses(new HashSet<>(Arrays.asList(binaryClassNames)));
	}

	public ModuleInfoWriter uses(String binaryClassName) {
		return uses(Collections.singleton(binaryClassName));
	}

	public ModuleInfoWriter provides(String binaryClassName, Set<String> binaryWithClassNames) {
		requireNonNull(binaryClassName);
		if (binaryWithClassNames.isEmpty()) {
			throw new IllegalArgumentException("No module names specified");
		}
		binaryWithClassNames.forEach(Objects::requireNonNull);
		ModuleAttribute.Provide provide = new ModuleAttribute.Provide(binaryClassName,
			binaryWithClassNames.toArray(new String[0]));
		provides.put(binaryClassName, provide);
		binaryWithClassNames.forEach(c -> {
			int i = c.lastIndexOf('/');
			if (i > 0) {
				String binaryPackageName = c.substring(0, i);
				packages(binaryPackageName);
			}
		});
		return this;
	}

	public ModuleInfoWriter provides(String binaryClassName, String binaryWithClassName) {
		return provides(binaryClassName, Collections.singleton(binaryWithClassName));
	}

	public ModuleInfoWriter provides(String binaryClassName, String... binaryWithClassNames) {
		return provides(binaryClassName, new HashSet<>(Arrays.asList(binaryWithClassNames)));
	}

	public ModuleInfoWriter mainClass(String binaryClassName) {
		this.mainClass = requireNonNull(binaryClassName);
		return this;
	}

	public ModuleInfoWriter packages(Set<String> binaryPackageNames) {
		binaryPackageNames.forEach(Objects::requireNonNull);
		packages.addAll(binaryPackageNames);
		return this;
	}

	public ModuleInfoWriter packages(String... binaryPackageNames) {
		return packages(new HashSet<>(Arrays.asList(binaryPackageNames)));
	}

	public ModuleInfoWriter packages(String binaryPackageName) {
		return packages(Collections.singleton(binaryPackageName));
	}

	@Override
	public void write(DataOutput out) throws IOException {
		ConstantPoolWriter constantPool = new ConstantPoolWriter();
		ClassFileWriter classFile = new ClassFileWriter(constantPool, ClassFile.ACC_MODULE, major_version, 0,
			"module-info", null);
		classFile.attributes(new ModuleAttributeWriter(constantPool, module_name, module_flags, module_version,
			requires.values(), exports.values(), opens.values(), uses, provides.values()));
		if (!packages.isEmpty()) {
			classFile.attributes(new ModulePackagesAttributeWriter(constantPool, packages));
		}
		if (mainClass != null) {
			classFile.attributes(new ModuleMainClassAttributeWriter(constantPool, mainClass));
		}
		classFile.write(out);
	}
}
