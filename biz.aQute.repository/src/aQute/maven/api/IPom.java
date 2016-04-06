package aQute.maven.api;

import java.util.Map;

import org.osgi.dto.DTO;

public interface IPom {
	class Dependency extends DTO {
		public boolean		optional;
		public Archive		archive;
		public String		systemPath;
		public MavenScope	scope;
		public String		error;
	}

	Revision getRevision();

	IPom getParent();

	String getPackaging();

	Archive binaryArchive();

	Map<Program,Dependency> getDependencies(MavenScope scope, boolean transitive) throws Exception;
}
