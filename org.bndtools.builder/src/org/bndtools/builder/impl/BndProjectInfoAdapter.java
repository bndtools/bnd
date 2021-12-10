package org.bndtools.builder.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.bndtools.api.builder.IProjectDecorator.BndProjectInfo;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Packages;

public class BndProjectInfoAdapter implements BndProjectInfo {

	final Collection<File>	sourcePath;
	final Packages			exports;
	final Packages			imports;
	final Packages			contained;

	public BndProjectInfoAdapter(Project project) throws Exception {
		sourcePath = new ArrayList<>(project.getSourcePath());
		exports = project.getExports()
			.dup();
		imports = project.getImports()
			.dup();
		contained = project.getContained()
			.dup();
	}

	@Override
	public Collection<File> getSourcePath() throws Exception {
		return sourcePath;
	}

	@Override
	public Packages getExports() {
		return exports;
	}

	@Override
	public Packages getImports() {
		return imports;
	}

	@Override
	public Packages getContained() {
		return contained;
	}

}
