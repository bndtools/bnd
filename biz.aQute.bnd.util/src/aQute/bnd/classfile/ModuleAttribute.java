package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class ModuleAttribute implements Attribute {
	public static final String	NAME			= "Module";
	public static final int		ACC_OPEN		= 0x0020;
	public static final int		ACC_SYNTHETIC	= 0x1000;
	public static final int		ACC_MANDATED	= 0x8000;
	public final String			module_name;
	public final int			module_flags;
	public final String			module_version;
	public final Require[]		requires;
	public final Export[]		exports;
	public final Open[]			opens;
	public final String[]		uses;
	public final Provide[]		provides;

	public ModuleAttribute(String module_name, int module_flags, String module_version, Require[] requires,
		Export[] exports, Open[] opens, String[] uses, Provide[] provides) {
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

	public static ModuleAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int module_name_index = in.readUnsignedShort();
		int module_flags = in.readUnsignedShort();
		int module_version_index = in.readUnsignedShort();

		int requires_count = in.readUnsignedShort();
		Require[] requires = new Require[requires_count];
		for (int i = 0; i < requires_count; i++) {
			requires[i] = Require.read(in, constant_pool);
		}

		int exports_count = in.readUnsignedShort();
		Export[] exports = new Export[exports_count];
		for (int i = 0; i < exports_count; i++) {
			exports[i] = Export.read(in, constant_pool);
		}

		int opens_count = in.readUnsignedShort();
		Open[] opens = new Open[opens_count];
		for (int i = 0; i < opens_count; i++) {
			opens[i] = Open.read(in, constant_pool);
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
			provides[i] = Provide.read(in, constant_pool);
		}

		return new ModuleAttribute(constant_pool.moduleName(module_name_index), module_flags,
			(module_version_index != 0) ? constant_pool.utf8(module_version_index) : null, requires, exports, opens,
			uses, provides);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		int module_name_index = constant_pool.moduleInfo(module_name);
		int module_version_index = (module_version != null) ? constant_pool.utf8Info(module_version) : 0;
		out.writeShort(module_name_index);
		out.writeShort(module_flags);
		out.writeShort(module_version_index);

		out.writeShort(requires.length);
		for (Require require : requires) {
			require.write(out, constant_pool);
		}

		out.writeShort(exports.length);
		for (Export export : exports) {
			export.write(out, constant_pool);
		}

		out.writeShort(opens.length);
		for (Open open : opens) {
			open.write(out, constant_pool);
		}

		out.writeShort(uses.length);
		for (String use : uses) {
			int uses_index = constant_pool.classInfo(use);
			out.writeShort(uses_index);
		}

		out.writeShort(provides.length);
		for (Provide provide : provides) {
			provide.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = (8 + uses.length) * Short.BYTES;
		for (Require require : requires) {
			attribute_length += require.value_length();
		}
		for (Export export : exports) {
			attribute_length += export.value_length();
		}
		for (Open open : opens) {
			attribute_length += open.value_length();
		}
		for (Provide provide : provides) {
			attribute_length += provide.value_length();
		}
		return attribute_length;
	}

	public static class Require {
		public static final int	ACC_TRANSITIVE		= 0x0020;
		public static final int	ACC_STATIC_PHASE	= 0x0040;
		public static final int	ACC_SYNTHETIC		= 0x1000;
		public static final int	ACC_MANDATED		= 0x8000;
		public final String		requires;
		public final int		requires_flags;
		public final String		requires_version;

		public Require(String requires, int requires_flags, String requires_version) {
			this.requires = requires;
			this.requires_flags = requires_flags;
			this.requires_version = requires_version;
		}

		@Override
		public String toString() {
			return requires + " " + requires_version + " " + requires_flags;
		}

		static Require read(DataInput in, ConstantPool constant_pool) throws IOException {
			int requires_index = in.readUnsignedShort();
			int requires_flags = in.readUnsignedShort();
			int requires_version_index = in.readUnsignedShort();
			return new Require(constant_pool.moduleName(requires_index), requires_flags,
				(requires_version_index != 0) ? constant_pool.utf8(requires_version_index) : null);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeShort(constant_pool.moduleInfo(requires));
			out.writeShort(requires_flags);
			out.writeShort((requires_version != null) ? constant_pool.utf8Info(requires_version) : 0);
		}

		int value_length() {
			return 3 * Short.BYTES;
		}
	}

	public static class Export {
		public static final int	ACC_SYNTHETIC	= 0x1000;
		public static final int	ACC_MANDATED	= 0x8000;
		public final String		exports;
		public final int		exports_flags;
		public final String[]	exports_to;

		public Export(String exports, int exports_flags, String[] exports_to) {
			this.exports = exports;
			this.exports_flags = exports_flags;
			this.exports_to = exports_to;
		}

		@Override
		public String toString() {
			return exports + " " + Arrays.toString(exports_to) + " " + exports_flags;
		}

		static Export read(DataInput in, ConstantPool constant_pool) throws IOException {
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

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeShort(constant_pool.packageInfo(exports));
			out.writeShort(exports_flags);
			out.writeShort(exports_to.length);
			for (String to : exports_to) {
				out.writeShort(constant_pool.moduleInfo(to));
			}
		}

		int value_length() {
			return (3 + exports_to.length) * Short.BYTES;
		}
	}

	public static class Open {
		public static final int	ACC_SYNTHETIC	= 0x1000;
		public static final int	ACC_MANDATED	= 0x8000;
		public final String		opens;
		public final int		opens_flags;
		public final String[]	opens_to;

		public Open(String opens, int opens_flags, String[] opens_to) {
			this.opens = opens;
			this.opens_flags = opens_flags;
			this.opens_to = opens_to;
		}

		@Override
		public String toString() {
			return opens + " " + Arrays.toString(opens_to) + " " + opens_flags;
		}

		static Open read(DataInput in, ConstantPool constant_pool) throws IOException {
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

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeShort(constant_pool.packageInfo(opens));
			out.writeShort(opens_flags);
			out.writeShort(opens_to.length);
			for (String to : opens_to) {
				out.writeShort(constant_pool.moduleInfo(to));
			}
		}

		int value_length() {
			return (3 + opens_to.length) * Short.BYTES;
		}
	}

	public static class Provide {
		public final String		provides;
		public final String[]	provides_with;

		public Provide(String provides, String[] provides_with) {
			this.provides = provides;
			this.provides_with = provides_with;
		}

		@Override
		public String toString() {
			return provides + " " + Arrays.toString(provides_with);
		}

		static Provide read(DataInput in, ConstantPool constant_pool) throws IOException {
			int provides_index = in.readUnsignedShort();
			int provides_with_count = in.readUnsignedShort();
			String[] provides_with = new String[provides_with_count];
			for (int i = 0; i < provides_with_count; i++) {
				int provides_with_index = in.readUnsignedShort();
				provides_with[i] = constant_pool.className(provides_with_index);
			}
			return new Provide(constant_pool.className(provides_index), provides_with);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			out.writeShort(constant_pool.classInfo(provides));
			out.writeShort(provides_with.length);
			for (String with : provides_with) {
				out.writeShort(constant_pool.classInfo(with));
			}
		}

		int value_length() {
			return (2 + provides_with.length) * Short.BYTES;
		}
	}
}
