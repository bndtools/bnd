package aQute.bnd.signatures;

import java.util.Set;

public interface Signature {
	Set<String> erasedBinaryReferences();
}
