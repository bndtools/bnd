package aQute.maven.api;

import java.util.Map;

import aQute.bnd.util.dto.DTO;

public interface IPom {
	class Dependency extends DTO {
		public boolean		optional;
		public Program		program;
		public String		version;
		public String		systemPath;
		public MavenScope	scope;
		public String		error;
		public String		type;
		public String		classifier;

		public Archive getArchive() {
			Revision revision = getRevision();
			return revision.archive(type, classifier);
		}

		public Revision getRevision() {
			if (version == null)
				return null;
			return program.version(version);
		}
	}

	Revision getRevision();

	IPom getParent();

	String getPackaging();

	Archive binaryArchive();

	Map<Program,Dependency> getDependencies(MavenScope scope, boolean transitive) throws Exception;
}
