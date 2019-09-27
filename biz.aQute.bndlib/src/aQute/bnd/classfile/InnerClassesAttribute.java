package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class InnerClassesAttribute implements Attribute {
	public static final String	NAME	= "InnerClasses";
	public final InnerClass[]	classes;

	public InnerClassesAttribute(InnerClass[] classes) {
		this.classes = classes;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(classes);
	}

	public static InnerClassesAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int number_of_classes = in.readUnsignedShort();
		InnerClass[] classes = new InnerClass[number_of_classes];
		for (int i = 0; i < number_of_classes; i++) {
			classes[i] = InnerClass.read(in, constant_pool);
		}
		return new InnerClassesAttribute(classes);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeShort(classes.length);
		for (InnerClass inner : classes) {
			inner.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		for (InnerClass inner : classes) {
			attribute_length += inner.value_length();
		}
		return attribute_length;
	}

	public static class InnerClass {
		public final String	inner_class;
		public final String	outer_class;
		public final String	inner_name;
		public final int	inner_access;

		public InnerClass(String inner_class, String outer_class, String inner_name, int inner_access) {
			this.inner_class = inner_class;
			this.outer_class = outer_class;
			this.inner_name = inner_name;
			this.inner_access = inner_access;
		}

		@Override
		public String toString() {
			return String.format("inner_class=%s outer_class=%s inner_name=%s inner_access=%d", inner_class,
				outer_class, inner_name, inner_access);
		}

		static InnerClass read(DataInput in, ConstantPool constant_pool) throws IOException {
			int inner_class_info_index = in.readUnsignedShort();
			int outer_class_info_index = in.readUnsignedShort();
			int inner_name_index = in.readUnsignedShort();
			int inner_class_access_flags = in.readUnsignedShort();
			String inner_class = constant_pool.className(inner_class_info_index);
			String outer_class = (outer_class_info_index != 0) ? constant_pool.className(outer_class_info_index) : null;
			String inner_name = (inner_name_index != 0) ? constant_pool.utf8(inner_name_index) : null;
			return new InnerClass(inner_class, outer_class, inner_name, inner_class_access_flags);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			int inner_class_info_index = constant_pool.classInfo(inner_class);
			int outer_class_info_index = (outer_class != null) ? constant_pool.classInfo(outer_class) : 0;
			int inner_name_index = (inner_name != null) ? constant_pool.utf8Info(inner_name) : 0;
			out.writeShort(inner_class_info_index);
			out.writeShort(outer_class_info_index);
			out.writeShort(inner_name_index);
			out.writeShort(inner_access);
		}

		int value_length() {
			return 4 * Short.BYTES;
		}
	}
}
