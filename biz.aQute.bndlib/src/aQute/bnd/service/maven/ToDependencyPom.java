package aQute.bnd.service.maven;

import java.io.OutputStream;

public interface ToDependencyPom {

	/**
	 * Create a pom with the repository as dependencies
	 *
	 * @param out where it should be stored
	 * @param options te options
	 */
	void toPom(OutputStream out, PomOptions options) throws Exception;
}
