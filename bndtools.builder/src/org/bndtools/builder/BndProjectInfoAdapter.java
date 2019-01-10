package org.bndtools.builder;

import java.io.File;
import java.util.Collection;

import org.bndtools.build.api.IProjectDecorator.BndProjectInfo;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Packages;

public class BndProjectInfoAdapter implements BndProjectInfo {

    private final Project project;

    public BndProjectInfoAdapter(Project project) {
        this.project = project;
    }

    @Override
    public Collection<File> getSourcePath() throws Exception {
        return project.getSourcePath();
    }

    @Override
    public Packages getExports() {
        return project.getExports();
    }

    @Override
    public Packages getImports() {
        return project.getImports();
    }

    @Override
    public Packages getContained() {
        return project.getContained();
    }

}
