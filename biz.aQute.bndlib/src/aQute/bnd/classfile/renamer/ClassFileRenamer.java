package aQute.bnd.classfile.renamer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ConstantPool.ClassInfo;
import aQute.bnd.classfile.ConstantPool.InfoTag;
import aQute.bnd.classfile.ConstantPool.MethodTypeInfo;
import aQute.bnd.classfile.ConstantPool.NameAndTypeInfo;
import aQute.bnd.classfile.EnclosingMethodAttribute;
import aQute.bnd.classfile.FieldInfo;
import aQute.bnd.classfile.InnerClassesAttribute;
import aQute.bnd.classfile.InnerClassesAttribute.InnerClass;
import aQute.bnd.classfile.LocalVariableTableAttribute;
import aQute.bnd.classfile.LocalVariableTableAttribute.LocalVariable;
import aQute.bnd.classfile.LocalVariableTypeTableAttribute;
import aQute.bnd.classfile.LocalVariableTypeTableAttribute.LocalVariableType;
import aQute.bnd.classfile.MethodInfo;
import aQute.bnd.classfile.SignatureAttribute;
import aQute.bnd.classfile.builder.ClassFileBuilder;
import aQute.bnd.classfile.builder.MutableConstantPool;
import aQute.bnd.classfile.preview.RecordAttribute;
import aQute.bnd.classfile.preview.RecordAttribute.RecordComponent;
import aQute.lib.strings.Strings;

public class ClassFileRenamer {
	final Function<String, String>	mapper;
	final Map<Integer, String>		renamed	= new HashMap<>();
	final ClassFile					classFile;
	final ClassFileBuilder			builder;
	final SignatureRenamer			sp;
	boolean							changed	= false;

	/**
	 * Rename all types in in a class file with another type. The mapper
	 * function takes a type found in the class file and should return null for
	 * no mapping or a new type name.
	 *
	 * @param cf the class file
	 * @param mapper the mapping functions
	 * @return If there were any renames, it return a new Class File
	 */
	public static Optional<ClassFile> rename(ClassFile cf, Function<String, String> mapper) {
		ClassFileRenamer classFileRenamer = new ClassFileRenamer(cf, mapper);
		return classFileRenamer.rename();
	}

	static class SignatureParser {
		protected String	source;
		protected int		index;

		public void parse(String signature) {
			source = signature;
			index = 0;
			parseSignature();
		}

		protected void parseSignature() {
			if (current() == '<') {
				consume('<');
				typeParameters();
				consume('>');
			}

			if (current() == '(') {
				consume('(');
				while (isType()) {
					type();
				}
				consume(')');
			}

			while (isType()) {
				type();
			}

			while (current() == '^') {
				consume('^');
				type();
			}
			if (!isEof())
				throw error("Not parsed to the end");
		}

		protected boolean isType() {
			if (isEof())
				return false;

			switch (current()) {
				case 'B' :
				case 'C' :
				case 'D' :
				case 'F' :
				case 'I' :
				case 'J' :
				case 'S' :
				case 'L' :
				case 'T' :
				case 'V' :
				case 'Z' :
				case '+' :
				case '-' :
				case '*' :
				case '[' :
					return true;
				default :
					return false;
			}
		}

		protected void typeParameters() {
			while (isIdentifier()) {

				identifier();

				while (current() == ':') {
					next();
					if (isType())
						type();
				}

			}
		}

		protected boolean isIdentifier() {
			if (isEof())
				return false;

			switch (current()) {
				case ';' :
				case '.' :
				case '/' :
				case '[' :
				case ':' :
				case '>' :
				case '<' :
					return false;
				default :
					return true;
			}
		}

		protected void type() {
			switch (current()) {
				case 'B' :
				case 'C' :
				case 'D' :
				case 'F' :
				case 'I' :
				case 'J' :
				case 'S' :
				case 'V' :
				case 'Z' :
					primitive();
					return;

				case '+' : {
					next();
					type();
					break;
				}
				case '*' : {
					next();
					break;
				}
				case '-' : {
					next();
					type();
					break;
				}

				case 'L' :
					classType();
					break;

				case 'T' :
					variable();
					break;

				case '[' :
					next();
					type();
					break;

				default :
					throw error("Unexpected character");
			}
		}

