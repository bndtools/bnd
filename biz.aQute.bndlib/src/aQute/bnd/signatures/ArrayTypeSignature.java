package aQute.bnd.signatures;

import static aQute.bnd.signatures.Signatures.parseJavaTypeSignature;

import java.util.Objects;

import aQute.lib.stringrover.StringRover;

public class ArrayTypeSignature implements ReferenceTypeSignature {
	public final JavaTypeSignature component;

	public ArrayTypeSignature(JavaTypeSignature component) {
		this.component = component;
	}

	@Override
	public int hashCode() {
		return 31 + component.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ArrayTypeSignature)) {
			return false;
		}
		ArrayTypeSignature other = (ArrayTypeSignature) obj;
		return Objects.equals(component, other.component);
	}

	@Override
	public String toString() {
		return "[" + component;
	}

	static ArrayTypeSignature parseArrayTypeSignature(StringRover signature) {
		assert signature.charAt(0) == '[';
		JavaTypeSignature component = parseJavaTypeSignature(signature.increment());
		return new ArrayTypeSignature(component);
	}
}
