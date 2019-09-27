package aQute.maven.api;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.osgi.util.promise.Promise;

import aQute.maven.provider.MavenBackingRepository;

/**
 * This is an abstraction of a Maven repository. It is a repository backed by a
 * local directory (usually ~/.m2/repository) and a remote release and/or
 * snapshot repository that is accessed over http(s).
 * <p>
 * The repository stores <em>archives</em> organized in <em>revisions</em>.
 * Revisions are organized in <em>programs</em>. Archives are the files created
 * per revision, they are distinguished by <em>extension</em> and
 * <em>classifier</em>.
 * <p>
 * Maven maintains the remote snapshots with date stamped archives. (The
 * SNAPSHOT is replaced with a date stamp + build number). This repository can
 * resolve the latest archive and provide a list of date stamped archives.
 */
public interface IMavenRepo extends Closeable {
	/**
	 * The format for an archive is:
	 *
	 * <pre>
	 * 		group ':' artifact ( ':' extension ( ':' classifier )? )? ':' version ( '-SNAPSHOT' )?
	 * </pre>
	 */
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
	 * Get the archive from the repository. Syntax:
	 *
	 * <pre>
	 *  s = group ':' artifact (':' extension ( ':' classifier )?)? ':' version
	 * </pre>
	 *
	 * @param s the archive specified as string
	 */
	Archive getArchive(String s) throws Exception;

	/**
	 * Get the revisions that belong to a program. This uses the
	 * {@code maven-metadata-xml} file in the program directory on the remote
	 * repository (release + snapshot). For a local only repository this must
	 * return an empty list.
	 *
	 * @param program the program to list the revisions for
	 * @return a list of revisions in the repository
	 */
	List<Revision> getRevisions(Program program) throws Exception;

	/**
	 * List the snapshot archives for a given revision. If the revision is not a
	 * snapshot, the list will be empty
	 *
	 * @param revision the revisions to list from
	 * @return the list snapshot archives
	 */
	List<Archive> getSnapshotArchives(Revision revision) throws Exception;

	/**
	 * For a given snapshot revision, get the latest resolved archive.
	 *
	 * @param revision the revision (MUST be snapshot)
	 * @param extension the extension of the archive
	 * @param classifier the classifier of the archive ( may be null)
	 * @return the archive as resolved against the remote maven-metadata.xml for
	 *         the revision
	 */
	Archive getResolvedArchive(Revision revision, String extension, String classifier) throws Exception;

	/**
	 * Get the file in the repositories local cache directory.
	 *
	 * @param archive The archive to fetch
	 * @return the file or null if not found
	 */
	Promise<File> get(Archive archive) throws Exception;

	/**
	 * Get the last updated time for a snapshot revision.
	 *
	 * @param revision the snapshot revision to get the time from
	 * @return the time of the snapshot revision remote update
	 */
	long getLastUpdated(Revision revision) throws Exception;

	/**
	 * Create a release object to release the revision.
	 *
	 * @param revision the revision to release
	 * @return the Release object
	 */
	Release release(Revision revision, Properties context) throws Exception;

	/**
	 * Take a generic snapshot archive and resolve it to the latest released
	 * snapshot.
	 *
	 * @param archive the archive to resolve
	 * @return the archive or null if not found
	 */
	Archive resolveSnapshot(Archive archive) throws Exception;

	/**
	 * Get the file object for the archive. The file does not have to exist
	 *
	 * @param archive the archive to find the file for
	 * @return the File or null if not found
	 */
	File toLocalFile(Archive archive);

	/**
	 * Get the URI to the remote archive
	 *
	 * @param archive the archive to calculate the URI for
	 * @return the URI for the given archive
	 */
	URI toRemoteURI(Archive archive) throws Exception;

	/**
	 * Refresh the repository against the file system and remote repositories.
	 * Return true if there was a change
	 */
	boolean refresh() throws Exception;

	/**
	 * Get the name of this repository
	 *
	 * @return the name
	 */
	String getName();

	IPom getPom(InputStream pomFile) throws Exception;

	IPom getPom(Revision revision) throws Exception;

	List<MavenBackingRepository> getSnapshotRepositories();

	List<MavenBackingRepository> getReleaseRepositories();

	boolean isLocalOnly();

	boolean exists(Archive binaryArchive) throws Exception;

}
