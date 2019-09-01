package aQute.bnd.signatures;

import static aQute.bnd.signatures.Signatures.intern;
import static aQute.bnd.signatures.Signatures.isEmpty;
import static aQute.bnd.signatures.TypeArgument.parseTypeArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import aQute.lib.stringrover.StringRover;

public class SimpleClassTypeSignature {
	static final SimpleClassTypeSignature[]	EMPTY	= new SimpleClassTypeSignature[0];
	public final String						identifier;
	public final TypeArgument[]				typeArguments;

	public SimpleClassTypeSignature(String identifier, TypeArgument[] typeArguments) {
		this.identifier = identifier;
		this.typeArguments = typeArguments;
	}

	@Override
	public int hashCode() {
		int result = 1 * 31 + identifier.hashCode();
		result = result * 31 + Arrays.hashCode(typeArguments);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SimpleClassTypeSignature)) {
			return false;
		}
		SimpleClassTypeSignature other = (SimpleClassTypeSignature) obj;
		return Objects.equals(identifier, other.identifier) && Arrays.equals(typeArguments, other.typeArguments);
	}

	@Override
	public String toString() {
		if (isEmpty(typeArguments)) {
			return identifier;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(identifier)
			.append('<');
		for (TypeArgument t : typeArguments) {
			sb.append(t);
		}
		return sb.append('>')
			.toString();
	}

	static SimpleClassTypeSignature parseSimpleClassTypeSignature(StringRover signature) {
		int offset = 0;
		for (char c = signature.charAt(offset); c != '.' && c != ';' && c != '<'; c = signature.charAt(++offset)) {}
		String identifier = intern(signature.substring(0, offset));
		signature.increment(offset);
		if (signature.charAt(0) != '<') {
			return new SimpleClassTypeSignature(identifier, TypeArgument.EMPTY);
		}
		signature.increment();
		List<TypeArgument> list = new ArrayList<>();
		do {
			TypeArgument item = parseTypeArgument(signature);
			list.add(item);
		} while (signature.charAt(0) != '>');
		signature.increment();
		return new SimpleClassTypeSignature(identifier, list.toArray(TypeArgument.EMPTY));
	}
}
