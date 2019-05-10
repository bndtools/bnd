package aQute.bnd.classfile;

import java.io.DataInput;
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

	static ModulePackagesAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int package_count = in.readUnsignedShort();
		String[] packages = new String[package_count];
		for (int i = 0; i < package_count; i++) {
			int package_index = in.readUnsignedShort();
			packages[i] = constant_pool.packageName(package_index);
		}
		return new ModulePackagesAttribute(packages);
	}
}
