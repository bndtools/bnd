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
		if (!(obj instanceof TypeArgument)) {
			return false;
		}
		TypeArgument other = (TypeArgument) obj;
		return (wildcard == other.wildcard) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		switch (wildcard) {
			case WILD :
				return "*";
			case EXACT :
				return type.toString();
			case SUPER :
				return "-" + type;
			case EXTENDS :
				return "+" + type;
			default :
				return wildcard.toString() + type;
		}
	}

	static TypeArgument parseTypeArgument(StringRover signature) {
		switch (signature.charAt(0)) {
			case '*' :
				signature.increment();
				return new TypeArgument(WildcardIndicator.WILD, ClassTypeSignature.OBJECT);
			case '+' :
				return new TypeArgument(WildcardIndicator.EXTENDS, parseReferenceTypeSignature(signature.increment()));
			case '-' :
				return new TypeArgument(WildcardIndicator.SUPER, parseReferenceTypeSignature(signature.increment()));
			default :
				return new TypeArgument(WildcardIndicator.EXACT, parseReferenceTypeSignature(signature));
		}
	}

	static void erasedBinaryReferences(TypeArgument[] typeArguments, Set<String> references) {
		for (TypeArgument typeArgument : typeArguments) {
			if (typeArgument.wildcard != WildcardIndicator.WILD) {
				Signatures.erasedBinaryReferences(typeArgument.type, references);
			}
		}
	}
}