		protected void variable() {
			consume('T');
			identifier();
			consume(';');
		}

		protected void identifier() {
			while (isIdentifier()) {
				next();
			}
		}

		protected void primitive() {
			next();
		}

		protected void classType() {
			consume('L');

			do {
				binary();
				if (current() == '<') {
					consume('<');
					while (isType())
						type();
					consume('>');
				}
			} while (expect('.'));
			consume(';');
		}

		protected void binary() {
			do {
				identifier();
			} while (expect('/'));
		}

		protected boolean expect(char c) {
			if (current() == c) {
				next();
				return true;
			}
			return false;
		}

		protected RuntimeException error(String format, Object... args) {
			return new RuntimeException(String.format(format, args)
				.concat("\n")
				.concat(toString()));
		}

		protected char assume(char c, String format, Object... args) {
			if (c == current()) {
				return next();
			}
			throw error(format, args);
		}

		protected char current() {
			if (isEof())
				return 0;
			return source.charAt(index);
		}

		protected void consume(char c) {
			assume(c, "expected %s", c);
		}

		protected char next() {
			index++;
			return current();
		}

		protected boolean isEof() {
			return index >= source.length();
		}

		@Override
		public String toString() {
			int begin = Math.max(0, index - 100);
			int end = Math.min(source.length(), index + 100);
			String substring = Strings.substring(source, begin, end)
				.concat("\n");

			if (index - begin > 0)
				substring = substring.concat(Strings.times(" ", index - begin));

			return substring.concat("^");
		}

		protected int index() {
			return index;
		}

		protected String source() {
			return source;
		}
	}

	static class Replace {
		Replace(Replace prev, String binary, int from, int to) {
			this.replace = binary;
			this.from = from;
			this.to = to;
			this.prev = prev;
		}

		final Replace	prev;
		final String	replace;
		final int		from, to;

		void replace(StringBuilder sb) {
			sb.replace(from, to, replace);
			if (prev != null)
				prev.replace(sb);
		}
	}

	static class SignatureRenamer extends SignatureParser {

		final Function<String, String>	mapping;
		Replace							replace	= null;

		public String rename(String signature) {
			replace = null;
			parse(signature);
			return renamed();
		}

		public SignatureRenamer(Function<String, String> mapper) {
			this.mapping = mapper;
		}

		private String renamed() {
			if (replace == null)
				return null;

			StringBuilder sb = new StringBuilder(source());
			replace.replace(sb);
			return sb.toString();
		}

		@Override
		protected void binary() {
			int begin = index();
			super.binary();
			int end = index();
			String type = source.substring(begin, end);
			String renamed = mapping.apply(type);

			if (renamed == null)
				return;

			replace = new Replace(replace, renamed, begin, end);
		}

	}

	ClassFileRenamer(ClassFile classFile, Function<String, String> mapper) {
		this.sp = new SignatureRenamer(mapper);
		this.classFile = classFile;
		this.mapper = mapper;
		this.builder = new ClassFileBuilder(classFile);
	}

