package aQute.bnd.signatures;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class MethodResolver extends ClassResolver {
	private final MethodSignature methodSig;

	public MethodResolver(ClassSignature classSig, MethodSignature methodSig) {
		super(classSig);
		this.methodSig = requireNonNull(methodSig);
	}

	public JavaTypeSignature resolveParameter(int index) {
		return resolveType(methodSig.parameterTypes[index]);
	}

	public Result resolveResult() {
		Result resultType = methodSig.resultType;
		if (resultType instanceof VoidDescriptor) {
			return resultType;
		}

		return resolveType((JavaTypeSignature) resultType);
	}

	@Override
	public ReferenceTypeSignature resolveType(TypeVariableSignature typeVariable) {
		ReferenceTypeSignature result = resolveType(methodSig.typeParameters, typeVariable);
		if (result != null) {
			return result;
		}
		return super.resolveType(typeVariable);
	}

	@Override
	public int hashCode() {
		int result = 1 * 31 + methodSig.hashCode();
		result = result * 31 + super.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MethodResolver)) {
			return false;
		}
		MethodResolver other = (MethodResolver) obj;
		return Objects.equals(methodSig, other.methodSig) && super.equals(other);
	}

	@Override
	public String toString() {
		return "MethodResolver: " + methodSig;
	}
}
