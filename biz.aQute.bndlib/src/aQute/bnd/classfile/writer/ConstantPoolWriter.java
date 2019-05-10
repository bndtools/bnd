package aQute.bnd.classfile.writer;

import static aQute.bnd.classfile.ConstantPool.CONSTANT_Class;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Double;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Dynamic;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Fieldref;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Float;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Integer;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_InterfaceMethodref;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_InvokeDynamic;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Long;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_MethodHandle;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_MethodType;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Methodref;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Module;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_NameAndType;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Package;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_String;
import static aQute.bnd.classfile.ConstantPool.CONSTANT_Utf8;
import static java.util.Objects.requireNonNull;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import aQute.bnd.classfile.ConstantPool.AbstractDynamicInfo;
import aQute.bnd.classfile.ConstantPool.AbstractRefInfo;
import aQute.bnd.classfile.ConstantPool.ClassInfo;
import aQute.bnd.classfile.ConstantPool.Info;
import aQute.bnd.classfile.ConstantPool.MethodHandleInfo;
import aQute.bnd.classfile.ConstantPool.MethodTypeInfo;
import aQute.bnd.classfile.ConstantPool.ModuleInfo;
import aQute.bnd.classfile.ConstantPool.NameAndTypeInfo;
import aQute.bnd.classfile.ConstantPool.PackageInfo;
import aQute.bnd.classfile.ConstantPool.StringInfo;

public class ConstantPoolWriter implements DataOutputWriter {

	private final List<Object> pool;

	public ConstantPoolWriter() {
		pool = new ArrayList<>();
		pool.add(null); // index 0
	}

	@Override
	public String toString() {
		return pool.toString();
	}

	// CONSTANT_Integer
	public int integerInfo(int constant) {
		return entry(Integer.class, other -> Integer.compare(constant, other.intValue()) == 0,
			() -> Integer.valueOf(constant));
	}

	// CONSTANT_Long
	public int longInfo(long constant) {
		return entry(Long.class, other -> Long.compare(constant, other.longValue()) == 0, () -> Long.valueOf(constant));
	}

	// CONSTANT_Float
	public int floatInfo(float constant) {
		return entry(Float.class, other -> Float.compare(constant, other.floatValue()) == 0,
			() -> Float.valueOf(constant));
	}

	// CONSTANT_Double
	public int doubleInfo(double constant) {
		return entry(Double.class, other -> Double.compare(constant, other.doubleValue()) == 0,
			() -> Double.valueOf(constant));
	}

	// CONSTANT_Utf8
	public int utf8Info(String utf8) {
		requireNonNull(utf8);
		return entry(String.class, utf8::equals, () -> utf8);
	}

	// CONSTANT_String
	public int stringInfo(String string) {
		requireNonNull(string);
		return entry(StringInfo.class, other -> string.equals(pool.get(other.string_index)),
			() -> new StringInfo(utf8Info(string)));
	}

	// CONSTANT_Module
	public int moduleInfo(String module_name) {
		requireNonNull(module_name);
		return entry(ModuleInfo.class, other -> module_name.equals(pool.get(other.name_index)),
			() -> new ModuleInfo(utf8Info(module_name)));
	}

	// CONSTANT_Package
	public int packageInfo(String package_name) {
		requireNonNull(package_name);
		return entry(PackageInfo.class, other -> package_name.equals(pool.get(other.name_index)),
			() -> new PackageInfo(utf8Info(package_name)));
	}

	// CONSTANT_Class
	public int classInfo(String class_name) {
		requireNonNull(class_name);
		return entry(ClassInfo.class, other -> class_name.equals(pool.get(other.class_index)),
			() -> new ClassInfo(utf8Info(class_name)));
	}

	// CONSTANT_Fieldref
	// CONSTANT_Methodref
	// CONSTANT_InterfaceMethodref
	// CONSTANT_NameAndType
	// CONSTANT_MethodHandle
	// CONSTANT_MethodType
	// CONSTANT_Dynamic
	// CONSTANT_InvokeDynamic

	<I> int entry(Class<I> infoType, Predicate<I> match, Supplier<I> supplier) {
		for (int index = 1, len = pool.size(); index < len; index++) {
			Object entry = pool.get(index);
			if (infoType.isInstance(entry) && match.test(infoType.cast(entry))) {
				return index;
			}
		}
		I entry = supplier.get();
		int index = pool.size(); // supplier may have added to pool
		pool.add(entry);
		if ((infoType == Long.class) || (infoType == Double.class)) {
			// For some insane optimization reason, the Long(5) and
			// Double(6) entries take two slots in the constant pool.
			// See 4.4.5
			pool.add(null);
		}
		return index;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		int constant_pool_count = pool.size();
		out.writeShort(constant_pool_count);
		for (int index = 1; index < constant_pool_count; index++) {
			Object entry = pool.get(index);
			if (entry instanceof Info) {
				int tag = ((Info) entry).tag();
				out.writeByte(tag);
				switch (tag) {
					case CONSTANT_Class : {
						ClassInfo info = (ClassInfo) entry;
						out.writeShort(info.class_index);
						break;
					}
					case CONSTANT_String : {
						StringInfo info = (StringInfo) entry;
						out.writeShort(info.string_index);
						break;
					}
					case CONSTANT_Fieldref :
					case CONSTANT_Methodref :
					case CONSTANT_InterfaceMethodref : {
						AbstractRefInfo info = (AbstractRefInfo) entry;
						out.writeShort(info.class_index);
						out.writeShort(info.name_and_type_index);
						break;
					}
					case CONSTANT_NameAndType : {
						NameAndTypeInfo info = (NameAndTypeInfo) entry;
						out.writeShort(info.name_index);
						out.writeShort(info.descriptor_index);
						break;
					}
					case CONSTANT_MethodHandle : {
						MethodHandleInfo info = (MethodHandleInfo) entry;
						out.writeByte(info.reference_kind);
						out.writeShort(info.reference_index);
						break;
					}
					case CONSTANT_MethodType : {
						MethodTypeInfo info = (MethodTypeInfo) entry;
						out.writeShort(info.descriptor_index);
						break;
					}
					case CONSTANT_Dynamic :
					case CONSTANT_InvokeDynamic : {
						AbstractDynamicInfo info = (AbstractDynamicInfo) entry;
						out.writeShort(info.bootstrap_method_attr_index);
						out.writeShort(info.name_and_type_index);
						break;
					}
					case CONSTANT_Module : {
						ModuleInfo info = (ModuleInfo) entry;
						out.writeShort(info.name_index);
						break;
					}
					case CONSTANT_Package : {
						PackageInfo info = (PackageInfo) entry;
						out.writeShort(info.name_index);
						break;
					}
					default : {
						throw new IOException("Unrecognized constant pool entry " + entry + " at index " + index);
					}
				}
			} else if (entry instanceof String) {
				out.writeByte(CONSTANT_Utf8);
				out.writeUTF((String) entry);
			} else if (entry instanceof Integer) {
				out.writeByte(CONSTANT_Integer);
				out.writeInt(((Integer) entry).intValue());
			} else if (entry instanceof Long) {
				out.writeByte(CONSTANT_Long);
				out.writeLong(((Long) entry).longValue());
				// For some insane optimization reason, the Long(5) and
				// Double(6) entries take two slots in the constant pool.
				// See 4.4.5
				index++;
			} else if (entry instanceof Float) {
				out.writeByte(CONSTANT_Float);
				out.writeFloat(((Float) entry).floatValue());
			} else if (entry instanceof Double) {
				out.writeByte(CONSTANT_Double);
				out.writeDouble(((Double) entry).doubleValue());
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
