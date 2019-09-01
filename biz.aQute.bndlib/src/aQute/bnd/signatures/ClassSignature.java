package aQute.bnd.signatures;

import static aQute.bnd.signatures.ClassTypeSignature.parseClassTypeSignature;
import static aQute.bnd.signatures.Signatures.isEmpty;
import static aQute.bnd.signatures.TypeParameter.parseTypeParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import aQute.lib.stringrover.StringRover;

public class ClassSignature implements Signature {
	public final TypeParameter[]		typeParameters;
	public final ClassTypeSignature		superClass;
	public final ClassTypeSignature[]	superInterfaces;

	public static ClassSignature of(String signature) {
		return parseClassSignature(new StringRover(signature));
	}

	public ClassSignature(TypeParameter[] typeParameters, ClassTypeSignature superClass,
		ClassTypeSignature[] superInterfaces) {
		this.typeParameters = typeParameters;
		this.superClass = superClass;
		this.superInterfaces = superInterfaces;
	}

	@Override
	public Set<String> erasedBinaryReferences() {
		Set<String> references = new HashSet<>();
		TypeParameter.erasedBinaryReferences(typeParameters, references);
		Signatures.erasedBinaryReferences(superClass, references);
		for (ClassTypeSignature superInterface : superInterfaces) {
			Signatures.erasedBinaryReferences(superInterface, references);
		}
		return references;
	}

	@Override
	public int hashCode() {
		int result = 1 * 31 + Arrays.hashCode(typeParameters);
		result = result * 31 + superClass.hashCode();
		result = result * 31 + Arrays.hashCode(superInterfaces);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ClassSignature)) {
			return false;
		}
		ClassSignature other = (ClassSignature) obj;
		return Objects.equals(superClass, other.superClass) && Arrays.equals(typeParameters, other.typeParameters)
			&& Arrays.equals(superInterfaces, other.superInterfaces);
	}

	@Override
	public String toString() {
		if (isEmpty(typeParameters) && isEmpty(superInterfaces)) {
			return superClass.toString();
		}
		StringBuilder sb = new StringBuilder();
		if (!isEmpty(typeParameters)) {
			sb.append('<');
			for (TypeParameter t : typeParameters) {
				sb.append(t);
			}
			sb.append('>');
		}
		sb.append(superClass);
		for (ClassTypeSignature t : superInterfaces) {
			sb.append(t);
		}
		return sb.toString();
	}

	static ClassSignature parseClassSignature(StringRover signature) {
		TypeParameter[] typeParameters = parseTypeParameters(signature);
		ClassTypeSignature superClass = parseClassTypeSignature(signature);
		if (signature.isEmpty()) {
			return new ClassSignature(typeParameters, superClass, ClassTypeSignature.EMPTY);
		}
		List<ClassTypeSignature> list = new ArrayList<>();
		do {
			ClassTypeSignature item = parseClassTypeSignature(signature);
			list.add(item);
		} while (!signature.isEmpty());
		return new ClassSignature(typeParameters, superClass, list.toArray(ClassTypeSignature.EMPTY));
	}
}
