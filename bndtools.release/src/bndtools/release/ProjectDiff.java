package bndtools.release;

import java.util.List;

import aQute.bnd.build.Project;
import bndtools.diff.JarDiff;

public class ProjectDiff {
	
	private final Project project;
	private final List<JarDiff> compare;
	private boolean release;
	private String releaseRepository;
	private String defaultReleaseRepository;
	
	public ProjectDiff(Project project, List<JarDiff> compare) {
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

	public List<JarDiff> getJarDiffs() {
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
