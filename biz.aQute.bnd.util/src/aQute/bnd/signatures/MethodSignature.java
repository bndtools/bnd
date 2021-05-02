package aQute.bnd.signatures;

import static aQute.bnd.signatures.ClassTypeSignature.parseClassTypeSignature;
import static aQute.bnd.signatures.Signatures.EMPTY_JavaTypeSignature;
import static aQute.bnd.signatures.Signatures.isEmpty;
import static aQute.bnd.signatures.Signatures.parseJavaTypeSignature;
import static aQute.bnd.signatures.TypeParameter.parseTypeParameters;
import static aQute.bnd.signatures.TypeVariableSignature.parseTypeVariableSignature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import aQute.lib.stringrover.StringRover;

public class MethodSignature implements Signature {
	public final TypeParameter[]		typeParameters;
	public final JavaTypeSignature[]	parameterTypes;
	public final Result					resultType;
	public final ThrowsSignature[]		throwTypes;

	public static MethodSignature of(String signature) {
		return parseMethodSignature(new StringRover(signature));
	}

	public MethodSignature(TypeParameter[] typeParameters, JavaTypeSignature[] parameterTypes, Result resultType,
		ThrowsSignature[] throwTypes) {
		this.typeParameters = typeParameters;
		this.parameterTypes = parameterTypes;
		this.resultType = resultType;
		this.throwTypes = throwTypes;
	}

	@Override
	public Set<String> erasedBinaryReferences() {
		Set<String> references = new HashSet<>();
		TypeParameter.erasedBinaryReferences(typeParameters, references);
		for (JavaTypeSignature parameterType : parameterTypes) {
			Signatures.erasedBinaryReferences(parameterType, references);
		}
		if (resultType instanceof ReferenceTypeSignature) {
			Signatures.erasedBinaryReferences((ReferenceTypeSignature) resultType, references);
		}
		for (ThrowsSignature throwType : throwTypes) {
			Signatures.erasedBinaryReferences((JavaTypeSignature) throwType, references);
		}
		return references;
	}

	@Override
	public int hashCode() {
		int result = 1 * 31 + Arrays.hashCode(typeParameters);
		result = result * 31 + Arrays.hashCode(parameterTypes);
		result = result * 31 + resultType.hashCode();
		result = result * 31 + Arrays.hashCode(throwTypes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MethodSignature)) {
			return false;
		}
		MethodSignature other = (MethodSignature) obj;
		return Objects.equals(resultType, other.resultType) && Arrays.equals(typeParameters, other.typeParameters)
			&& Arrays.equals(parameterTypes, other.parameterTypes) && Arrays.equals(throwTypes, other.throwTypes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!isEmpty(typeParameters)) {
			sb.append('<');
			for (TypeParameter t : typeParameters) {
				sb.append(t);
			}
			sb.append('>');
		}
		sb.append('(');
		for (JavaTypeSignature t : parameterTypes) {
			sb.append(t);
		}
		sb.append(')')
			.append(resultType);
		for (ThrowsSignature t : throwTypes) {
			sb.append('^')
				.append(t);
		}
		return sb.toString();
	}

	static MethodSignature parseMethodSignature(StringRover signature) {
		TypeParameter[] typeParameters = parseTypeParameters(signature);
		JavaTypeSignature[] parameterTypes;
		assert signature.charAt(0) == '(';
		if (signature.increment()
			.charAt(0) != ')') {
			List<JavaTypeSignature> list = new ArrayList<>();
			do {
				JavaTypeSignature item = parseJavaTypeSignature(signature);
				list.add(item);
			} while (signature.charAt(0) != ')');
			parameterTypes = list.toArray(EMPTY_JavaTypeSignature);
		} else {
			parameterTypes = EMPTY_JavaTypeSignature;
		}
		Result returnType = parseResult(signature.increment());
		if (signature.isEmpty()) {
			return new MethodSignature(typeParameters, parameterTypes, returnType, EMPTY_ThrowsSignature);
		}
		List<ThrowsSignature> list = new ArrayList<>();
		do {
			ThrowsSignature item = parseThrowsSignature(signature);
			list.add(item);
		} while (!signature.isEmpty());
		return new MethodSignature(typeParameters, parameterTypes, returnType, list.toArray(EMPTY_ThrowsSignature));
	}

	static Result parseResult(StringRover signature) {
		switch (signature.charAt(0)) {
			case 'V' :
				signature.increment();
				return VoidDescriptor.V;
			default :
				return parseJavaTypeSignature(signature);
		}
	}

	static final ThrowsSignature[] EMPTY_ThrowsSignature = new ThrowsSignature[0];

	static ThrowsSignature parseThrowsSignature(StringRover signature) {
		assert signature.charAt(0) == '^';
		switch (signature.charAt(1)) {
			case 'T' :
				return parseTypeVariableSignature(signature.increment());
			case 'L' :
				return parseClassTypeSignature(signature.increment());
			default :
				throw new IllegalArgumentException("invalid signature: " + signature);
		}
	}
}
