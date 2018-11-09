package aQute.bnd.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

public class ConstantPool {
	public static final int	CONSTANT_Utf8				= 1;
	public static final int	CONSTANT_Integer			= 3;
	public static final int	CONSTANT_Float				= 4;
	public static final int	CONSTANT_Long				= 5;
	public static final int	CONSTANT_Double				= 6;
	public static final int	CONSTANT_Class				= 7;
	public static final int	CONSTANT_String				= 8;
	public static final int	CONSTANT_Fieldref			= 9;
	public static final int	CONSTANT_Methodref			= 10;
	public static final int	CONSTANT_InterfaceMethodref	= 11;
	public static final int	CONSTANT_NameAndType		= 12;
	public static final int	CONSTANT_MethodHandle		= 15;
	public static final int	CONSTANT_MethodType			= 16;
	public static final int	CONSTANT_Dynamic			= 17;
	public static final int	CONSTANT_InvokeDynamic		= 18;
	public static final int	CONSTANT_Module				= 19;
	public static final int	CONSTANT_Package			= 20;

	final Object[]			pool;

	ConstantPool(int constant_pool_count) {
		this.pool = new Object[constant_pool_count];
	}

	public int size() {
		return pool.length;
	}

	@SuppressWarnings("unchecked")
	public <T> T entry(int index) {
		return (T) pool[index];
	}

	public int tag(int index) {
		Object entry = pool[index];
		if (entry instanceof Info) {
			return ((Info) entry).tag();
		} else if (entry instanceof String) {
			return CONSTANT_Utf8;
		} else if (entry instanceof Integer) {
			return CONSTANT_Integer;
		} else if (entry instanceof Long) {
			return CONSTANT_Long;
		} else if (entry instanceof Float) {
			return CONSTANT_Float;
		} else if (entry instanceof Double) {
			return CONSTANT_Double;
		} else {
			return 0;
		}
	}

	public String utf8(int utf8_index) {
		return (String) pool[utf8_index];
	}

	public String className(int class_info_index) {
		ClassInfo classInfo = entry(class_info_index);
		return utf8(classInfo.class_index);
	}

	public String moduleName(int module_info_index) {
		ModuleInfo moduleInfo = entry(module_info_index);
		return utf8(moduleInfo.name_index);
	}

	public String packageName(int package_info_index) {
		PackageInfo packageInfo = entry(package_info_index);
		return utf8(packageInfo.name_index);
	}

	public String string(int string_info_index) {
		StringInfo stringInfo = entry(string_info_index);
		return utf8(stringInfo.string_index);
	}

	@Override
	public String toString() {
		return Arrays.deepToString(pool);
	}

	static ConstantPool parseConstantPool(DataInput in) throws IOException {
		int constant_pool_count = in.readUnsignedShort();
		ConstantPool constant_pool = new ConstantPool(constant_pool_count);
		Object[] pool = constant_pool.pool;
		for (int index = 1; index < constant_pool_count; index++) {
			int tag = in.readUnsignedByte();
			switch (tag) {
				case CONSTANT_Utf8 : {
					pool[index] = parseUtf8Info(in);
					break;
				}
				case CONSTANT_Integer : {
					pool[index] = parseIntegerInfo(in);
					break;
				}
				case CONSTANT_Float : {
					pool[index] = parseFloatInfo(in);
					break;
				}
				case CONSTANT_Long : {
					pool[index] = parseLongInfo(in);
					// For some insane optimization reason, the Long(5) and
					// Double(6) entries take two slots in the constant pool.
					// See 4.4.5
					index++;
					break;
				}
				case CONSTANT_Double : {
					pool[index] = parseDoubleInfo(in);
					// For some insane optimization reason, the Long(5) and
					// Double(6) entries take two slots in the constant pool.
					// See 4.4.5
					index++;
					break;
				}
				case CONSTANT_Class : {
					pool[index] = parseClassInfo(in);
					break;
				}
				case CONSTANT_String : {
					pool[index] = parseStringInfo(in);
					break;
				}
				case CONSTANT_Fieldref : {
					pool[index] = parseFieldrefInfo(in);
					break;
				}
				case CONSTANT_Methodref : {
					pool[index] = parseMethodrefInfo(in);
					break;
				}
				case CONSTANT_InterfaceMethodref : {
					pool[index] = parseInterfaceMethodrefInfo(in);
					break;
				}
				case CONSTANT_NameAndType : {
					pool[index] = parseNameAndTypeInfo(in);
					break;
				}
				case CONSTANT_MethodHandle : {
					pool[index] = parseMethodHandleInfo(in);
					break;
				}
				case CONSTANT_MethodType : {
					pool[index] = parseMethodTypeInfo(in);
					break;
				}
				case CONSTANT_Dynamic : {
					pool[index] = parseDynamicInfo(in);
					break;
				}
				case CONSTANT_InvokeDynamic : {
					pool[index] = parseInvokeDynamicInfo(in);
					break;
				}
				case CONSTANT_Module : {
					pool[index] = parseModuleInfo(in);
					break;
				}
				case CONSTANT_Package : {
					pool[index] = parsePackageInfo(in);
					break;
				}
				default : {
					throw new IOException("Unrecognized constant pool tag value " + tag + " at index " + index);
				}
			}
		}

		return constant_pool;
	}

