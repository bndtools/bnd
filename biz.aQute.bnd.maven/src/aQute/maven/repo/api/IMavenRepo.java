package aQute.maven.repo.api;

import java.io.Closeable;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import org.osgi.util.promise.Promise;

public interface IMavenRepo extends Closeable {
	Pattern ARCHIVE_P = Pattern.compile(//
										//
			"           (?<group>[^:]+)" //
					+ ":          (?<artifact>[^:]+)" //
					+ "(:"//
					+ "		(?<extension>[^:]+)"//
					+ "		(:"//
					+ "			(?<classifier>[^:]+)"//
					+ "		)?"//
					+ ")?"//
					+ ":"//
					+ "		(?<version>[^:]+)", //
			Pattern.COMMENTS);

	/**
	 * Syntax:
	 * 
	 * <pre>
	 *  s = group ':' artifact (':' extension ( ':' classifier )?)? ':' version
	 * </pre>
	 * 
	 * @param s
	 * @throws Exception
	 */
	Archive getArchive(String s) throws Exception;

	List<Revision> getRevisions(Program program) throws Exception;

	List<Archive> getSnapshotArchives(Revision revision) throws Exception;

	Archive getResolvedArchive(Revision revision, String extension, String classifier) throws Exception;

	Promise<File> get(Archive archive) throws Exception;

	long getLastUpdated(Revision revision) throws Exception;

	Release release(Revision revision) throws Exception;

	Archive resolveSnapshot(Archive archive) throws Exception;

	List<Program> getLocalPrograms() throws Exception;

	File toLocalFile(Archive archive);

	URI toRemoteURI(Archive archive) throws Exception;

}
