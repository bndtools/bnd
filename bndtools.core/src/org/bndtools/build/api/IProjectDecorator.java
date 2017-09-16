package org.bndtools.build.api;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IProject;

import aQute.bnd.osgi.Packages;

public interface IProjectDecorator {

    void updateDecoration(IProject project, BndProjectInfo info) throws Exception;

    static interface BndProjectInfo {
        Collection<File> getSourcePath() throws Exception;

        public Packages getExports();

        public Packages getImports();

        public Packages getContained();
    }

}
