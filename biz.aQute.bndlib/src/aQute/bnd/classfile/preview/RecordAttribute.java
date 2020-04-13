package aQute.bnd.classfile.preview;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ConstantPool;

public class RecordAttribute implements Attribute {
	public static final String		NAME	= "Record";
	public final RecordComponent[]	components;

	public RecordAttribute(RecordComponent[] components) {
		this.components = components;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String toString() {
		return NAME + " " + Arrays.toString(components);
	}

	public static RecordAttribute read(DataInput in, ConstantPool constant_pool) throws IOException {
		int components_count = in.readUnsignedShort();
		RecordComponent[] components = new RecordComponent[components_count];
		for (int i = 0; i < components_count; i++) {
			components[i] = RecordComponent.read(in, constant_pool);
		}
		return new RecordAttribute(components);
	}

	@Override
	public void write(DataOutput out, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = constant_pool.utf8Info(name());
		int attribute_length = attribute_length();
		out.writeShort(attribute_name_index);
		out.writeInt(attribute_length);

		out.writeShort(components.length);
		for (RecordComponent component : components) {
			component.write(out, constant_pool);
		}
	}

	@Override
	public int attribute_length() {
		int attribute_length = 1 * Short.BYTES;
		for (RecordComponent component : components) {
			attribute_length += component.value_length();
		}
		return attribute_length;
	}

	public static class RecordComponent {
		public final String			name;
		public final String			descriptor;
		public final Attribute[]	attributes;

		public RecordComponent(String name, String descriptor, Attribute[] attributes) {
			this.name = name;
			this.descriptor = descriptor;
			this.attributes = attributes;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			return sb.append(name)
				.append(' ')
				.append(descriptor)
				.append(' ')
				.append(Arrays.toString(attributes))
				.toString();
		}

		static RecordComponent read(DataInput in, ConstantPool constant_pool) throws IOException {
			int name_index = in.readUnsignedShort();
			int descriptor_index = in.readUnsignedShort();
			Attribute[] attributes = Attribute.readAttributes(in, constant_pool);
			return new RecordComponent(constant_pool.utf8(name_index), constant_pool.utf8(descriptor_index),
				attributes);
		}

		void write(DataOutput out, ConstantPool constant_pool) throws IOException {
			int name_index = constant_pool.utf8Info(name);
			int descriptor_index = constant_pool.utf8Info(descriptor);
			out.writeShort(name_index);
			out.writeShort(descriptor_index);
			Attribute.writeAttributes(out, constant_pool, attributes);
		}

		int value_length() {
			int attribute_length = 2 * Short.BYTES;
			attribute_length += Attribute.attributes_length(attributes);
			return attribute_length;
		}
	}
}
