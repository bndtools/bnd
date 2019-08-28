package aQute.bnd.classfile.builder;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.FieldInfo;
import aQute.bnd.classfile.MethodInfo;

public class ClassFileBuilder {
	public static final String[]		EMPTY_STRING_ARRAY		= new String[0];
	public static final FieldInfo[]		EMPTY_FIELD_ARRAY		= new FieldInfo[0];
	public static final MethodInfo[]	EMPTY_METHOD_ARRAY		= new MethodInfo[0];
	public static final Attribute[]		EMPTY_ATTRIBUTE_ARRAY	= new Attribute[0];
	private int							minor_version;
	private int							major_version;
	private MutableConstantPool		constant_pool;
	private int							access;
	private String						this_class;
	private String						super_class;
	private final List<String>			interfaces				= new ArrayList<>();
	private final List<FieldInfo>		fields					= new ArrayList<>();
	private final List<MethodInfo>		methods					= new ArrayList<>();
	private final List<Attribute>		attributes				= new ArrayList<>();

	public ClassFileBuilder(int access_flags, int major_version, int minor_version, String this_class,
		String super_class, Set<String> interfaces) {
		this.constant_pool = new MutableConstantPool();
		this.access = access_flags;
		this.major_version = major_version;
		this.minor_version = minor_version;
		this.this_class = requireNonNull(this_class);
		this.super_class = super_class;
		interfaces.forEach(Objects::requireNonNull);
		this.interfaces.addAll(interfaces);
	}

	public ClassFileBuilder(int access_flags, int major_version, int minor_version, String this_class,
		String super_class, String... interfaces) {
		this(access_flags, major_version, minor_version, this_class, super_class,
			new HashSet<>(Arrays.asList(interfaces)));
	}

	public ClassFileBuilder(int access_flags, int major_version, int minor_version, String this_class,
		String super_class) {
		this(access_flags, major_version, minor_version, this_class, super_class, Collections.emptySet());
	}

	public ClassFileBuilder(ClassFile classFile) {
		minor_version = classFile.minor_version;
		major_version = classFile.major_version;

		constant_pool = new MutableConstantPool(classFile.constant_pool);

		access = classFile.access;

		this_class = classFile.this_class;

		super_class = classFile.super_class;

		Collections.addAll(interfaces, classFile.interfaces);
		Collections.addAll(fields, classFile.fields);
		Collections.addAll(methods, classFile.methods);
		Collections.addAll(attributes, classFile.attributes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Modifier.toString(access & Modifier.classModifiers()));
		if (sb.length() > 0) {
			sb.append(' ');
		}
		if ((access & Modifier.INTERFACE) != 0) {
			if ((access & ClassFile.ACC_ANNOTATION) != 0) {
				sb.append('@');
			}
			sb.append("interface ");
		} else if ((access & ClassFile.ACC_ENUM) != 0) {
			sb.append("enum ");
		} else if ((access & ClassFile.ACC_MODULE) == 0) {
			sb.append("class ");
		}
		return sb.append(this_class)
			.toString();
	}

	public MutableConstantPool constantPool() {
		return constant_pool;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public List<FieldInfo> getFields() {
		return fields;
	}

	public List<MethodInfo> getMethods() {
		return methods;
	}

	public ClassFileBuilder attributes(List<Attribute> attributes) {
		attributes.forEach(Objects::requireNonNull);
		this.attributes.addAll(attributes);
		return this;
	}

	public ClassFileBuilder attributes(Attribute... attributes) {
		return attributes(Arrays.asList(attributes));
	}

	public ClassFileBuilder attribute(Attribute attribute) {
		return attributes(Collections.singletonList(attribute));
	}

	public ClassFileBuilder fields(List<FieldInfo> fields) {
		fields.forEach(Objects::requireNonNull);
		this.fields.addAll(fields);
		return this;
	}

	public ClassFileBuilder fields(FieldInfo... fields) {
		return fields(Arrays.asList(fields));
	}

	public ClassFileBuilder field(FieldInfo field) {
		return fields(Collections.singletonList(field));
	}

	public ClassFileBuilder methods(List<MethodInfo> methods) {
		methods.forEach(Objects::requireNonNull);
		this.methods.addAll(methods);
		return this;
	}

	public ClassFileBuilder methods(MethodInfo... methods) {
		return methods(Arrays.asList(methods));
	}

	public ClassFileBuilder method(MethodInfo method) {
		return methods(Collections.singletonList(method));
	}

	public ClassFile build() {
		ClassFile classFile = new ClassFile(minor_version, major_version, constantPool(), access, this_class,
			super_class, interfaces.toArray(EMPTY_STRING_ARRAY), getFields().toArray(EMPTY_FIELD_ARRAY),
			getMethods().toArray(EMPTY_METHOD_ARRAY), getAttributes().toArray(EMPTY_ATTRIBUTE_ARRAY));
		return classFile;
	}
}
