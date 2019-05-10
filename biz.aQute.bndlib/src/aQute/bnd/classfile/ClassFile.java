package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;

import aQute.lib.io.ByteBufferDataInput;

public class ClassFile extends ElementInfo {
	public static final int		MAJOR_VERSION	= 55;
	public static final int		ACC_ANNOTATION	= 0x2000;
	public static final int		ACC_ENUM		= 0x4000;
	public static final int		ACC_MODULE		= 0x8000;

	public final int			minor_version;
	public final int			major_version;
	public final ConstantPool	constant_pool;
	public final String			this_class;
	public final String			super_class;
	public final String[]		interfaces;
	public final FieldInfo[]	fields;
	public final MethodInfo[]	methods;

	public ClassFile(int minor_version, int major_version, ConstantPool constant_pool, int access_flags,
		String this_class, String super_class, String[] interfaces, FieldInfo[] fields, MethodInfo[] methods,
		Attribute[] attributes) {
		super(access_flags, attributes);
		this.minor_version = minor_version;
		this.major_version = major_version;
		this.constant_pool = constant_pool;
		this.this_class = this_class;
		this.super_class = super_class;
		this.interfaces = interfaces;
		this.fields = fields;
		this.methods = methods;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Modifier.toString(access & Modifier.classModifiers()));
		if (sb.length() > 0) {
			sb.append(' ');
		}
		if ((access & Modifier.INTERFACE) != 0) {
			if ((access & ACC_ANNOTATION) != 0) {
				sb.append('@');
			}
			sb.append("interface ");
		} else if ((access & ACC_ENUM) != 0) {
			sb.append("enum ");
		} else if ((access & ACC_MODULE) == 0) {
			sb.append("class ");
		}
		return sb.append(this_class)
			.append(' ')
			.append(Arrays.toString(attributes))
			.toString();
	}

	public static ClassFile parseClassFile(DataInput in) throws IOException {
		int magic = in.readInt();
		if (magic != 0xCAFEBABE) {
			throw new IOException("Not a valid class file (no CAFEBABE header)");
		}

		int minor_version = in.readUnsignedShort();
		int major_version = in.readUnsignedShort();

		ConstantPool constant_pool = ConstantPool.read(in);

		int access_flags = in.readUnsignedShort();

		int this_class_index = in.readUnsignedShort();
		String this_class = constant_pool.className(this_class_index);

		int super_class_index = in.readUnsignedShort();
		String super_class = (super_class_index != 0) ? constant_pool.className(super_class_index) : null;

		int interfaces_count = in.readUnsignedShort();
		String[] interfaces = new String[interfaces_count];
		for (int i = 0; i < interfaces_count; i++) {
			int interface_index = in.readUnsignedShort();
			interfaces[i] = constant_pool.className(interface_index);
		}

		int fields_count = in.readUnsignedShort();
		FieldInfo[] fields = new FieldInfo[fields_count];
		for (int i = 0; i < fields_count; i++) {
			fields[i] = FieldInfo.read(in, constant_pool);
		}

		int methods_count = in.readUnsignedShort();
		MethodInfo[] methods = new MethodInfo[methods_count];
		for (int i = 0; i < methods_count; i++) {
			methods[i] = MethodInfo.read(in, constant_pool);
		}

		Attribute[] attributes = readAttributes(in, constant_pool);

		ClassFile class_file = new ClassFile(minor_version, major_version, constant_pool, access_flags, this_class,
			super_class, interfaces, fields, methods, attributes);
		return class_file;
	}

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

	static ByteBuffer slice(DataInput in, int length) throws IOException {
		if (in instanceof ByteBufferDataInput) {
			ByteBufferDataInput bbin = (ByteBufferDataInput) in;
			return bbin.slice(length);
		}
		byte[] array = new byte[length];
		in.readFully(array, 0, length);
		return ByteBuffer.wrap(array, 0, length);
	}
}
