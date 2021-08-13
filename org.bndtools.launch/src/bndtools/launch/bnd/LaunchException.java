package bndtools.launch.bnd;

class LaunchException extends Exception {
	private static final long	serialVersionUID	= 1L;
	private final int			err;

	LaunchException(String message, int err) {
		super(message);
		this.err = err;
	}

	int getErr() {
		return err;
	}

}
