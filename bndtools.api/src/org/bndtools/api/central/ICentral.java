package org.bndtools.api.central;

import org.bndtools.api.ModelListener;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.build.Project;

@ProviderType
public interface ICentral {

	Project getModel(IJavaProject project);

	void changed(Project model);

	void addModelListener(ModelListener m);

	void removeModelListener(ModelListener m);

	void close();

}
