package aQute.bnd.classfile.writer;

import static java.util.Objects.requireNonNull;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ClassFileWriter implements DataOutputWriter {
	private final int					minor_version;
	private final int					major_version;
	private final ConstantPoolWriter	constant_pool;
	private final int					access_flags;
	private final int					this_class;
	private final int					super_class;
	private final int[]					interfaces;
	private final List<MemberWriter>	fields		= new ArrayList<>();
	private final List<MemberWriter>	methods		= new ArrayList<>();
	private final List<AttributeWriter>	attributes	= new ArrayList<>();

	public ClassFileWriter(ConstantPoolWriter constantPool, int access_flags, int major_version, int minor_version,
		String this_class, String super_class, Set<String> interfaces) {
		this.constant_pool = requireNonNull(constantPool);
		this.access_flags = access_flags;
		this.major_version = major_version;
		this.minor_version = minor_version;
		this.this_class = constantPool.classInfo(this_class);
		this.super_class = (super_class != null) ? constantPool.classInfo(super_class) : 0;
		interfaces.forEach(Objects::requireNonNull);
		this.interfaces = interfaces.stream()
			.mapToInt(constantPool::classInfo)
			.toArray();
	}

	public ClassFileWriter(ConstantPoolWriter constantPool, int access_flags, int major_version, int minor_version,
		String this_class, String super_class, String... interfaces) {
		this(constantPool, access_flags, major_version, minor_version, this_class, super_class,
			new HashSet<>(Arrays.asList(interfaces)));
	}

	public ClassFileWriter(ConstantPoolWriter constantPool, int access_flags, int major_version, int minor_version,
		String this_class, String super_class) {
		this(constantPool, access_flags, major_version, minor_version, this_class, super_class, Collections.emptySet());
	}

	public ClassFileWriter attributes(Set<AttributeWriter> attributes) {
		attributes.forEach(Objects::requireNonNull);
		this.attributes.addAll(attributes);
		return this;
	}

	public ClassFileWriter attributes(AttributeWriter... attributes) {
		return attributes(new HashSet<>(Arrays.asList(attributes)));
	}

	public ClassFileWriter attributes(AttributeWriter attribute) {
		return attributes(Collections.singleton(attribute));
	}

	public ClassFileWriter fields(Set<MemberWriter> fields) {
		fields.forEach(Objects::requireNonNull);
		this.fields.addAll(fields);
		return this;
	}

	public ClassFileWriter fields(MemberWriter... fields) {
		return fields(new HashSet<>(Arrays.asList(fields)));
	}

	public ClassFileWriter fields(MemberWriter field) {
		return fields(Collections.singleton(field));
	}

	public ClassFileWriter methods(Set<MemberWriter> methods) {
		methods.forEach(Objects::requireNonNull);
		this.methods.addAll(methods);
		return this;
	}

	public ClassFileWriter methods(MemberWriter... methods) {
		return methods(new HashSet<>(Arrays.asList(methods)));
	}

	public ClassFileWriter methods(MemberWriter method) {
		return methods(Collections.singleton(method));
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(0xCAFEBABE);
		out.writeShort(minor_version);
		out.writeShort(major_version);

		constant_pool.write(out);

		out.writeShort(access_flags);

		out.writeShort(this_class);
		out.writeShort(super_class);

		out.writeShort(interfaces.length);
		for (int interf : interfaces) {
			out.writeShort(interf);
		}

		out.writeShort(fields.size());
		for (DataOutputWriter field : fields) {
			field.write(out);
		}

		out.writeShort(methods.size());
		for (DataOutputWriter method : methods) {
			method.write(out);
		}

		out.writeShort(attributes.size());
		for (DataOutputWriter attribute : attributes) {
			attribute.write(out);
		}
	}
}
