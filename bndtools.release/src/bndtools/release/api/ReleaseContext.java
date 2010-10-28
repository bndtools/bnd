/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release.api;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.build.Project;
import aQute.bnd.service.RepositoryPlugin;
import aQute.libg.reporter.Reporter;
import bndtools.diff.JarDiff;

public class ReleaseContext {

	private Project project;
	private List<JarDiff> jarDiffs;
	private RepositoryPlugin repository;
	private IProgressMonitor progressMonitor;
	private Reporter reporter;

	public ReleaseContext(Project project, List<JarDiff> jarDiffs, RepositoryPlugin repository, IProgressMonitor progressMonitor, Reporter reporter) {
		this.project = project;
		this.jarDiffs = jarDiffs;
		this.repository = repository;
		this.progressMonitor = progressMonitor;
		this.reporter = reporter;
	}

	public Project getProject() {
		return project;
	}

	public List<JarDiff> getJarDiffs() {
		return jarDiffs;
	}

	public RepositoryPlugin getRepository() {
		return repository;
	}

	public IProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	public Reporter getReporter() {
		return reporter;
	}
	
}
