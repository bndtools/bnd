package aQute.bnd.service;

public interface OrderedPlugin {
	/**
	 * For sorting plugins before calling.
	 * <p>
	 * Lower values are called before higher values.
	 *
	 * @return A numerical value. The default is 0.
	 */
	default int ordering() {
		return 0;
	}

}
