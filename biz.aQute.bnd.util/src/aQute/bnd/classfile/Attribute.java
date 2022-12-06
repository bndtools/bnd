package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.osgi.annotation.versioning.ProviderType;

import aQute.lib.io.LimitedDataInput;

@ProviderType
public interface Attribute {
	String name();

	void write(DataOutput out, ConstantPool constant_pool) throws IOException;

	int attribute_length();

	static Attribute[] readAttributes(DataInput in, ConstantPool constant_pool) throws IOException {
		int attributes_count = in.readUnsignedShort();
		Attribute[] attributes = new Attribute[attributes_count];
		for (int i = 0; i < attributes_count; i++) {
			attributes[i] = readAttribute(in, constant_pool);
		}

		return attributes;
	}

	static Attribute readAttribute(DataInput in, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = in.readUnsignedShort();
		int attribute_length = in.readInt();
		String attribute_name = constant_pool.utf8(attribute_name_index);
		in = LimitedDataInput.wrap(in, attribute_length);
		return switch (attribute_name) {
			case AnnotationDefaultAttribute.NAME -> AnnotationDefaultAttribute.read(in, constant_pool);
			case BootstrapMethodsAttribute.NAME -> BootstrapMethodsAttribute.read(in, constant_pool);
			case CodeAttribute.NAME -> CodeAttribute.read(in, constant_pool);
			case ConstantValueAttribute.NAME -> ConstantValueAttribute.read(in, constant_pool);
			case DeprecatedAttribute.NAME -> DeprecatedAttribute.read(in, constant_pool);
			case EnclosingMethodAttribute.NAME -> EnclosingMethodAttribute.read(in, constant_pool);
			case ExceptionsAttribute.NAME -> ExceptionsAttribute.read(in, constant_pool);
			case InnerClassesAttribute.NAME -> InnerClassesAttribute.read(in, constant_pool);
			case LineNumberTableAttribute.NAME -> LineNumberTableAttribute.read(in, constant_pool);
			case LocalVariableTableAttribute.NAME -> LocalVariableTableAttribute.read(in, constant_pool);
			case LocalVariableTypeTableAttribute.NAME -> LocalVariableTypeTableAttribute.read(in, constant_pool);
			case MethodParametersAttribute.NAME -> MethodParametersAttribute.read(in, constant_pool);
			case ModuleAttribute.NAME -> ModuleAttribute.read(in, constant_pool);
			case ModuleMainClassAttribute.NAME -> ModuleMainClassAttribute.read(in, constant_pool);
			case ModulePackagesAttribute.NAME -> ModulePackagesAttribute.read(in, constant_pool);
			case NestHostAttribute.NAME -> NestHostAttribute.read(in, constant_pool);
			case NestMembersAttribute.NAME -> NestMembersAttribute.read(in, constant_pool);
			case RuntimeInvisibleAnnotationsAttribute.NAME -> RuntimeInvisibleAnnotationsAttribute.read(in,
				constant_pool);
			case RuntimeInvisibleParameterAnnotationsAttribute.NAME -> RuntimeInvisibleParameterAnnotationsAttribute
				.read(in, constant_pool);
			case RuntimeInvisibleTypeAnnotationsAttribute.NAME -> RuntimeInvisibleTypeAnnotationsAttribute.read(in,
				constant_pool);
			case RuntimeVisibleAnnotationsAttribute.NAME -> RuntimeVisibleAnnotationsAttribute.read(in, constant_pool);
			case RuntimeVisibleParameterAnnotationsAttribute.NAME -> RuntimeVisibleParameterAnnotationsAttribute
				.read(in, constant_pool);
			case RuntimeVisibleTypeAnnotationsAttribute.NAME -> RuntimeVisibleTypeAnnotationsAttribute.read(in,
				constant_pool);
			case SignatureAttribute.NAME -> SignatureAttribute.read(in, constant_pool);
			case SourceDebugExtensionAttribute.NAME -> SourceDebugExtensionAttribute.read(in, attribute_length);
			case SourceFileAttribute.NAME -> SourceFileAttribute.read(in, constant_pool);
			case StackMapTableAttribute.NAME -> StackMapTableAttribute.read(in, constant_pool);
			case SyntheticAttribute.NAME -> SyntheticAttribute.read(in, constant_pool);
			case RecordAttribute.NAME -> RecordAttribute.read(in, constant_pool);
			case PermittedSubclassesAttribute.NAME -> PermittedSubclassesAttribute.read(in, constant_pool);
			default -> UnrecognizedAttribute.read(in, attribute_name, attribute_length);
		};
	}

	static void writeAttributes(DataOutput out, ConstantPool constant_pool, Attribute[] attributes) throws IOException {
		out.writeShort(attributes.length);
		for (Attribute attribute : attributes) {
			attribute.write(out, constant_pool);
		}
	}

	static int attributes_length(Attribute[] attributes) {
		int attribute_length = 1 * Short.BYTES;
		for (Attribute attribute : attributes) {
			attribute_length += 1 * Short.BYTES + 1 * Integer.BYTES + attribute.attribute_length();
		}
		return attribute_length;
	}
}
