package org.bndtools.api;

import aQute.bnd.build.Run;

public interface RunListener {

	void create(Run run) throws Exception;

	void end(Run run) throws Exception;

}
