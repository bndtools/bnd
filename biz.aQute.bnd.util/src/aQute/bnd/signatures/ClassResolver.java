package aQute.bnd.signatures;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class ClassResolver {
	private final ClassSignature classSig;

	public ClassResolver(ClassSignature classSig) {
		this.classSig = requireNonNull(classSig);
	}

	@SuppressWarnings("unchecked")
	public <T extends JavaTypeSignature> T resolveType(JavaTypeSignature type) {
		if ((type instanceof BaseType) || (type instanceof ClassTypeSignature)) {
			return (T) type;
		}
		if (type instanceof ArrayTypeSignature) {
			return (T) resolveType((ArrayTypeSignature) type);
		}
		return (T) resolveType((TypeVariableSignature) type);
	}

	public ArrayTypeSignature resolveType(ArrayTypeSignature arrayType) {
		JavaTypeSignature component = arrayType.component;
		int depth = 1;
		while (component instanceof ArrayTypeSignature) {
			depth++;
			component = ((ArrayTypeSignature) component).component;
		}
		if ((component instanceof BaseType) || (component instanceof ClassTypeSignature)) {
			return arrayType;
		}
		component = resolveType((TypeVariableSignature) component);
		arrayType = new ArrayTypeSignature(component);
		while (--depth > 0) {
			arrayType = new ArrayTypeSignature(arrayType);
		}
		return arrayType;
	}

	public ReferenceTypeSignature resolveType(TypeArgument typeArgument) {
		return resolveType(typeArgument.type);
	}

	public ReferenceTypeSignature resolveType(TypeVariableSignature typeVariable) {
		return resolveType(classSig.typeParameters, typeVariable);
	}

	protected ReferenceTypeSignature resolveType(TypeParameter[] typeParameters, TypeVariableSignature typeVariable) {
		String variable = typeVariable.identifier;
		for (TypeParameter typeParameter : typeParameters) {
			if (variable.equals(typeParameter.identifier)) {
				ReferenceTypeSignature classBound = typeParameter.classBound;
				if (classBound != null) {
					return resolveType(classBound);
				}
				for (ReferenceTypeSignature interfaceBound : typeParameter.interfaceBounds) {
					return resolveType(interfaceBound);
				}
				break;
			}
		}
		return null;
	}

	@Override
	public int hashCode() {
		return 31 + classSig.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ClassResolver)) {
			return false;
		}
		ClassResolver other = (ClassResolver) obj;
		return Objects.equals(classSig, other.classSig);
	}

	@Override
	public String toString() {
		return "ClassResolver: " + classSig;
	}
}
