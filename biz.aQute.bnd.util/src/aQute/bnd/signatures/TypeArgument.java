package aQute.bnd.signatures;

import static aQute.bnd.signatures.Signatures.parseReferenceTypeSignature;

import java.util.Objects;
import java.util.Set;

import aQute.lib.stringrover.StringRover;

public class TypeArgument {
	static final TypeArgument[]			EMPTY	= new TypeArgument[0];
	public final WildcardIndicator		wildcard;
	public final ReferenceTypeSignature	type;

	public TypeArgument(WildcardIndicator wildcard, ReferenceTypeSignature type) {
		this.wildcard = wildcard;
		this.type = type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(wildcard, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TypeArgument other)) {
			return false;
		}
		return (wildcard == other.wildcard) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return switch (wildcard) {
			case WILD -> "*";
			case EXACT -> type.toString();
			case SUPER -> "-" + type;
			case EXTENDS -> "+" + type;
			default -> wildcard.toString() + type;
		};
	}

	static TypeArgument parseTypeArgument(StringRover signature) {
		return switch (signature.charAt(0)) {
			case '*' -> {
				signature.increment();
				yield new TypeArgument(WildcardIndicator.WILD, ClassTypeSignature.OBJECT);
			}
			case '+' -> new TypeArgument(WildcardIndicator.EXTENDS, parseReferenceTypeSignature(signature.increment()));
			case '-' -> new TypeArgument(WildcardIndicator.SUPER, parseReferenceTypeSignature(signature.increment()));
			default -> new TypeArgument(WildcardIndicator.EXACT, parseReferenceTypeSignature(signature));
		};
	}

	static void erasedBinaryReferences(TypeArgument[] typeArguments, Set<String> references) {
		for (TypeArgument typeArgument : typeArguments) {
			if (typeArgument.wildcard != WildcardIndicator.WILD) {
				Signatures.erasedBinaryReferences(typeArgument.type, references);
			}
		}
	}
}
