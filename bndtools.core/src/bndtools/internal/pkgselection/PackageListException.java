package bndtools.internal.pkgselection;

public class PackageListException extends Exception {

	private static final long serialVersionUID = 1L;

	public PackageListException(String message) {
		super(message);
	}

	public PackageListException(Throwable cause) {
		super(cause);
	}

	public PackageListException(String message, Throwable cause) {
		super(message, cause);
	}

}