	static String parseUtf8Info(DataInput in) throws IOException {
		String constant = in.readUTF();
		return constant.intern();
	}

	static Integer parseIntegerInfo(DataInput in) throws IOException {
		int constant = in.readInt();
		return Integer.valueOf(constant);
	}

	static Float parseFloatInfo(DataInput in) throws IOException {
		float constant = in.readFloat();
		return Float.valueOf(constant);
	}

	static Long parseLongInfo(DataInput in) throws IOException {
		long constant = in.readLong();
		return Long.valueOf(constant);
	}

	static Double parseDoubleInfo(DataInput in) throws IOException {
		double constant = in.readDouble();
		return Double.valueOf(constant);
	}

	public interface Info {
		int tag();
	}

	public static class ClassInfo implements Info {
		public final int class_index;

		ClassInfo(int class_index) {
			this.class_index = class_index;
		}

		@Override
		public int tag() {
			return CONSTANT_Class;
		}

		@Override
		public String toString() {
			return "ClassInfo:" + class_index;
		}
	}

	static ClassInfo parseClassInfo(DataInput in) throws IOException {
		int name_index = in.readUnsignedShort();
		return new ClassInfo(name_index);
	}

	public static class StringInfo implements Info {
		public final int string_index;

		StringInfo(int string_index) {
			this.string_index = string_index;
		}

		@Override
		public int tag() {
			return CONSTANT_String;
		}

		@Override
		public String toString() {
			return "StringInfo:" + string_index;
		}
	}

	static StringInfo parseStringInfo(DataInput in) throws IOException {
		int string_index = in.readUnsignedShort();
		return new StringInfo(string_index);
	}

	public abstract static class AbstractRefInfo implements Info {
		public final int	class_index;
		public final int	name_and_type_index;

		AbstractRefInfo(int class_index, int name_and_type_index) {
			this.class_index = class_index;
			this.name_and_type_index = name_and_type_index;
		}
	}

	@FunctionalInterface
	interface IntBiFunction<R> {
		R apply(int a, int b);
	}

	static <R extends AbstractRefInfo> R parseRefInfo(DataInput in, IntBiFunction<R> constructor) throws IOException {
		int class_index = in.readUnsignedShort();
		int name_and_type_index = in.readUnsignedShort();
		return constructor.apply(class_index, name_and_type_index);
	}

	public static class FieldrefInfo extends AbstractRefInfo {
		FieldrefInfo(int class_index, int name_and_type_index) {
			super(class_index, name_and_type_index);
		}

		@Override
		public int tag() {
			return CONSTANT_Fieldref;
		}

		@Override
		public String toString() {
			return "FieldrefInfo:" + class_index + ":" + name_and_type_index;
		}
	}

	static FieldrefInfo parseFieldrefInfo(DataInput in) throws IOException {
		return parseRefInfo(in, FieldrefInfo::new);
	}

	public static class MethodrefInfo extends AbstractRefInfo {
		MethodrefInfo(int class_index, int name_and_type_index) {
			super(class_index, name_and_type_index);
		}

		@Override
		public int tag() {
			return CONSTANT_Methodref;
		}

		@Override
		public String toString() {
			return "MethodrefInfo:" + class_index + ":" + name_and_type_index;
		}
	}

	static MethodrefInfo parseMethodrefInfo(DataInput in) throws IOException {
		return parseRefInfo(in, MethodrefInfo::new);
	}

	public static class InterfaceMethodrefInfo extends AbstractRefInfo {
		InterfaceMethodrefInfo(int class_index, int name_and_type_index) {
			super(class_index, name_and_type_index);
		}

		@Override
		public int tag() {
			return CONSTANT_InterfaceMethodref;
		}

		@Override
		public String toString() {
			return "InterfaceMethodrefInfo:" + class_index + ":" + name_and_type_index;
		}
	}

	static InterfaceMethodrefInfo parseInterfaceMethodrefInfo(DataInput in) throws IOException {
		return parseRefInfo(in, InterfaceMethodrefInfo::new);
	}

	public static class NameAndTypeInfo implements Info {
		public final int	name_index;
		public final int	descriptor_index;

		NameAndTypeInfo(int name_index, int descriptor_index) {
			this.name_index = name_index;
			this.descriptor_index = descriptor_index;
		}

		@Override
		public int tag() {
			return CONSTANT_NameAndType;
		}

		@Override
		public String toString() {
			return "NameAndTypeInfo:" + name_index + ":" + descriptor_index;
		}
	}

