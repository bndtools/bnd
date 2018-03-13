package aQute.bnd.compatibility;

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
		return super.toString().toLowerCase();
	}
}
