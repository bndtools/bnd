package aQute.bnd.classfile.builder;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.classfile.ModuleMainClassAttribute;
import aQute.bnd.classfile.ModulePackagesAttribute;

public class ModuleInfoBuilder extends ClassFileBuilder {
	static final ModuleAttribute.Require[]		EMPTY_REQUIRE_ARRAY	= new ModuleAttribute.Require[0];
	static final ModuleAttribute.Export[]		EMPTY_EXPORT_ARRAY	= new ModuleAttribute.Export[0];
	static final ModuleAttribute.Open[]			EMPTY_OPEN_ARRAY	= new ModuleAttribute.Open[0];
	static final ModuleAttribute.Provide[]		EMPTY_PROVIDE_ARRAY	= new ModuleAttribute.Provide[0];
	private String								module_name;
	private int									module_flags;
	private String								module_version;
	private final List<ModuleAttribute.Require>	requires			= new ArrayList<>();
	private final List<ModuleAttribute.Export>	exports				= new ArrayList<>();
	private final List<ModuleAttribute.Open>	opens				= new ArrayList<>();
	private final List<String>					uses				= new ArrayList<>();
	private final List<ModuleAttribute.Provide>	provides			= new ArrayList<>();
	private String								mainClass;
	private final List<String>					packages			= new ArrayList<>();

	public ModuleInfoBuilder() {
		super(ClassFile.ACC_MODULE, 53, 0, "module-info", null);
		requires.add(new ModuleAttribute.Require("java.base", ModuleAttribute.Require.ACC_MANDATED, null));
	}

	public String module_name() {
		return module_name;
	}

	public ModuleInfoBuilder module_name(String module_name) {
		this.module_name = requireNonNull(module_name);
		return this;
	}

	public String module_version() {
		return module_version;
	}

	public ModuleInfoBuilder module_version(String module_version) {
		this.module_version = module_version;
		return this;
	}

	public int module_flags() {
		return module_flags;
	}

	public ModuleInfoBuilder module_flags(int module_flags) {
		this.module_flags = module_flags;
		return this;
	}

	public List<ModuleAttribute.Require> requires() {
		return requires;
	}

	public ModuleInfoBuilder requires(String moduleName, int flags) {
		return requires(moduleName, flags, null);
	}

	public ModuleInfoBuilder requires(String moduleName, int flags, String moduleVersion) {
		requireNonNull(moduleName);
		for (ListIterator<ModuleAttribute.Require> iter = requires.listIterator(); iter.hasNext();) {
			ModuleAttribute.Require entry = iter.next();
			if (entry.requires.equals(moduleName)) {
				iter.remove();
				break;
			}
		}
		ModuleAttribute.Require require = new ModuleAttribute.Require(moduleName, flags, moduleVersion);
		requires.add(require);
		return this;
	}

	public List<ModuleAttribute.Export> exports() {
		return exports;
	}

	public ModuleInfoBuilder exports(String binaryPackageName, int flags, Collection<String> toModules) {
		requireNonNull(binaryPackageName);
		toModules.forEach(Objects::requireNonNull);
		for (ListIterator<ModuleAttribute.Export> iter = exports.listIterator(); iter.hasNext();) {
			ModuleAttribute.Export entry = iter.next();
			if (entry.exports.equals(binaryPackageName)) {
				iter.remove();
				break;
			}
		}
		if (!(toModules instanceof Set)) {
			toModules = new LinkedHashSet<>(toModules);
		}
		ModuleAttribute.Export export = new ModuleAttribute.Export(binaryPackageName, flags,
			toModules.toArray(EMPTY_STRING_ARRAY));
		exports.add(export);
		packages(binaryPackageName);
		return this;
	}

	public ModuleInfoBuilder exports(String binaryPackageName, int flags) {
		return exports(binaryPackageName, flags, Collections.emptySet());
	}

	public ModuleInfoBuilder exports(String binaryPackageName, int flags, String toModule) {
		return exports(binaryPackageName, flags, Collections.singleton(toModule));
	}

	public ModuleInfoBuilder exports(String binaryPackageName, int flags, String... toModules) {
		return exports(binaryPackageName, flags, Arrays.asList(toModules));
	}

	public List<ModuleAttribute.Open> opens() {
		return opens;
	}

	public ModuleInfoBuilder opens(String binaryPackageName, int flags, Collection<String> toModules) {
		requireNonNull(binaryPackageName);
		toModules.forEach(Objects::requireNonNull);
		for (ListIterator<ModuleAttribute.Open> iter = opens.listIterator(); iter.hasNext();) {
			ModuleAttribute.Open entry = iter.next();
			if (entry.opens.equals(binaryPackageName)) {
				iter.remove();
				break;
			}
		}
		if (!(toModules instanceof Set)) {
			toModules = new LinkedHashSet<>(toModules);
		}
		ModuleAttribute.Open open = new ModuleAttribute.Open(binaryPackageName, flags,
			toModules.toArray(EMPTY_STRING_ARRAY));
		opens.add(open);
		packages(binaryPackageName);
		return this;
	}