	static NameAndTypeInfo parseNameAndTypeInfo(DataInput in) throws IOException {
		int name_index = in.readUnsignedShort();
		int descriptor_index = in.readUnsignedShort();
		return new NameAndTypeInfo(name_index, descriptor_index);
	}

	public static class MethodHandleInfo implements Info {
		public static final int	REF_getField			= 1;
		public static final int	REF_getStatic			= 2;
		public static final int	REF_putField			= 3;
		public static final int	REF_putStatic			= 4;
		public static final int	REF_invokeVirtual		= 5;
		public static final int	REF_invokeStatic		= 6;
		public static final int	REF_invokeSpecial		= 7;
		public static final int	REF_newInvokeSpecial	= 8;
		public static final int	REF_invokeInterface		= 9;
		public final int		reference_kind;
		public final int		reference_index;

		MethodHandleInfo(int reference_kind, int reference_index) {
			this.reference_kind = reference_kind;
			this.reference_index = reference_index;
		}

		@Override
		public int tag() {
			return CONSTANT_MethodHandle;
		}

		@Override
		public String toString() {
			return "MethodHandleInfo:" + reference_kind + ":" + reference_index;
		}
	}

	static MethodHandleInfo parseMethodHandleInfo(DataInput in) throws IOException {
		int reference_kind = in.readUnsignedByte();
		int reference_index = in.readUnsignedShort();
		return new MethodHandleInfo(reference_kind, reference_index);
	}

	public static class MethodTypeInfo implements Info {
		public final int descriptor_index;

		MethodTypeInfo(int descriptor_index) {
			this.descriptor_index = descriptor_index;
		}

		@Override
		public int tag() {
			return CONSTANT_MethodType;
		}

		@Override
		public String toString() {
			return "MethodTypeInfo:" + descriptor_index;
		}
	}

	static MethodTypeInfo parseMethodTypeInfo(DataInput in) throws IOException {
		int descriptor_index = in.readUnsignedShort();
		return new MethodTypeInfo(descriptor_index);
	}

	public abstract static class AbstractDynamicInfo implements Info {
		public final int	bootstrap_method_attr_index;
		public final int	name_and_type_index;

		AbstractDynamicInfo(int bootstrap_method_attr_index, int name_and_type_index) {
			this.bootstrap_method_attr_index = bootstrap_method_attr_index;
			this.name_and_type_index = name_and_type_index;
		}
	}

	static <D extends AbstractDynamicInfo> D parseAbstractDynamicInfo(DataInput in, IntBiFunction<D> constructor)
		throws IOException {
		int bootstrap_method_attr_index = in.readUnsignedShort();
		int name_and_type_index = in.readUnsignedShort();
		return constructor.apply(bootstrap_method_attr_index, name_and_type_index);
	}

	public static class DynamicInfo extends AbstractDynamicInfo {
		DynamicInfo(int bootstrap_method_attr_index, int name_and_type_index) {
			super(bootstrap_method_attr_index, name_and_type_index);
		}

		@Override
		public int tag() {
			return CONSTANT_Dynamic;
		}

		@Override
		public String toString() {
			return "DynamicInfo:" + bootstrap_method_attr_index + ":" + name_and_type_index;
		}
	}

	static DynamicInfo parseDynamicInfo(DataInput in) throws IOException {
		return parseAbstractDynamicInfo(in, DynamicInfo::new);
	}

	public static class InvokeDynamicInfo extends AbstractDynamicInfo {
		InvokeDynamicInfo(int bootstrap_method_attr_index, int name_and_type_index) {
			super(bootstrap_method_attr_index, name_and_type_index);
		}

		@Override
		public int tag() {
			return CONSTANT_InvokeDynamic;
		}

		@Override
		public String toString() {
			return "InvokeDynamicInfo:" + bootstrap_method_attr_index + ":" + name_and_type_index;
		}
	}

	static InvokeDynamicInfo parseInvokeDynamicInfo(DataInput in) throws IOException {
		return parseAbstractDynamicInfo(in, InvokeDynamicInfo::new);
	}

	public static class ModuleInfo implements Info {
		public final int name_index;

		ModuleInfo(int name_index) {
			this.name_index = name_index;
		}

		@Override
		public int tag() {
			return CONSTANT_Module;
		}

		@Override
		public String toString() {
			return "ModuleInfo:" + name_index;
		}
	}

	static ModuleInfo parseModuleInfo(DataInput in) throws IOException {
		int name_index = in.readUnsignedShort();
		return new ModuleInfo(name_index);
	}

	public static class PackageInfo implements Info {
		public final int name_index;

		PackageInfo(int name_index) {
			this.name_index = name_index;
		}

		@Override
		public int tag() {
			return CONSTANT_Package;
		}

		@Override
		public String toString() {
			return "PackageInfo:" + name_index;
		}
	}

	static PackageInfo parsePackageInfo(DataInput in) throws IOException {
		int name_index = in.readUnsignedShort();
		return new PackageInfo(name_index);
	}
}
