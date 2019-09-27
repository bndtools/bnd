package org.bndtools.api;

import aQute.bnd.build.Project;

public interface ModelListener {
	void modelChanged(Project model) throws Exception;
}
