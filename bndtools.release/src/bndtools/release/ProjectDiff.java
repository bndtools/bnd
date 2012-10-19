/*******************************************************************************
 * Copyright (c) 2012 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import java.util.List;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline;

public class ProjectDiff {
	
	private final Project project;
	private final List<Baseline> compare;
	private boolean release;
	private String releaseRepository;
	private String defaultReleaseRepository;
	
	public ProjectDiff(Project project, List<Baseline> compare) {
		this.project = project;
		this.compare = compare;
		this.release = true;
	}

	public boolean isRelease() {
		return release;
	}

	public void setRelease(boolean release) {
		this.release = release;
	}

	public Project getProject() {
		return project;
	}

	public List<Baseline> getBaselines() {
		return compare;
	}

	public String getReleaseRepository() {
		return releaseRepository;
	}

	public String getDefaultReleaseRepository() {
		return defaultReleaseRepository;
	}

	public void setDefaultReleaseRepository(String defaultReleaseRepository) {
		this.defaultReleaseRepository = defaultReleaseRepository;
	}

	public void setReleaseRepository(String releaseRepository) {
		this.releaseRepository = releaseRepository;
	}
}