	public ModuleInfoBuilder opens(String binaryPackageName, int flags) {
		return opens(binaryPackageName, flags, Collections.emptySet());
	}

	public ModuleInfoBuilder opens(String binaryPackageName, int flags, String toModule) {
		return opens(binaryPackageName, flags, Collections.singleton(toModule));
	}

	public ModuleInfoBuilder opens(String binaryPackageName, int flags, String... toModules) {
		return opens(binaryPackageName, flags, Arrays.asList(toModules));
	}

	public List<String> uses() {
		return uses;
	}

	public ModuleInfoBuilder uses(String binaryClassName) {
		requireNonNull(binaryClassName);
		if (!uses.contains(binaryClassName)) {
			uses.add(binaryClassName);
		}
		return this;
	}

	public ModuleInfoBuilder uses(Collection<String> binaryClassNames) {
		for (String u : binaryClassNames) {
			uses(u);
		}
		return this;
	}

	public ModuleInfoBuilder uses(String[] binaryClassNames) {
		for (String u : binaryClassNames) {
			uses(u);
		}
		return this;
	}

	public ModuleInfoBuilder uses(String binaryClassName, String... binaryClassNames) {
		uses(binaryClassName);
		uses(binaryClassNames);
		return this;
	}

	public List<ModuleAttribute.Provide> provides() {
		return provides;
	}

	public ModuleInfoBuilder provides(String binaryClassName, Collection<String> binaryWithClassNames) {
		requireNonNull(binaryClassName);
		if (binaryWithClassNames.isEmpty()) {
			throw new IllegalArgumentException("No module names specified");
		}
		binaryWithClassNames.forEach(Objects::requireNonNull);
		for (ListIterator<ModuleAttribute.Provide> iter = provides.listIterator(); iter.hasNext();) {
			ModuleAttribute.Provide entry = iter.next();
			if (entry.provides.equals(binaryClassName)) {
				iter.remove();
				break;
			}
		}
		if (!(binaryWithClassNames instanceof Set)) {
			binaryWithClassNames = new LinkedHashSet<>(binaryWithClassNames);
		}
		ModuleAttribute.Provide provide = new ModuleAttribute.Provide(binaryClassName,
			binaryWithClassNames.toArray(EMPTY_STRING_ARRAY));
		provides.add(provide);
		binaryWithClassNames.forEach(c -> {
			int i = c.lastIndexOf('/');
			if (i > 0) {
				String binaryPackageName = c.substring(0, i);
				packages(binaryPackageName);
			}
		});
		return this;
	}

	public ModuleInfoBuilder provides(String binaryClassName, String binaryWithClassName) {
		return provides(binaryClassName, Collections.singleton(binaryWithClassName));
	}

	public ModuleInfoBuilder provides(String binaryClassName, String... binaryWithClassNames) {
		return provides(binaryClassName, Arrays.asList(binaryWithClassNames));
	}

	public String mainClass() {
		return mainClass;
	}

	public ModuleInfoBuilder mainClass(String binaryClassName) {
		this.mainClass = requireNonNull(binaryClassName);
		return this;
	}

	public List<String> packages() {
		return packages;
	}

	public ModuleInfoBuilder packages(String binaryPackageName) {
		requireNonNull(binaryPackageName);
		if (!packages.contains(binaryPackageName)) {
			packages.add(binaryPackageName);
		}
		return this;
	}

	public ModuleInfoBuilder packages(Collection<String> binaryPackageNames) {
		for (String p : binaryPackageNames) {
			packages(p);
		}
		return this;
	}

	public ModuleInfoBuilder packages(String[] binaryPackageNames) {
		for (String p : binaryPackageNames) {
			packages(p);
		}
		return this;
	}

	public ModuleInfoBuilder packages(String binaryPackageName, String... binaryPackageNames) {
		packages(binaryPackageName);
		packages(binaryPackageNames);
		return this;
	}

	@Override
	public ClassFile build() {
		attributes(new ModuleAttribute(module_name(), module_flags(), module_version(), //
			requires().toArray(EMPTY_REQUIRE_ARRAY), //
			exports().toArray(EMPTY_EXPORT_ARRAY), //
			opens().toArray(EMPTY_OPEN_ARRAY), //
			uses().toArray(EMPTY_STRING_ARRAY), //
			provides().toArray(EMPTY_PROVIDE_ARRAY)));
		if (!packages().isEmpty()) {
			attributes(new ModulePackagesAttribute(packages().toArray(EMPTY_STRING_ARRAY)));
		}
		if (mainClass() != null) {
			attributes(new ModuleMainClassAttribute(mainClass()));
		}
		return super.build();
	}
}
