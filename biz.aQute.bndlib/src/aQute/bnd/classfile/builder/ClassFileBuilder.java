package aQute.bnd.classfile.builder;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.FieldInfo;
import aQute.bnd.classfile.MethodInfo;

public class ClassFileBuilder {
	static final String[]			EMPTY_STRING_ARRAY		= new String[0];
	static final FieldInfo[]		EMPTY_FIELD_ARRAY		= new FieldInfo[0];
	static final MethodInfo[]		EMPTY_METHOD_ARRAY		= new MethodInfo[0];
	static final Attribute[]		EMPTY_ATTRIBUTE_ARRAY	= new Attribute[0];
	private int						minor_version;
	private int						major_version;
	private MutableConstantPool		constant_pool;
	private int						access;
	private String					this_class;
	private String					super_class;
	private final List<String>		interfaces				= new ArrayList<>();
	private final List<FieldInfo>	fields					= new ArrayList<>();
	private final List<MethodInfo>	methods					= new ArrayList<>();
	private final List<Attribute>	attributes				= new ArrayList<>();

	public ClassFileBuilder(int access_flags, int major_version, int minor_version, String this_class,
		String super_class, Collection<String> interfaces) {
		minor_version(minor_version).major_version(major_version)
			.constant_pool(new MutableConstantPool())
			.access(access_flags)
			.this_class(this_class)
			.super_class(super_class)
			.interfaces(interfaces);
	}

	public ClassFileBuilder(int access_flags, int major_version, int minor_version, String this_class,
		String super_class, String... interfaces) {
		this(access_flags, major_version, minor_version, this_class, super_class, Arrays.asList(interfaces));
	}

	public ClassFileBuilder(int access_flags, int major_version, int minor_version, String this_class,
		String super_class) {
		this(access_flags, major_version, minor_version, this_class, super_class, Collections.emptyList());
	}

	public ClassFileBuilder(ClassFile classFile) {
		minor_version(classFile.minor_version).major_version(classFile.major_version)
			.constant_pool(new MutableConstantPool(classFile.constant_pool))
			.access(classFile.access)
			.this_class(classFile.this_class)
			.super_class(classFile.super_class)
			.interfaces(classFile.interfaces)
			.fields(classFile.fields)
			.methods(classFile.methods)
			.attributes(classFile.attributes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Modifier.toString(access() & Modifier.classModifiers()));
		if (sb.length() > 0) {
			sb.append(' ');
		}
		if ((access() & Modifier.INTERFACE) != 0) {
			if ((access() & ClassFile.ACC_ANNOTATION) != 0) {
				sb.append('@');
			}
			sb.append("interface ");
		} else if ((access() & ClassFile.ACC_ENUM) != 0) {
			sb.append("enum ");
		} else if ((access() & ClassFile.ACC_MODULE) == 0) {
			sb.append("class ");
		}
		return sb.append(this_class())
			.toString();
	}

	public int minor_version() {
		return minor_version;
	}

	public ClassFileBuilder minor_version(int minor_version) {
		this.minor_version = minor_version;
		return this;
	}

	public int major_version() {
		return major_version;
	}

	public ClassFileBuilder major_version(int major_version) {
		this.major_version = major_version;
		return this;
	}

	public MutableConstantPool constant_pool() {
		return constant_pool;
	}

	public ClassFileBuilder constant_pool(MutableConstantPool constant_pool) {
		this.constant_pool = requireNonNull(constant_pool);
		return this;
	}

	public int access() {
		return access;
	}

	public ClassFileBuilder access(int access) {
		this.access = access;
		return this;
	}

	public String this_class() {
		return this_class;
	}

	public ClassFileBuilder this_class(String this_class) {
		this.this_class = requireNonNull(this_class);
		return this;
	}

	public String super_class() {
		return super_class;
	}

	public ClassFileBuilder super_class(String super_class) {
		this.super_class = super_class;
		return this;
	}

	public List<String> interfaces() {
		return interfaces;
	}

	public ClassFileBuilder interfaces(String interfc) {
		requireNonNull(interfc);
		if (!interfaces.contains(interfc)) {
			interfaces.add(interfc);
		}
		return this;
	}

	public ClassFileBuilder interfaces(String[] interfcs) {
		for (String i : interfcs) {
			interfaces(i);
		}
		return this;
	}

	public ClassFileBuilder interfaces(Collection<String> interfcs) {
		for (String i : interfcs) {
			interfaces(i);
		}
		return this;
	}

	public ClassFileBuilder interfaces(String interfc, String... interfcs) {
		interfaces(interfc);
		interfaces(interfcs);
		return this;
	}

	public List<FieldInfo> fields() {
		return fields;
	}

	public ClassFileBuilder fields(FieldInfo field) {
		requireNonNull(field);
		for (ListIterator<FieldInfo> iter = fields.listIterator(); iter.hasNext();) {
			FieldInfo member = iter.next();
			if (member.name.equals(field.name) && member.descriptor.equals(field.descriptor)) {
				iter.remove();
				break;
			}
		}
		fields.add(field);
		return this;
	}

	public ClassFileBuilder fields(FieldInfo[] fields) {
		for (FieldInfo f : fields) {
			fields(f);
		}
		return this;
	}

	public ClassFileBuilder fields(Collection<FieldInfo> fields) {
		for (FieldInfo f : fields) {
			fields(f);
		}
		return this;
	}

	public ClassFileBuilder fields(FieldInfo field, FieldInfo... fields) {
		fields(field);
		fields(fields);
		return this;
	}

	public List<MethodInfo> methods() {
		return methods;
	}

	public ClassFileBuilder methods(MethodInfo method) {
		requireNonNull(method);
		for (ListIterator<MethodInfo> iter = methods.listIterator(); iter.hasNext();) {
			MethodInfo member = iter.next();
			if (member.name.equals(method.name) && member.descriptor.equals(method.descriptor)) {
				iter.remove();
				break;
			}
		}
		methods.add(method);
		return this;
	}

	public ClassFileBuilder methods(MethodInfo[] methods) {
		for (MethodInfo m : methods) {
			methods(m);
		}
		return this;
	}

	public ClassFileBuilder methods(Collection<MethodInfo> methods) {
		for (MethodInfo m : methods) {
			methods(m);
		}
		return this;
	}

	public ClassFileBuilder methods(MethodInfo method, MethodInfo... methods) {
		methods(method);
		methods(methods);
		return this;
	}

	public List<Attribute> attributes() {
		return attributes;
	}

	public ClassFileBuilder attributes(Attribute attribute) {
		requireNonNull(attribute);
		attributes.add(attribute);
		return this;
	}

	public ClassFileBuilder attributes(Attribute[] attributes) {
		for (Attribute a : attributes) {
			attributes(a);
		}
		return this;
	}

	public ClassFileBuilder attributes(Collection<Attribute> attributes) {
		for (Attribute a : attributes) {
			attributes(a);
		}
		return this;
	}

	public ClassFileBuilder attributes(Attribute attribute, Attribute... attributes) {
		attributes(attribute);
		attributes(attributes);
		return this;
	}

	public ClassFile build() {
		ClassFile classFile = new ClassFile(minor_version(), major_version(), constant_pool(), access(), this_class(),
			super_class(), interfaces().toArray(EMPTY_STRING_ARRAY), fields().toArray(EMPTY_FIELD_ARRAY),
			methods().toArray(EMPTY_METHOD_ARRAY), attributes().toArray(EMPTY_ATTRIBUTE_ARRAY));
		return classFile;
	}
}
