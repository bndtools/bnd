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
		switch (signature.charAt(0)) {
			case 'B' :
				signature.increment();
				return BaseType.B;
			case 'C' :
				signature.increment();
				return BaseType.C;
			case 'D' :
				signature.increment();
				return BaseType.D;
			case 'F' :
				signature.increment();
				return BaseType.F;
			case 'I' :
				signature.increment();
				return BaseType.I;
			case 'J' :
				signature.increment();
				return BaseType.J;
			case 'S' :
				signature.increment();
				return BaseType.S;
			case 'Z' :
				signature.increment();
				return BaseType.Z;
			default :
				return parseReferenceTypeSignature(signature);
		}
	}

	static final ReferenceTypeSignature[] EMPTY_ReferenceTypeSignature = new ReferenceTypeSignature[0];

	static ReferenceTypeSignature parseReferenceTypeSignature(StringRover signature) {
		switch (signature.charAt(0)) {
			case 'T' :
				return parseTypeVariableSignature(signature);
			case 'L' :
				return parseClassTypeSignature(signature);
			case '[' :
				return parseArrayTypeSignature(signature);
			default :
				throw new IllegalArgumentException("invalid signature: " + signature);
		}
	}

	static void erasedBinaryReferences(JavaTypeSignature sig, Set<String> references) {
		while (sig instanceof ArrayTypeSignature) {
			sig = ((ArrayTypeSignature) sig).component;
		}
		if (sig instanceof ClassTypeSignature) {
			ClassTypeSignature type = (ClassTypeSignature) sig;
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
