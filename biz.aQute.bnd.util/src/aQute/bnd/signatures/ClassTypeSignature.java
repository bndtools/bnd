package aQute.bnd.signatures;

import static aQute.bnd.signatures.Signatures.intern;
import static aQute.bnd.signatures.SimpleClassTypeSignature.parseSimpleClassTypeSignature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import aQute.lib.stringrover.StringRover;

public class ClassTypeSignature implements ReferenceTypeSignature, ThrowsSignature {
	public static final ClassTypeSignature	OBJECT	= new ClassTypeSignature("java/lang/Object", "java/lang/",
		new SimpleClassTypeSignature("Object", TypeArgument.EMPTY), SimpleClassTypeSignature.EMPTY);
	static final ClassTypeSignature[]		EMPTY	= new ClassTypeSignature[0];
	public final String						packageSpecifier;
	public final SimpleClassTypeSignature	classType;
	public final SimpleClassTypeSignature[]	innerTypes;
	public final String						binary;

	public ClassTypeSignature(String binary, String packageSpecifier, SimpleClassTypeSignature classType,
		SimpleClassTypeSignature[] innerTypes) {
		this.binary = binary;
		this.packageSpecifier = packageSpecifier;
		this.classType = classType;
		this.innerTypes = innerTypes;
	}

	@Override
	public int hashCode() {
		int result = 1 * 31 + packageSpecifier.hashCode();
		result = result * 31 + classType.hashCode();
		result = result * 31 + Arrays.hashCode(innerTypes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ClassTypeSignature)) {
			return false;
		}
		ClassTypeSignature other = (ClassTypeSignature) obj;
		return Objects.equals(packageSpecifier, other.packageSpecifier) && Objects.equals(classType, other.classType)
			&& Arrays.equals(innerTypes, other.innerTypes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('L')
			.append(packageSpecifier)
			.append(classType);
		for (SimpleClassTypeSignature t : innerTypes) {
			sb.append('.')
				.append(t);
		}
		return sb.append(';')
			.toString();
	}

	static ClassTypeSignature parseClassTypeSignature(StringRover signature) {
		StringBuilder binary = new StringBuilder();
		assert signature.charAt(0) == 'L';
		signature.increment();
		int offset = 0;
		int slash = -1;
		for (char c = signature.charAt(offset); c != '.' && c != ';' && c != '<'; c = signature.charAt(++offset)) {
			if (c == '/') {
				slash = offset;
			}
		}
		String packageSpecifier = intern(signature.substring(0, slash + 1));
		binary.append(packageSpecifier);
		SimpleClassTypeSignature classType = parseSimpleClassTypeSignature(signature.increment(slash + 1));
		binary.append(classType.identifier);
		ClassTypeSignature result;
		if (signature.charAt(0) == '.') {
			List<SimpleClassTypeSignature> list = new ArrayList<>();
			do {
				SimpleClassTypeSignature item = parseSimpleClassTypeSignature(signature.increment());
				list.add(item);
				binary.append('$')
					.append(item.identifier);
			} while (signature.charAt(0) == '.');
			result = new ClassTypeSignature(intern(binary.toString()), packageSpecifier, classType,
				list.toArray(SimpleClassTypeSignature.EMPTY));
		} else {
			result = new ClassTypeSignature(intern(binary.toString()), packageSpecifier, classType,
				SimpleClassTypeSignature.EMPTY);
		}
		assert signature.charAt(0) == ';';
		signature.increment();
		return result;
	}
}