	Optional<ClassFile> rename() {
		builder.attributes()
			.clear();
		builder.interfaces()
			.clear();
		builder.methods()
			.clear();
		builder.fields()
			.clear();
		builder.interfaces()
			.clear();

		for (MethodInfo mi : classFile.methods) {
			MethodInfo renamed = new MethodInfo(mi.access, mi.name, renameSignatureOrDescriptor(mi.descriptor),
				rename(mi.attributes));
			builder.methods(renamed);
		}
		for (FieldInfo fi : classFile.fields) {
			FieldInfo renamed = new FieldInfo(fi.access, fi.name, renameSignatureOrDescriptor(fi.descriptor),
				rename(fi.attributes));
			builder.fields(renamed);
		}

		builder.attributes(rename(classFile.attributes));

		MutableConstantPool pool = builder.constant_pool();

		for (String interf : classFile.interfaces) {
			String renamed = renameBinary(interf);
			builder.interfaces(renamed);
		}

		String thisClass = renameBinary(builder.this_class());
		builder.this_class(thisClass);

		builder.super_class(renameBinary(classFile.super_class));

		for (int index = 0; index < builder.constant_pool()
			.size(); index++) {
			switch (builder.constant_pool()
				.tag(index)) {

			}
		}
		for (Object entry : builder.constant_pool()) {
			InfoTag tag = InfoTag.tag(entry);
			if (tag == null)
				continue;

			switch (tag) {
				case NameAndType : {
					NameAndTypeInfo info = tag.cast(entry);
					rename(info.descriptor_index);
				}
					break;

				case MethodType : {
					MethodTypeInfo info = tag.cast(entry);
					rename(info.descriptor_index);
				}

				case Class : {
					ClassInfo info = tag.cast(entry);
					String original = classFile.constant_pool.utf8(info.class_index);
					renameBinary(original);
					break;
				}
				case Double :
				case Dynamic :
				case Fieldref :
				case Float :
				case Integer :
				case InterfaceMethodRef :
				case InvokeDynamic :
				case Long :
				case MethodHandle :
				case Methodref :
				case Module :
				case Package :
				case String :
				case Utf8 :
					break;
			}
		}

		//
		// The actual pool rename is delayed to not interfere with the 'find'
		// that many
		// infos and attributes do. If we rename eagerly, they will add new
		// entries
		//

		for (Entry<Integer, String> entry : renamed.entrySet()) {
			builder.constant_pool()
				.entry(entry.getKey(), entry.getValue());
		}

		builder.constant_pool(pool);
		return Optional.of(builder.build());

	}

	private void rename(int descriptor_index) {
		String original = classFile.constant_pool.utf8(descriptor_index);
		String rename = sp.rename(original);
		if (rename != null) {
			renamed.put(descriptor_index, rename);
		}
	}

	private Attribute[] rename(Attribute[] attributes) {
		Attribute[] result = null;

		for (int index = 0; index < attributes.length; index++) {
			Attribute attr = rename(attributes[index]);
			if (attr != null) {
				if (result != null) {
					result = attributes.clone();
					result[index] = attr;
				}
			}
		}

		return result == null ? attributes : result;
	}

