package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class ModuleAttribute implements Attribute {
	public static final String	NAME	= "Module";
	public final String			module_name;
	public final int			module_flags;
	public final String			module_version;
	public final Require[]		requires;
	public final Export[]		exports;
	public final Open[]			opens;
	public final String[]		uses;
	public final Provide[]		provides;

	ModuleAttribute(String module_name, int module_flags, String module_version, Require[] requires, Export[] exports,
		Open[] opens, String[] uses, Provide[] provides) {
		this.module_name = module_name;
		this.module_flags = module_flags;
		this.module_version = module_version;
		this.requires = requires;
		this.exports = exports;
		this.opens = opens;
		this.uses = uses;
		this.provides = provides;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + module_name + " " + module_version + " " + module_flags;
	}

	static ModuleAttribute parseModuleAttribute(DataInput in, ConstantPool constant_pool) throws IOException {
		int module_name_index = in.readUnsignedShort();
		int module_flags = in.readUnsignedShort();
		int module_version_index = in.readUnsignedShort();

		int requires_count = in.readUnsignedShort();
		Require[] requires = new Require[requires_count];
		for (int i = 0; i < requires_count; i++) {
			requires[i] = Require.parseRequire(in, constant_pool);
		}

		int exports_count = in.readUnsignedShort();
		Export[] exports = new Export[exports_count];
		for (int i = 0; i < exports_count; i++) {
			exports[i] = Export.parseExport(in, constant_pool);
		}

		int opens_count = in.readUnsignedShort();
		Open[] opens = new Open[opens_count];
		for (int i = 0; i < opens_count; i++) {
			opens[i] = Open.parseOpen(in, constant_pool);
		}

		int uses_count = in.readUnsignedShort();
		String[] uses = new String[uses_count];
		for (int i = 0; i < uses_count; i++) {
			int uses_index = in.readUnsignedShort();
			uses[i] = constant_pool.className(uses_index);
		}

		int provides_count = in.readUnsignedShort();
		Provide[] provides = new Provide[provides_count];
		for (int i = 0; i < provides_count; i++) {
			provides[i] = Provide.parseProvide(in, constant_pool);
		}

		return new ModuleAttribute(constant_pool.moduleName(module_name_index), module_flags,
			constant_pool.utf8(module_version_index), requires, exports, opens, uses, provides);
	}

	public static class Require {
		public final String	requires;
		public final int	requires_flags;
		public final String	requires_version;

		Require(String requires, int requires_flags, String requires_version) {
			this.requires = requires;
			this.requires_flags = requires_flags;
			this.requires_version = requires_version;
		}

		@Override
		public String toString() {
			return requires + " " + requires_version + " " + requires_flags;
		}

		static Require parseRequire(DataInput in, ConstantPool constant_pool) throws IOException {
			int requires_index = in.readUnsignedShort();
			int requires_flags = in.readUnsignedShort();
			int requires_version_index = in.readUnsignedShort();
			return new Require(constant_pool.moduleName(requires_index), requires_flags,
				constant_pool.utf8(requires_version_index));
		}
	}

	public static class Export {
		public final String		exports;
		public final int		exports_flags;
		public final String[]	exports_to;

		Export(String exports, int exports_flags, String[] exports_to) {
			this.exports = exports;
			this.exports_flags = exports_flags;
			this.exports_to = exports_to;
		}

		@Override
		public String toString() {
			return exports + " " + Arrays.toString(exports_to) + " " + exports_flags;
		}

		static Export parseExport(DataInput in, ConstantPool constant_pool) throws IOException {
			int exports_index = in.readUnsignedShort();
			int exports_flags = in.readUnsignedShort();
			int exports_to_count = in.readUnsignedShort();
			String[] exports_to = new String[exports_to_count];
			for (int i = 0; i < exports_to_count; i++) {
				int exports_to_index = in.readUnsignedShort();
				exports_to[i] = constant_pool.moduleName(exports_to_index);
			}
			return new Export(constant_pool.packageName(exports_index), exports_flags, exports_to);
		}
	}

	public static class Open {
		public final String		opens;
		public final int		opens_flags;
		public final String[]	opens_to;

		Open(String opens, int opens_flags, String[] opens_to) {
			this.opens = opens;
			this.opens_flags = opens_flags;
			this.opens_to = opens_to;
		}

		@Override
		public String toString() {
			return opens + " " + Arrays.toString(opens_to) + " " + opens_flags;
		}

		static Open parseOpen(DataInput in, ConstantPool constant_pool) throws IOException {
			int opens_index = in.readUnsignedShort();
			int opens_flags = in.readUnsignedShort();
			int opens_to_count = in.readUnsignedShort();
			String[] opens_to = new String[opens_to_count];
			for (int i = 0; i < opens_to_count; i++) {
				int opens_to_index = in.readUnsignedShort();
				opens_to[i] = constant_pool.moduleName(opens_to_index);
			}
			return new Open(constant_pool.packageName(opens_index), opens_flags, opens_to);
		}
	}

	public static class Provide {
		public final String		provides;
		public final String[]	provides_with;

		Provide(String provides, String[] provides_with) {
			this.provides = provides;
			this.provides_with = provides_with;
		}

		@Override
		public String toString() {
			return provides + " " + Arrays.toString(provides_with);
		}

		static Provide parseProvide(DataInput in, ConstantPool constant_pool) throws IOException {
			int provides_index = in.readUnsignedShort();
			int provides_with_count = in.readUnsignedShort();
			String[] provides_with = new String[provides_with_count];
			for (int i = 0; i < provides_with_count; i++) {
				int provides_with_index = in.readUnsignedShort();
				provides_with[i] = constant_pool.className(provides_with_index);
			}
			return new Provide(constant_pool.className(provides_index), provides_with);
		}
	}
}
