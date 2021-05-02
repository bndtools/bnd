package aQute.bnd.signatures;

import static aQute.bnd.signatures.Signatures.parseJavaTypeSignature;

import aQute.lib.stringrover.StringRover;

public interface JavaTypeSignature extends Result {
	@SuppressWarnings("unchecked")
	static <T extends JavaTypeSignature> T of(String signature) {
		return (T) parseJavaTypeSignature(new StringRover(signature));
	}
}
