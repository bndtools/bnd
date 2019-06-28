package org.bndtools.api;

import aQute.bnd.build.Run;

import org.eclipse.core.resources.IResource;

public interface RunProvider {

	Run create(IResource targetResource, RunMode mode) throws Exception;

}
