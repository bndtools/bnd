package org.bndtools.build.api;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IProject;

import aQute.bnd.osgi.Packages;

public interface IProjectDecorator {

	void updateDecoration(IProject project, BndProjectInfo info) throws Exception;

	interface BndProjectInfo {
		Collection<File> getSourcePath() throws Exception;

		Packages getExports();

		Packages getImports();

		Packages getContained();
	}

}
