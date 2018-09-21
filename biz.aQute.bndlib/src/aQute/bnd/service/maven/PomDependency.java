package aQute.bnd.service.maven;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;

public class PomDependency extends DTO {
	public PomRevision	revision;
	public String		classifier;
	public String		extension;
	public boolean		optional	= false;

	public static class PomRevision extends DTO {
		public String		group;
		public String		artifact;
		public MavenVersion	version;
	}
}
