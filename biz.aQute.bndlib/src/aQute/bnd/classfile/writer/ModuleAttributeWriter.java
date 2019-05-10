package aQute.bnd.classfile.writer;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import aQute.bnd.classfile.ModuleAttribute;

public class ModuleAttributeWriter implements AttributeWriter {
	static class Requires {
		final int	requires_index;
		final int	requires_flags;
		final int	requires_version_index;

		Requires(ConstantPoolWriter constantPool, ModuleAttribute.Require require) {
			this.requires_index = constantPool.moduleInfo(require.requires);
			this.requires_flags = require.requires_flags;
			this.requires_version_index = (require.requires_version != null)
				? constantPool.utf8Info(require.requires_version)
				: 0;
		}

		int length() {
			return 3 * (Short.SIZE / Byte.SIZE);
		}
	}

	static class Exports {
		final int	exports_index;
		final int	exports_flags;
		final int[]	exports_to_index;

		Exports(ConstantPoolWriter constantPool, ModuleAttribute.Export export) {
			this.exports_index = constantPool.packageInfo(export.exports);
			this.exports_flags = export.exports_flags;
			this.exports_to_index = Arrays.stream(export.exports_to)
				.mapToInt(constantPool::moduleInfo)
				.toArray();
		}

		int length() {
			return (3 + exports_to_index.length) * (Short.SIZE / Byte.SIZE);
		}
	}

	static class Opens {
		final int	opens_index;
		final int	opens_flags;
		final int[]	opens_to_index;

		Opens(ConstantPoolWriter constantPool, ModuleAttribute.Open open) {
			this.opens_index = constantPool.packageInfo(open.opens);
			this.opens_flags = open.opens_flags;
			this.opens_to_index = Arrays.stream(open.opens_to)
				.mapToInt(constantPool::moduleInfo)
				.toArray();
		}

		int length() {
			return (3 + opens_to_index.length) * (Short.SIZE / Byte.SIZE);
		}
	}

	static class Provides {
		final int	provides_index;
		final int[]	provides_with_index;

		Provides(ConstantPoolWriter constantPool, ModuleAttribute.Provide provide) {
			this.provides_index = constantPool.classInfo(provide.provides);
			this.provides_with_index = Arrays.stream(provide.provides_with)
				.mapToInt(constantPool::classInfo)
				.toArray();
		}

		int length() {
			return (2 + provides_with_index.length) * (Short.SIZE / Byte.SIZE);
		}
	}

	private final int			attribute_name_index;
	private final int			attribute_length;
	private final int			module_name_index;
	private final int			module_flags;
	private final int			module_version_index;
	private final Requires[]	requires;
	private final Exports[]		exports;
	private final Opens[]		opens;
	private final int[]			uses;
	private final Provides[]	provides;

	public ModuleAttributeWriter(ConstantPoolWriter constantPool, String module_name, int module_flags,
		String module_version, Collection<ModuleAttribute.Require> requires, Collection<ModuleAttribute.Export> exports,
		Collection<ModuleAttribute.Open> opens, Set<String> uses, Collection<ModuleAttribute.Provide> provides) {
		attribute_name_index = constantPool.utf8Info(ModuleAttribute.NAME);
		module_name_index = constantPool.moduleInfo(module_name);
		this.module_flags = module_flags;
		module_version_index = (module_version != null) ? constantPool.utf8Info(module_version) : 0;

		this.requires = requires.stream()
			.map(require -> new Requires(constantPool, require))
			.toArray(Requires[]::new);

		this.exports = exports.stream()
			.map(export -> new Exports(constantPool, export))
			.toArray(Exports[]::new);

		this.opens = opens.stream()
			.map(open -> new Opens(constantPool, open))
			.toArray(Opens[]::new);

		this.uses = uses.stream()
			.mapToInt(constantPool::classInfo)
			.toArray();

		this.provides = provides.stream()
			.map(provide -> new Provides(constantPool, provide))
			.toArray(Provides[]::new);

		attribute_length = (8 + this.uses.length) * (Short.SIZE / Byte.SIZE) //
			+ Arrays.stream(this.requires)
				.mapToInt(Requires::length)
				.sum()
			+ Arrays.stream(this.exports)
				.mapToInt(Exports::length)
				.sum()
			+ Arrays.stream(this.opens)
				.mapToInt(Opens::length)
				.sum()
			+ Arrays.stream(this.provides)
				.mapToInt(Provides::length)
				.sum();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeShort(module_name_index);
		out.writeShort(module_flags);
		out.writeShort(module_version_index);

		out.writeShort(requires.length);
		for (Requires i : requires) {
			out.writeShort(i.requires_index);
			out.writeShort(i.requires_flags);
			out.writeShort(i.requires_version_index);
		}

		out.writeShort(exports.length);
		for (Exports i : exports) {
			out.writeShort(i.exports_index);
			out.writeShort(i.exports_flags);
			out.writeShort(i.exports_to_index.length);
			for (int j : i.exports_to_index) {
				out.writeShort(j);
			}
		}

		out.writeShort(opens.length);
		for (Opens i : opens) {
			out.writeShort(i.opens_index);
			out.writeShort(i.opens_flags);
			out.writeShort(i.opens_to_index.length);
			for (int j : i.opens_to_index) {
				out.writeShort(j);
			}
		}

		out.writeShort(uses.length);
		for (int i : uses) {
			out.writeShort(i);
		}

		out.writeShort(provides.length);
		for (Provides i : provides) {
			out.writeShort(i.provides_index);
			out.writeShort(i.provides_with_index.length);
			for (int j : i.provides_with_index) {
				out.writeShort(j);
			}
		}
	}
}
