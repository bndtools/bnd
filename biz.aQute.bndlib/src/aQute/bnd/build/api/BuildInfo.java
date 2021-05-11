package aQute.bnd.build.api;

import java.util.List;

import aQute.bnd.build.Project;

public interface BuildInfo {
	Project getProject();
	List<? extends ArtifactInfo> getArtifactInfos();
}
