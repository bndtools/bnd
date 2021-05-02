package aQute.bnd.signatures;

import static aQute.bnd.signatures.Signatures.parseReferenceTypeSignature;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import aQute.lib.stringrover.StringRover;

public class FieldSignature implements Signature {
	public final ReferenceTypeSignature type;

	public static FieldSignature of(String signature) {
		return parseFieldSignature(new StringRover(signature));
	}

	public FieldSignature(ReferenceTypeSignature type) {
		this.type = type;
	}

	@Override
	public Set<String> erasedBinaryReferences() {
		Set<String> references = new HashSet<>();
		Signatures.erasedBinaryReferences(type, references);
		return references;
	}

	@Override
	public int hashCode() {
		return 31 + type.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FieldSignature)) {
			return false;
		}
		FieldSignature other = (FieldSignature) obj;
		return Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return type.toString();
	}

	static FieldSignature parseFieldSignature(StringRover signature) {
		return new FieldSignature(parseReferenceTypeSignature(signature));
	}
}
