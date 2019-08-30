package aQute.bnd.signatures;

import static aQute.bnd.signatures.Signatures.EMPTY_ReferenceTypeSignature;
import static aQute.bnd.signatures.Signatures.intern;
import static aQute.bnd.signatures.Signatures.parseReferenceTypeSignature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import aQute.lib.stringrover.StringRover;

public class TypeParameter {
	static final TypeParameter[]			EMPTY	= new TypeParameter[0];
	public final String						identifier;
	public final ReferenceTypeSignature		classBound;
	public final ReferenceTypeSignature[]	interfaceBounds;

	public TypeParameter(String identifier, ReferenceTypeSignature classBound,
		ReferenceTypeSignature[] interfaceBounds) {
		this.identifier = identifier;
		this.classBound = classBound;
		this.interfaceBounds = interfaceBounds;
	}

	@Override
	public int hashCode() {
		int result = 1 * 31 + identifier.hashCode();
		result = result * 31 + (classBound == null ? 0 : classBound.hashCode());
		result = result * 31 + Arrays.hashCode(interfaceBounds);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TypeParameter)) {
			return false;
		}
		TypeParameter other = (TypeParameter) obj;
		return Objects.equals(identifier, other.identifier) && Objects.equals(classBound, other.classBound)
			&& Arrays.equals(interfaceBounds, other.interfaceBounds);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(identifier)
			.append(':');
		if (classBound != null) {
			sb.append(classBound);
		}
		for (ReferenceTypeSignature t : interfaceBounds) {
			sb.append(':')
				.append(t);
		}
		return sb.toString();
	}

	static TypeParameter parseTypeParameter(StringRover signature) {
		int end = signature.indexOf(':', 0);
		String identifier = intern(signature.substring(0, end));
		signature.increment(end + 1);
		ReferenceTypeSignature classBound;
		char c = signature.isEmpty() ? 0 : signature.charAt(0);
		if (c == 'L' || c == 'T' || c == '[') {
			classBound = parseReferenceTypeSignature(signature);
			c = signature.isEmpty() ? 0 : signature.charAt(0);
		} else {
			classBound = null;
		}
		if (c != ':') {
			return new TypeParameter(identifier, classBound, EMPTY_ReferenceTypeSignature);
		}
		List<ReferenceTypeSignature> list = new ArrayList<>();
		do {
			ReferenceTypeSignature item = parseReferenceTypeSignature(signature.increment());
			list.add(item);
			c = signature.isEmpty() ? 0 : signature.charAt(0);
		} while (c == ':');
		return new TypeParameter(identifier, classBound, list.toArray(EMPTY_ReferenceTypeSignature));
	}

	static TypeParameter[] parseTypeParameters(StringRover signature) {
		if (signature.charAt(0) != '<') {
			return EMPTY;
		}
		signature.increment();
		List<TypeParameter> list = new ArrayList<>();
		do {
			TypeParameter item = parseTypeParameter(signature);
			list.add(item);
		} while (signature.charAt(0) != '>');
		signature.increment();
		return list.toArray(EMPTY);
	}

	static void erasedBinaryReferences(TypeParameter[] typeParameters, Set<String> references) {
		for (TypeParameter typeParameter : typeParameters) {
			Signatures.erasedBinaryReferences(typeParameter.classBound, references);
			for (ReferenceTypeSignature interfaceBound : typeParameter.interfaceBounds) {
				Signatures.erasedBinaryReferences(interfaceBound, references);
			}
		}
	}
}
