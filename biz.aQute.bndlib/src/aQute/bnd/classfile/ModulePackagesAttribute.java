package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class ModulePackagesAttribute implements Attribute {
	public static final String	NAME	= "ModulePackages";
	public final String[]		packages;

	public ModulePackagesAttribute(String[] packages) {
		this.packages = packages;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(packages);
	}

	public static ModulePackagesAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int package_count = in.readUnsignedShort();
		String[] packages = new String[package_count];
		for (int i = 0; i < package_count; i++) {
			int package_index = in.readUnsignedShort();
			packages[i] = constant_pool.packageName(package_index);
		}
		return new ModulePackagesAttribute(packages);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);
		out.writeShort(packages.length);
		for (String pkg : packages) {
			int package_index = constant_pool.packageInfo(pkg);
			out.writeShort(package_index);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = (1 + packages.length) * Short.BYTES;
		return attribute_length;
	}
}
