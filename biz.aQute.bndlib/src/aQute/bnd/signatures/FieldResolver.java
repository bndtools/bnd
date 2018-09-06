package aQute.bnd.signatures;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class FieldResolver extends ClassResolver {
	private final FieldSignature fieldSig;

	public FieldResolver(ClassSignature classSig, FieldSignature fieldSig) {
		super(classSig);
		this.fieldSig = requireNonNull(fieldSig);
	}

	public ReferenceTypeSignature resolveField() {
		return resolveType(fieldSig.type);
	}

	@Override
	public int hashCode() {
		int result = 1 * 31 + fieldSig.hashCode();
		result = result * 31 + super.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FieldResolver)) {
			return false;
		}
		FieldResolver other = (FieldResolver) obj;
		return Objects.equals(fieldSig, other.fieldSig) && super.equals(other);
	}

	@Override
	public String toString() {
		return "FieldResolver: " + fieldSig;
	}
}
