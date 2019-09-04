package aQute.bnd.classfile;

import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.osgi.annotation.versioning.ProviderType;

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

	public ConstantPool(Object[] pool) {
		this.pool = pool;
	}

	public int size() {
		return pool.length;
	}

	@SuppressWarnings("unchecked")
	public <T> T entry(int index) {
		return (T) pool[index];
	}

	public int tag(int index) {
		Object entry = entry(index);
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
		return entry(utf8_index);
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
		return Arrays.toString(pool);
	}

	public static ConstantPool read(DataInput in) throws IOException {
		int constant_pool_count = in.readUnsignedShort();
		Object[] pool = new Object[constant_pool_count];
		for (int index = 1; index < constant_pool_count; index++) {
			int tag = in.readUnsignedByte();
			switch (tag) {
				case CONSTANT_Utf8 : {
					pool[index] = readUtf8Info(in);
					break;
				}
				case CONSTANT_Integer : {
					pool[index] = readIntegerInfo(in);
					break;
				}
				case CONSTANT_Float : {
					pool[index] = readFloatInfo(in);
					break;
				}
				case CONSTANT_Long : {
					pool[index] = readLongInfo(in);
					// For some insane optimization reason, the Long(5) and
					// Double(6) entries take two slots in the constant pool.
					// See 4.4.5
					index++;
					break;
				}
				case CONSTANT_Double : {
					pool[index] = readDoubleInfo(in);
					// For some insane optimization reason, the Long(5) and
					// Double(6) entries take two slots in the constant pool.
					// See 4.4.5
					index++;
					break;
				}
				case CONSTANT_Class : {
					pool[index] = ClassInfo.read(in);
					break;
				}
				case CONSTANT_String : {
					pool[index] = StringInfo.read(in);
					break;
				}
				case CONSTANT_Fieldref : {
					pool[index] = FieldrefInfo.read(in);
					break;
				}
				case CONSTANT_Methodref : {
					pool[index] = MethodrefInfo.read(in);
					break;
				}
				case CONSTANT_InterfaceMethodref : {
					pool[index] = InterfaceMethodrefInfo.read(in);
					break;
				}
				case CONSTANT_NameAndType : {
					pool[index] = NameAndTypeInfo.read(in);
					break;
				}
				case CONSTANT_MethodHandle : {
					pool[index] = MethodHandleInfo.read(in);
					break;
				}
				case CONSTANT_MethodType : {
					pool[index] = MethodTypeInfo.read(in);
					break;
				}
				case CONSTANT_Dynamic : {
					pool[index] = DynamicInfo.read(in);
					break;
				}
				case CONSTANT_InvokeDynamic : {
					pool[index] = InvokeDynamicInfo.read(in);
					break;
				}
				case CONSTANT_Module : {
					pool[index] = ModuleInfo.read(in);
					break;
				}
				case CONSTANT_Package : {
					pool[index] = PackageInfo.read(in);
					break;
				}
				default : {
					throw new IOException("Unrecognized constant pool tag value " + tag + " at index " + index);
				}
			}
		}

		ConstantPool constant_pool = new ConstantPool(pool);
		return constant_pool;
	}

	static String readUtf8Info(DataInput in) throws IOException {
		String constant = in.readUTF();
		return constant.intern();
	}

	static void writeUtf8Info(DataOutput out, String constant) throws IOException {
		out.writeByte(CONSTANT_Utf8);
		out.writeUTF(constant);
	}

	static Integer readIntegerInfo(DataInput in) throws IOException {
		int constant = in.readInt();
		return constant;
	}

	static void writeIntegerInfo(DataOutput out, Integer constant) throws IOException {
		out.writeByte(CONSTANT_Integer);
		out.writeInt(constant);
	}

	static Float readFloatInfo(DataInput in) throws IOException {
		float constant = in.readFloat();
		return constant;
	}

	static void writeFloatInfo(DataOutput out, Float constant) throws IOException {
		out.writeByte(CONSTANT_Float);
		out.writeFloat(constant);
	}

	static Long readLongInfo(DataInput in) throws IOException {
		long constant = in.readLong();
		return constant;
	}

	static void writeLongInfo(DataOutput out, Long constant) throws IOException {
		out.writeByte(CONSTANT_Long);
		out.writeLong(constant);
	}

	static Double readDoubleInfo(DataInput in) throws IOException {
		double constant = in.readDouble();
		return constant;
	}

	static void writeDoubleInfo(DataOutput out, Double constant) throws IOException {
		out.writeByte(CONSTANT_Double);
		out.writeDouble(constant);
	}

	@ProviderType
	public interface Info {
		int tag();

		void write(DataOutput out) throws IOException;
	}

	public static class ClassInfo implements Info {
		public final int class_index;

		public ClassInfo(int class_index) {
			this.class_index = class_index;
		}

		static ClassInfo read(DataInput in) throws IOException {
			int name_index = in.readUnsignedShort();
			return new ClassInfo(name_index);
		}

		@Override
		public int tag() {
			return CONSTANT_Class;
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeShort(class_index);
		}

		@Override
		public String toString() {
			return "ClassInfo:" + class_index;
		}
	}

	public static class StringInfo implements Info {
		public final int string_index;

		public StringInfo(int string_index) {
			this.string_index = string_index;
		}

		static StringInfo read(DataInput in) throws IOException {
			int string_index = in.readUnsignedShort();
			return new StringInfo(string_index);
		}

		@Override
		public int tag() {
			return CONSTANT_String;
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeShort(string_index);
		}

		@Override
		public String toString() {
			return "StringInfo:" + string_index;
		}
	}

	public abstract static class AbstractRefInfo implements Info {
		public final int	class_index;
		public final int	name_and_type_index;

		protected AbstractRefInfo(int class_index, int name_and_type_index) {
			this.class_index = class_index;
			this.name_and_type_index = name_and_type_index;
		}

		@FunctionalInterface
		public interface Constructor<R extends AbstractRefInfo> {
			R init(int class_index, int name_and_type_index);
		}

		static <R extends AbstractRefInfo> R read(DataInput in, Constructor<R> constructor) throws IOException {
			int class_index = in.readUnsignedShort();
			int name_and_type_index = in.readUnsignedShort();
			return constructor.init(class_index, name_and_type_index);
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeShort(class_index);
			out.writeShort(name_and_type_index);
		}
	}

	public static class FieldrefInfo extends AbstractRefInfo {
		public FieldrefInfo(int class_index, int name_and_type_index) {
			super(class_index, name_and_type_index);
		}

		static FieldrefInfo read(DataInput in) throws IOException {
			return AbstractRefInfo.read(in, FieldrefInfo::new);
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

	public static class MethodrefInfo extends AbstractRefInfo {
		public MethodrefInfo(int class_index, int name_and_type_index) {
			super(class_index, name_and_type_index);
		}

		static MethodrefInfo read(DataInput in) throws IOException {
			return AbstractRefInfo.read(in, MethodrefInfo::new);
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

	public static class InterfaceMethodrefInfo extends AbstractRefInfo {
		public InterfaceMethodrefInfo(int class_index, int name_and_type_index) {
			super(class_index, name_and_type_index);
		}

		static InterfaceMethodrefInfo read(DataInput in) throws IOException {
			return AbstractRefInfo.read(in, InterfaceMethodrefInfo::new);
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

	public static class NameAndTypeInfo implements Info {
		public final int	name_index;
		public final int	descriptor_index;

		public NameAndTypeInfo(int name_index, int descriptor_index) {
			this.name_index = name_index;
			this.descriptor_index = descriptor_index;
		}

		static NameAndTypeInfo read(DataInput in) throws IOException {
			int name_index = in.readUnsignedShort();
			int descriptor_index = in.readUnsignedShort();
			return new NameAndTypeInfo(name_index, descriptor_index);
		}

		@Override
		public int tag() {
			return CONSTANT_NameAndType;
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeShort(name_index);
			out.writeShort(descriptor_index);
		}

		@Override
		public String toString() {
			return "NameAndTypeInfo:" + name_index + ":" + descriptor_index;
		}
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

		public MethodHandleInfo(int reference_kind, int reference_index) {
			this.reference_kind = reference_kind;
			this.reference_index = reference_index;
		}

		static MethodHandleInfo read(DataInput in) throws IOException {
			int reference_kind = in.readUnsignedByte();
			int reference_index = in.readUnsignedShort();
			return new MethodHandleInfo(reference_kind, reference_index);
		}

		@Override
		public int tag() {
			return CONSTANT_MethodHandle;
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeByte(reference_kind);
			out.writeShort(reference_index);
		}

		@Override
		public String toString() {
			return "MethodHandleInfo:" + reference_kind + ":" + reference_index;
		}
	}

	public static class MethodTypeInfo implements Info {
		public final int descriptor_index;

		public MethodTypeInfo(int descriptor_index) {
			this.descriptor_index = descriptor_index;
		}

		static MethodTypeInfo read(DataInput in) throws IOException {
			int descriptor_index = in.readUnsignedShort();
			return new MethodTypeInfo(descriptor_index);
		}

		@Override
		public int tag() {
			return CONSTANT_MethodType;
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeShort(descriptor_index);
		}

		@Override
		public String toString() {
			return "MethodTypeInfo:" + descriptor_index;
		}
	}

	public abstract static class AbstractDynamicInfo implements Info {
		public final int	bootstrap_method_attr_index;
		public final int	name_and_type_index;

		protected AbstractDynamicInfo(int bootstrap_method_attr_index, int name_and_type_index) {
			this.bootstrap_method_attr_index = bootstrap_method_attr_index;
			this.name_and_type_index = name_and_type_index;
		}

		@FunctionalInterface
		public interface Constructor<D extends AbstractDynamicInfo> {
			D init(int bootstrap_method_attr_index, int name_and_type_index);
		}

		static <D extends AbstractDynamicInfo> D read(DataInput in, Constructor<D> constructor) throws IOException {
			int bootstrap_method_attr_index = in.readUnsignedShort();
			int name_and_type_index = in.readUnsignedShort();
			return constructor.init(bootstrap_method_attr_index, name_and_type_index);
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeShort(bootstrap_method_attr_index);
			out.writeShort(name_and_type_index);
		}
	}

	public static class DynamicInfo extends AbstractDynamicInfo {
		public DynamicInfo(int bootstrap_method_attr_index, int name_and_type_index) {
			super(bootstrap_method_attr_index, name_and_type_index);
		}

		static DynamicInfo read(DataInput in) throws IOException {
			return AbstractDynamicInfo.read(in, DynamicInfo::new);
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

	public static class InvokeDynamicInfo extends AbstractDynamicInfo {
		public InvokeDynamicInfo(int bootstrap_method_attr_index, int name_and_type_index) {
			super(bootstrap_method_attr_index, name_and_type_index);
		}

		static InvokeDynamicInfo read(DataInput in) throws IOException {
			return AbstractDynamicInfo.read(in, InvokeDynamicInfo::new);
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

	public static class ModuleInfo implements Info {
		public final int name_index;

		public ModuleInfo(int name_index) {
			this.name_index = name_index;
		}

		static ModuleInfo read(DataInput in) throws IOException {
			int name_index = in.readUnsignedShort();
			return new ModuleInfo(name_index);
		}

		@Override
		public int tag() {
			return CONSTANT_Module;
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeShort(name_index);
		}

		@Override
		public String toString() {
			return "ModuleInfo:" + name_index;
		}
	}

	public static class PackageInfo implements Info {
		public final int name_index;

		public PackageInfo(int name_index) {
			this.name_index = name_index;
		}

		static PackageInfo read(DataInput in) throws IOException {
			int name_index = in.readUnsignedShort();
			return new PackageInfo(name_index);
		}

		@Override
		public int tag() {
			return CONSTANT_Package;
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeByte(tag());
			out.writeShort(name_index);
		}

		@Override
		public String toString() {
			return "PackageInfo:" + name_index;
		}
	}

	protected <I> int index(Class<I> infoType, Predicate<I> match, Supplier<I> supplier) {
		for (int index = 1, len = size(); index < len; index++) {
			Object entry = entry(index);
			if (infoType.isInstance(entry) && match.test(infoType.cast(entry))) {
				return index;
			}
		}
		return add(infoType, supplier);
	}

	protected <I> int add(Class<I> infoType, Supplier<I> supplier) {
		throw new UnsupportedOperationException(
			"Constant Pool is not writeable; trying to add entry of type " + infoType);
	}

	// CONSTANT_Integer
	public int integerInfo(int constant) {
		return index(Integer.class, other -> equalsInteger(constant, other), () -> constant);
	}

	public int integerInfo(Integer constant) {
		int const_value = constant.intValue();
		return index(Integer.class, other -> equalsInteger(const_value, other), () -> constant);
	}

	public int integerInfo(Byte constant) {
		return integerInfo(constant.intValue());
	}

	public int integerInfo(Character constant) {
		return integerInfo(constant.charValue());
	}

	public int integerInfo(Short constant) {
		return integerInfo(constant.intValue());
	}

	public int integerInfo(Boolean constant) {
		return integerInfo(constant.booleanValue() ? 1 : 0);
	}

	private static boolean equalsInteger(int a, int b) {
		return a == b;
	}

	// CONSTANT_Long
	public int longInfo(Long constant) {
		long const_value = constant.longValue();
		return index(Long.class, other -> equalsLong(const_value, other), () -> constant);
	}

	public int longInfo(long constant) {
		return index(Long.class, other -> equalsLong(constant, other), () -> constant);
	}

	private static boolean equalsLong(long a, long b) {
		return a == b;
	}

	// CONSTANT_Float
	public int floatInfo(Float constant) {
		float const_value = constant.floatValue();
		return index(Float.class, other -> equalsFloat(const_value, other), () -> constant);
	}

	public int floatInfo(float constant) {
		return index(Float.class, other -> equalsFloat(constant, other), () -> constant);
	}

	private static boolean equalsFloat(float a, float b) {
		return Float.compare(a, b) == 0;
	}

	// CONSTANT_Double
	public int doubleInfo(Double constant) {
		double const_value = constant.doubleValue();
		return index(Double.class, other -> equalsDouble(const_value, other), () -> constant);
	}

	public int doubleInfo(double constant) {
		return index(Double.class, other -> equalsDouble(constant, other), () -> constant);
	}

	private static boolean equalsDouble(double a, double b) {
		return Double.compare(a, b) == 0;
	}

	// CONSTANT_Utf8
	public int utf8Info(String utf8) {
		requireNonNull(utf8);
		return index(String.class, utf8::equals, () -> utf8.intern());
	}

	// CONSTANT_String
	public int stringInfo(String string) {
		requireNonNull(string);
		return index(StringInfo.class, other -> equalsStringInfo(string, other),
			() -> new StringInfo(utf8Info(string)));
	}

	private boolean equalsStringInfo(String string, StringInfo stringInfo) {
		return string.equals(entry(stringInfo.string_index));
	}

	// CONSTANT_Module
	public int moduleInfo(String module_name) {
		requireNonNull(module_name);
		return index(ModuleInfo.class, other -> equalsModuleInfo(module_name, other),
			() -> new ModuleInfo(utf8Info(module_name)));
	}

	private boolean equalsModuleInfo(String module_name, ModuleInfo moduleInfo) {
		return module_name.equals(entry(moduleInfo.name_index));
	}

	// CONSTANT_Package
	public int packageInfo(String package_name) {
		requireNonNull(package_name);
		return index(PackageInfo.class, other -> equalsPackageInfo(package_name, other),
			() -> new PackageInfo(utf8Info(package_name)));
	}

	private boolean equalsPackageInfo(String package_name, PackageInfo packageInfo) {
		return package_name.equals(entry(packageInfo.name_index));
	}

	// CONSTANT_Class
	public int classInfo(String class_name) {
		requireNonNull(class_name);
		return index(ClassInfo.class, other -> equalsClassInfo(class_name, other),
			() -> new ClassInfo(utf8Info(class_name)));
	}

	private boolean equalsClassInfo(String class_name, ClassInfo classInfo) {
		return class_name.equals(entry(classInfo.class_index));
	}

	@FunctionalInterface
	public interface RefInfoFunction {
		int index(String class_name, String name, String descriptor);
	}

	// CONSTANT_Fieldref
	public int fieldrefInfo(String class_name, String name, String descriptor) {
		requireNonNull(class_name);
		requireNonNull(name);
		requireNonNull(descriptor);
		return index(FieldrefInfo.class, other -> equalsAbstractRefInfo(class_name, name, descriptor, other),
			() -> new FieldrefInfo(classInfo(class_name), nameAndTypeInfo(name, descriptor)));
	}

	private boolean equalsAbstractRefInfo(String class_name, String name, String descriptor, AbstractRefInfo refInfo) {
		return equalsClassInfo(class_name, entry(refInfo.class_index))
			&& equalsNameAndTypeInfo(name, descriptor, entry(refInfo.name_and_type_index));
	}

	// CONSTANT_Methodref
	public int methodrefInfo(String class_name, String name, String descriptor) {
		requireNonNull(class_name);
		requireNonNull(name);
		requireNonNull(descriptor);
		return index(MethodrefInfo.class, other -> equalsAbstractRefInfo(class_name, name, descriptor, other),
			() -> new MethodrefInfo(classInfo(class_name), nameAndTypeInfo(name, descriptor)));
	}

	// CONSTANT_InterfaceMethodref
	public int interfaceMethodrefInfo(String class_name, String name, String descriptor) {
		requireNonNull(class_name);
		requireNonNull(name);
		requireNonNull(descriptor);
		return index(InterfaceMethodrefInfo.class, other -> equalsAbstractRefInfo(class_name, name, descriptor, other),
			() -> new InterfaceMethodrefInfo(classInfo(class_name), nameAndTypeInfo(name, descriptor)));
	}

	// CONSTANT_NameAndType
	public int nameAndTypeInfo(String name, String descriptor) {
		requireNonNull(name);
		requireNonNull(descriptor);
		return index(NameAndTypeInfo.class, other -> equalsNameAndTypeInfo(name, descriptor, other),
			() -> new NameAndTypeInfo(utf8Info(name), utf8Info(descriptor)));
	}

	private boolean equalsNameAndTypeInfo(String name, String descriptor, NameAndTypeInfo nameAndTypeInfo) {
		return name.equals(entry(nameAndTypeInfo.name_index))
			&& descriptor.equals(entry(nameAndTypeInfo.descriptor_index));
	}

	// CONSTANT_MethodHandle
	public int methodHandleInfo(int reference_kind, String class_name, String name, String descriptor,
		RefInfoFunction refInfoFunction) {
		requireNonNull(class_name);
		requireNonNull(name);
		requireNonNull(descriptor);
		requireNonNull(refInfoFunction);
		return index(MethodHandleInfo.class,
			other -> equalsMethodHandleInfo(reference_kind, class_name, name, descriptor, other),
			() -> new MethodHandleInfo(reference_kind, refInfoFunction.index(class_name, name, descriptor)));
	}

	private boolean equalsMethodHandleInfo(int reference_kind, String class_name, String name, String descriptor,
		MethodHandleInfo methodHandleInfo) {
		return equalsInteger(reference_kind, methodHandleInfo.reference_index)
			&& equalsAbstractRefInfo(class_name, name, descriptor, entry(methodHandleInfo.reference_index));
	}

	// CONSTANT_MethodType
	public int methodTypeInfo(String descriptor) {
		requireNonNull(descriptor);
		return index(MethodTypeInfo.class, other -> equalsMethodTypeInfo(descriptor, other),
			() -> new MethodTypeInfo(utf8Info(descriptor)));
	}

	private boolean equalsMethodTypeInfo(String descriptor, MethodTypeInfo methodTypeInfo) {
		return descriptor.equals(entry(methodTypeInfo.descriptor_index));
	}

	// CONSTANT_Dynamic
	public int dynamicInfo(int bootstrap_method_attr_index, String name, String descriptor) {
		requireNonNull(name);
		requireNonNull(descriptor);
		return index(DynamicInfo.class,
			other -> equalsAbstractDynamicInfo(bootstrap_method_attr_index, name, descriptor, other),
			() -> new DynamicInfo(bootstrap_method_attr_index, nameAndTypeInfo(name, descriptor)));
	}

	private boolean equalsAbstractDynamicInfo(int bootstrap_method_attr_index, String name, String descriptor,
		AbstractDynamicInfo abstractDynamicInfo) {
		return equalsInteger(bootstrap_method_attr_index, abstractDynamicInfo.bootstrap_method_attr_index)
			&& equalsNameAndTypeInfo(name, descriptor, entry(abstractDynamicInfo.name_and_type_index));
	}

	// CONSTANT_InvokeDynamic
	public int invokeDynamicInfo(int bootstrap_method_attr_index, String name, String descriptor) {
		requireNonNull(name);
		requireNonNull(descriptor);
		return index(InvokeDynamicInfo.class,
			other -> equalsAbstractDynamicInfo(bootstrap_method_attr_index, name, descriptor, other),
			() -> new InvokeDynamicInfo(bootstrap_method_attr_index, nameAndTypeInfo(name, descriptor)));
	}

	public void write(DataOutput out) throws IOException {
		int constant_pool_count = size();
		out.writeShort(constant_pool_count);
		for (int index = 1; index < constant_pool_count; index++) {
			Object entry = entry(index);
			if (entry instanceof Info) {
				((Info) entry).write(out);
			} else if (entry instanceof String) {
				writeUtf8Info(out, (String) entry);
			} else if (entry instanceof Integer) {
				writeIntegerInfo(out, (Integer) entry);
			} else if (entry instanceof Long) {
				writeLongInfo(out, (Long) entry);
				// For some insane optimization reason, the Long(5) and
				// Double(6) entries take two slots in the constant pool.
				// See 4.4.5
				index++;
			} else if (entry instanceof Float) {
				writeFloatInfo(out, (Float) entry);
			} else if (entry instanceof Double) {
				writeDoubleInfo(out, (Double) entry);
				// For some insane optimization reason, the Long(5) and
				// Double(6) entries take two slots in the constant pool.
				// See 4.4.5
				index++;
			} else {
				throw new IOException("Unrecognized constant pool entry " + entry + " at index " + index);
			}
		}
	}
}
