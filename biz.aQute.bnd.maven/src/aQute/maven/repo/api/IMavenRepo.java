package aQute.maven.repo.api;

import java.io.File;
import java.util.List;

public interface IMavenRepo {

	List<Revision> getRevisions(Program program) throws Exception;

	List<Archive> getSnapshotArchives(Revision revision) throws Exception;
	
	Archive getResolvedArchive(Revision revision, String extension, String classifier) throws Exception;

	File get(Archive archive) throws Exception;
	
	long getLastUpdated(Revision revision) throws Exception;

	Release release(Revision revision) throws Exception;

	Archive resolveSnapshot(Archive archive) throws Exception;
}
