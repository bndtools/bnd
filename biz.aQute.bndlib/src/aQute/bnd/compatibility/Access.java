package aQute.bnd.compatibility;

import java.lang.reflect.Modifier;

/**
 * Access modifier
 */
public enum Access {
	PUBLIC,
	PROTECTED,
	PACKAGE,
	PRIVATE,
	UNKNOWN;

	public static Access modifier(int mod) {
		if (Modifier.isPublic(mod))
			return PUBLIC;
		if (Modifier.isProtected(mod))
			return PROTECTED;
		if (Modifier.isPrivate(mod))
			return PRIVATE;

		return PACKAGE;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
