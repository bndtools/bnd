package aQute.bnd.signatures;

import static aQute.bnd.signatures.ArrayTypeSignature.parseArrayTypeSignature;
import static aQute.bnd.signatures.ClassTypeSignature.parseClassTypeSignature;
import static aQute.bnd.signatures.TypeVariableSignature.parseTypeVariableSignature;

import java.util.Set;

import aQute.lib.stringrover.StringRover;

/**
 * See <a href=
 * "https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1">JVMS
 * 4.7.9.1. Signatures</a> for the specification of signatures.
 */
class Signatures {
	private Signatures() {}

	static final JavaTypeSignature[] EMPTY_JavaTypeSignature = new JavaTypeSignature[0];

	static JavaTypeSignature parseJavaTypeSignature(StringRover signature) {
		return switch (signature.charAt(0)) {
			case 'B' -> {
				signature.increment();
				yield BaseType.B;
			}
			case 'C' -> {
				signature.increment();
				yield BaseType.C;
			}
			case 'D' -> {
				signature.increment();
				yield BaseType.D;
			}
			case 'F' -> {
				signature.increment();
				yield BaseType.F;
			}
			case 'I' -> {
				signature.increment();
				yield BaseType.I;
			}
			case 'J' -> {
				signature.increment();
				yield BaseType.J;
			}
			case 'S' -> {
				signature.increment();
				yield BaseType.S;
			}
			case 'Z' -> {
				signature.increment();
				yield BaseType.Z;
			}
			default -> parseReferenceTypeSignature(signature);
		};
	}

	static final ReferenceTypeSignature[] EMPTY_ReferenceTypeSignature = new ReferenceTypeSignature[0];

	static ReferenceTypeSignature parseReferenceTypeSignature(StringRover signature) {
		return switch (signature.charAt(0)) {
			case 'T' -> parseTypeVariableSignature(signature);
			case 'L' -> parseClassTypeSignature(signature);
			case '[' -> parseArrayTypeSignature(signature);
			default -> throw new IllegalArgumentException("invalid signature: " + signature);
		};
	}

	static void erasedBinaryReferences(JavaTypeSignature sig, Set<String> references) {
		while (sig instanceof ArrayTypeSignature type) {
			sig = type.component;
		}
		if (sig instanceof ClassTypeSignature type) {
			references.add(type.binary);
			TypeArgument.erasedBinaryReferences(type.classType.typeArguments, references);
			for (SimpleClassTypeSignature innerType : type.innerTypes) {
				TypeArgument.erasedBinaryReferences(innerType.typeArguments, references);
			}
		}
	}

	static boolean isEmpty(Object[] array) {
		return (array == null) || (array.length == 0);
	}

	static String intern(String s) {
		return s.intern();
	}
}
