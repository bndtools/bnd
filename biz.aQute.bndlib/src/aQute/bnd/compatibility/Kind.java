package aQute.bnd.compatibility;

import java.util.Locale;

/**
 * The kind of thing we scope
 */
public enum Kind {
	ROOT,
	CLASS,
	FIELD,
	CONSTRUCTOR,
	METHOD,
	UNKNOWN;

	@Override
	public String toString() {
		return super.toString().toLowerCase(Locale.ROOT);
	}
}
