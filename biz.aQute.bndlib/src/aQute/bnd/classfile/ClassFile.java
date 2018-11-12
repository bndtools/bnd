package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;

import aQute.lib.io.ByteBufferDataInput;

public class ClassFile extends ElementInfo {
	public static final int		MAJOR_VERSION	= 55;
	private static final int	ACC_ANNOTATION	= 0x2000;
	private static final int	ACC_ENUM		= 0x4000;
	private static final int	ACC_MODULE		= 0x8000;

	public final int			minor_version;
	public final int			major_version;
	public final ConstantPool	constant_pool;
	public final String			this_class;
	public final String			super_class;
	public final String[]		interfaces;
	public final FieldInfo[]	fields;
	public final MethodInfo[]	methods;

	ClassFile(int minor_version, int major_version, ConstantPool constant_pool, int access_flags, String this_class,
		String super_class, String[] interfaces, FieldInfo[] fields, MethodInfo[] methods, Attribute[] attributes) {
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

		ConstantPool constant_pool = ConstantPool.parseConstantPool(in);

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
			fields[i] = FieldInfo.parseFieldInfo(in, constant_pool);
		}

		int methods_count = in.readUnsignedShort();
		MethodInfo[] methods = new MethodInfo[methods_count];
		for (int i = 0; i < methods_count; i++) {
			methods[i] = MethodInfo.parseMethodInfo(in, constant_pool);
		}

		Attribute[] attributes = parseAttributes(in, constant_pool);

		ClassFile class_file = new ClassFile(minor_version, major_version, constant_pool, access_flags, this_class,
			super_class, interfaces, fields, methods, attributes);
		return class_file;
	}

	static Attribute[] parseAttributes(DataInput in, ConstantPool constant_pool) throws IOException {
		int attributes_count = in.readUnsignedShort();
		Attribute[] attributes = new Attribute[attributes_count];
		for (int i = 0; i < attributes_count; i++) {
			attributes[i] = parseAttribute(in, constant_pool);
		}

		return attributes;
	}

	static Attribute parseAttribute(DataInput in, ConstantPool constant_pool) throws IOException {
		int attribute_name_index = in.readUnsignedShort();
		int attribute_length = in.readInt();
		String attribute_name = constant_pool.utf8(attribute_name_index);
		switch (attribute_name) {
			case AnnotationDefaultAttribute.NAME : {
				return AnnotationDefaultAttribute.parseAnnotationDefaultAttribute(in, constant_pool);
			}
			case BootstrapMethodsAttribute.NAME : {
				return BootstrapMethodsAttribute.parseBootstrapMethodsAttribute(in, constant_pool);
			}
			case CodeAttribute.NAME : {
				return CodeAttribute.parseCodeAttribute(in, constant_pool);
			}
			case ConstantValueAttribute.NAME : {
				return ConstantValueAttribute.parseConstantValueAttribute(in, constant_pool);
			}
			case DeprecatedAttribute.NAME : {
				return DeprecatedAttribute.parseDeprecatedAttribute(in, constant_pool);
			}
			case EnclosingMethodAttribute.NAME : {
				return EnclosingMethodAttribute.parseEnclosingMethodAttribute(in, constant_pool);
			}
			case ExceptionsAttribute.NAME : {
				return ExceptionsAttribute.parseExceptionsAttribute(in, constant_pool);
			}
			case InnerClassesAttribute.NAME : {
				return InnerClassesAttribute.parseInnerClassesAttribute(in, constant_pool);
			}
			case LineNumberTableAttribute.NAME : {
				return LineNumberTableAttribute.parseLineNumberTableAttribute(in, constant_pool);
			}
			case LocalVariableTableAttribute.NAME : {
				return LocalVariableTableAttribute.parseLocalVariableTableAttribute(in, constant_pool);
			}
			case LocalVariableTypeTableAttribute.NAME : {
				return LocalVariableTypeTableAttribute.parseLocalVariableTypeTableAttribute(in, constant_pool);
			}
			case MethodParametersAttribute.NAME : {
				return MethodParametersAttribute.parseMethodParametersAttribute(in, constant_pool);
			}
			case ModuleAttribute.NAME : {
				return ModuleAttribute.parseModuleAttribute(in, constant_pool);
			}
			case ModuleMainClassAttribute.NAME : {
				return ModuleMainClassAttribute.parseModuleMainClassAttribute(in, constant_pool);
			}
			case ModulePackagesAttribute.NAME : {
				return ModulePackagesAttribute.parseModulePackagesAttribute(in, constant_pool);
			}
			case NestHostAttribute.NAME : {
				return NestHostAttribute.parseNestHostAttribute(in, constant_pool);
			}
			case NestMembersAttribute.NAME : {
				return NestMembersAttribute.parseNestMembersAttribute(in, constant_pool);
			}
			case RuntimeInvisibleAnnotationsAttribute.NAME : {
				return RuntimeInvisibleAnnotationsAttribute.parseRuntimeInvisibleAnnotationsAttribute(in,
					constant_pool);
			}
			case RuntimeInvisibleParameterAnnotationsAttribute.NAME : {
				return RuntimeInvisibleParameterAnnotationsAttribute
					.parseRuntimeInvisibleParameterAnnotationsAttribute(in, constant_pool);
			}
			case RuntimeInvisibleTypeAnnotationsAttribute.NAME : {
				return RuntimeInvisibleTypeAnnotationsAttribute.parseRuntimeInvisibleTypeAnnotationsAttribute(in,
					constant_pool);
			}
			case RuntimeVisibleAnnotationsAttribute.NAME : {
				return RuntimeVisibleAnnotationsAttribute.parseRuntimeVisibleAnnotationsAttribute(in, constant_pool);
			}
			case RuntimeVisibleParameterAnnotationsAttribute.NAME : {
				return RuntimeVisibleParameterAnnotationsAttribute.parseRuntimeVisibleParameterAnnotationsAttribute(in,
					constant_pool);
			}
			case RuntimeVisibleTypeAnnotationsAttribute.NAME : {
				return RuntimeVisibleTypeAnnotationsAttribute.parseRuntimeVisibleTypeAnnotationsAttribute(in,
					constant_pool);
			}
			case SignatureAttribute.NAME : {
				return SignatureAttribute.parseSignatureAttribute(in, constant_pool);
			}
			case SourceDebugExtensionAttribute.NAME : {
				return SourceDebugExtensionAttribute.parseSourceDebugExtensionAttribute(in, attribute_length);
			}
			case SourceFileAttribute.NAME : {
				return SourceFileAttribute.parseSourceFileAttribute(in, constant_pool);
			}
			case StackMapTableAttribute.NAME : {
				return StackMapTableAttribute.parseStackMapTableAttribute(in, constant_pool);
			}
			case SyntheticAttribute.NAME : {
				return SyntheticAttribute.parseSyntheticAttribute(in, constant_pool);
			}
			default : {
				return UnrecognizedAttribute.parseUnrecognizedAttribute(in, attribute_name, attribute_length);
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
