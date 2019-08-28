package bndtools.release;

import java.util.List;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline;

public class ProjectDiff {

	private final Project			project;
	private final List<Baseline>	compare;
	private boolean					release;
	private String					releaseRepository;
	private String					defaultReleaseRepository;
	private boolean					versionUpdateRequired;
	private boolean					releaseRequired;

	public ProjectDiff(Project project, List<Baseline> compare) {
		this.project = project;
		this.compare = compare;
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

	public boolean isVersionUpdateRequired() {
		return versionUpdateRequired;
	}

	public void setVersionUpdateRequired(boolean versionUpdateRequired) {
		this.versionUpdateRequired = versionUpdateRequired;
	}

	public boolean isReleaseRequired() {
		return releaseRequired;
	}

	public void setReleaseRequired(boolean releaseRequired) {
		this.releaseRequired = releaseRequired;
	}
}
