package aQute.bnd.service.verifier;

import aQute.bnd.osgi.Analyzer;

public interface VerifierPlugin {

	/**
	 * This plugin is called after the manifest generation and after the jar is
	 * fully populated with resources. The plugin is intended to verify details
	 * of the jar and manifest.
	 *
	 * @param analyzer
	 * @throws Exception
	 */
	void verify(Analyzer analyzer) throws Exception;

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
