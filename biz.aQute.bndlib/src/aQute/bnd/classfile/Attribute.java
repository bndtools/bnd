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
		switch (attribute_name) {
			case AnnotationDefaultAttribute.NAME : {
				return AnnotationDefaultAttribute.read(in, constant_pool);
			}
			case BootstrapMethodsAttribute.NAME : {
				return BootstrapMethodsAttribute.read(in, constant_pool);
			}
			case CodeAttribute.NAME : {
				return CodeAttribute.read(in, constant_pool);
			}
			case ConstantValueAttribute.NAME : {
				return ConstantValueAttribute.read(in, constant_pool);
			}
			case DeprecatedAttribute.NAME : {
				return DeprecatedAttribute.read(in, constant_pool);
			}
			case EnclosingMethodAttribute.NAME : {
				return EnclosingMethodAttribute.read(in, constant_pool);
			}
			case ExceptionsAttribute.NAME : {
				return ExceptionsAttribute.read(in, constant_pool);
			}
			case InnerClassesAttribute.NAME : {
				return InnerClassesAttribute.read(in, constant_pool);
			}
			case LineNumberTableAttribute.NAME : {
				return LineNumberTableAttribute.read(in, constant_pool);
			}
			case LocalVariableTableAttribute.NAME : {
				return LocalVariableTableAttribute.read(in, constant_pool);
			}
			case LocalVariableTypeTableAttribute.NAME : {
				return LocalVariableTypeTableAttribute.read(in, constant_pool);
			}
			case MethodParametersAttribute.NAME : {
				return MethodParametersAttribute.read(in, constant_pool);
			}
			case ModuleAttribute.NAME : {
				return ModuleAttribute.read(in, constant_pool);
			}
			case ModuleMainClassAttribute.NAME : {
				return ModuleMainClassAttribute.read(in, constant_pool);
			}
			case ModulePackagesAttribute.NAME : {
				return ModulePackagesAttribute.read(in, constant_pool);
			}
			case NestHostAttribute.NAME : {
				return NestHostAttribute.read(in, constant_pool);
			}
			case NestMembersAttribute.NAME : {
				return NestMembersAttribute.read(in, constant_pool);
			}
			case RuntimeInvisibleAnnotationsAttribute.NAME : {
				return RuntimeInvisibleAnnotationsAttribute.read(in, constant_pool);
			}
			case RuntimeInvisibleParameterAnnotationsAttribute.NAME : {
				return RuntimeInvisibleParameterAnnotationsAttribute.read(in, constant_pool);
			}
			case RuntimeInvisibleTypeAnnotationsAttribute.NAME : {
				return RuntimeInvisibleTypeAnnotationsAttribute.read(in, constant_pool);
			}
			case RuntimeVisibleAnnotationsAttribute.NAME : {
				return RuntimeVisibleAnnotationsAttribute.read(in, constant_pool);
			}
			case RuntimeVisibleParameterAnnotationsAttribute.NAME : {
				return RuntimeVisibleParameterAnnotationsAttribute.read(in, constant_pool);
			}
			case RuntimeVisibleTypeAnnotationsAttribute.NAME : {
				return RuntimeVisibleTypeAnnotationsAttribute.read(in, constant_pool);
			}
			case SignatureAttribute.NAME : {
				return SignatureAttribute.read(in, constant_pool);
			}
			case SourceDebugExtensionAttribute.NAME : {
				return SourceDebugExtensionAttribute.read(in, attribute_length);
			}
			case SourceFileAttribute.NAME : {
				return SourceFileAttribute.read(in, constant_pool);
			}
			case StackMapTableAttribute.NAME : {
				return StackMapTableAttribute.read(in, constant_pool);
			}
			case SyntheticAttribute.NAME : {
				return SyntheticAttribute.read(in, constant_pool);
			}
			default : {
				return UnrecognizedAttribute.read(in, attribute_name, attribute_length);
			}
		}
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
			attribute_length += 1 * Short.BYTES + 1 * Integer.BYTES
				+ attribute.attribute_length();
		}
		return attribute_length;
	}
}