	private Attribute rename(Attribute attribute) {
		Attribute.AttributeTag tag = attribute.tag();
		switch (tag) {
			case AnnotationDefault :
			case BootstraMethods :
			case Code :
			case ConstantValue :
			case Deprecated :
				break;

			case EnclosingMethod : {
				EnclosingMethodAttribute ema = tag.cast(attribute);
				String class_name = renameBinary(ema.class_name);
				String method_descriptor = renameSignatureOrDescriptor(ema.method_descriptor);
				boolean changed = false;
				if (class_name != null) {
					changed = true;
				} else {
					class_name = ema.class_name;
				}
				if (method_descriptor != null) {
					changed = true;
				} else {
					method_descriptor = ema.method_descriptor;
				}
				if (changed)
					return new EnclosingMethodAttribute(class_name, ema.method_name, method_descriptor);
				else
					return ema;

			}

			case Exceptions :
				break;

			case InnerClasses : {
				InnerClassesAttribute ica = tag.cast(attribute);
				InnerClass[] innerClasses = new InnerClass[ica.classes.length];
				for (int i = 0; i < ica.classes.length; i++) {
					InnerClass ic = ica.classes[i];
					String inner_class = renameBinary(ic.inner_class);
					String outer_class = renameBinary(ic.outer_class);
					innerClasses[i] = new InnerClass(inner_class, outer_class, ic.inner_name, ic.inner_access);
				}
				return new InnerClassesAttribute(innerClasses);
			}

			case LineNumberTable :
				break;

			case LocalVariableTable : {
				LocalVariableTableAttribute lvtta = tag.cast(attribute);
				LocalVariable[] lvts = null;

				for (int i = 0; i < lvtta.local_variable_table.length; i++) {
					LocalVariable lvt = lvtta.local_variable_table[i];
					String descriptor = renameSignatureOrDescriptor(lvt.descriptor);
					if (descriptor != null) {
						lvt = new LocalVariable(lvt.start_pc, lvt.length, lvt.name, descriptor, lvt.index);
						if (lvts == null) {
							lvts = lvtta.local_variable_table.clone();
						}
						lvts[i] = lvt;
					}
				}
				if (lvts != null) {
					return new LocalVariableTableAttribute(lvts);
				} else
					return null;
			}

			case LocalVariableTypeTable : {
				LocalVariableTypeTableAttribute lvtta = tag.cast(attribute);
				LocalVariableType[] lvts = null;

				for (int i = 0; i < lvtta.local_variable_type_table.length; i++) {
					LocalVariableType lvt = lvtta.local_variable_type_table[i];
					String signature = renameSignatureOrDescriptor(lvt.signature);
					if (signature != null) {
						lvt = new LocalVariableType(lvt.start_pc, lvt.length, lvt.name, signature, lvt.index);
						if (lvts == null) {
							lvts = lvtta.local_variable_type_table.clone();
						}
						lvts[i] = lvt;
					}
				}
				if (lvts != null) {
					return new LocalVariableTypeTableAttribute(lvts);
				} else
					return null;

			}

			case MethodParameters :
			case Module :
			case ModuleMainClass :
			case ModulePackages :
			case NestHost :
			case NestMembers :
				break;

			case Preview : {
				if (attribute instanceof RecordAttribute) {
					RecordAttribute ra = (RecordAttribute) attribute;
					RecordComponent[] components = null;
					for (int i = 0; i < ra.components.length; i++) {
						RecordComponent rc = ra.components[i];
						String descriptor = renameSignatureOrDescriptor(rc.descriptor);
						if (descriptor != null) {
							if (components == null) {
								components = ra.components.clone();
							}
							components[i] = new RecordComponent(rc.name, descriptor, rc.attributes);
						}
					}
					if (components != null)
						return new RecordAttribute(components);
					else
						return ra;
				}
				break;
			}
			case RuntimeInvisibleAnnotations :
			case RuntimeInvisibleParameterAnnotations :
			case RuntimeInvisibleTypeAnnotations :
			case RuntimeVisibleAnnotations :
			case RuntimeVisibleParameterAnnotations :
			case RuntimeVisibleTypeAnnotations :
				break;

			case Signature : {
				SignatureAttribute a = tag.cast(attribute);
				String renamed = renameSignatureOrDescriptor(a.signature);
				return new SignatureAttribute(renamed);
			}

			case SourceDebugExtension :
			case SourceFile :
			case StackMapTable :
			case Synthetic :
			case Unrecognized :
				break;

		}
		return null;
	}

	private String renameSignatureOrDescriptor(String signature) {
		int index = find(signature);
		return renameSignatureOrDescriptor(index);
	}

	private String renameSignatureOrDescriptor(int index) {
		if (renamed.containsKey(index))
			return renamed.get(index);

		String signature = classFile.constant_pool.entry(index);
		String replace = sp.rename(signature);
		if (replace != null) {
			renamed.put(index, replace);
			changed = true;
			return replace;
		} else {
			renamed.put(index, signature);
			return signature;
		}
	}

	private String renameBinary(String binaryName) {
		if (binaryName == null)
			return null;

		int index = find(binaryName);
		return renameBinary(index);
	}

	private String renameBinary(int index) {
		if (renamed.containsKey(index))
			return renamed.get(index);

		String binaryName = classFile.constant_pool.entry(index);
		String rename = mapper.apply(binaryName);
		if (rename == null) {
			renamed.put(index, binaryName);
			return binaryName;
		}

		changed = true;
		renamed.put(index, rename);
		return rename;
	}

	private int find(String string) {
		for (int i = 0; i < classFile.constant_pool.size(); i++) {
			if (string.equals(classFile.constant_pool.entry(i)))
				return i;
		}
		return -1;
	}

}
