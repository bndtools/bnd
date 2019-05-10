package aQute.bnd.classfile.writer;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

import aQute.bnd.classfile.ModulePackagesAttribute;

public class ModulePackagesAttributeWriter implements AttributeWriter {
	private final int	attribute_name_index;
	private final int	attribute_length;
	private final int[]	package_index;

	public ModulePackagesAttributeWriter(ConstantPoolWriter constantPool, Set<String> packages) {
		attribute_name_index = constantPool.utf8Info(ModulePackagesAttribute.NAME);
		package_index = packages.stream()
			.mapToInt(constantPool::packageInfo)
			.toArray();
		attribute_length = (1 + package_index.length) * (Short.SIZE / Byte.SIZE);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		out.writeShort(package_index.length);
		for (int pkg : package_index) {
			out.writeShort(pkg);
		}
	}
}
