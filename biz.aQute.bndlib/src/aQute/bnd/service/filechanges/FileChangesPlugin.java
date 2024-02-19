package aQute.bnd.service.filechanges;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.util.promise.Promise;

/**
 * A bnd plugin interface that is registered by the driver if the driver has a
 * file system that needs refreshing when you update the real file system.
 * Basically like Eclipse IResource. This abstracts that behavior.
 */
public interface FileChangesPlugin {
	/**
	 * A marker.
	 *
	 * @param what ERROR, WARNING, INFO, etc. (not an enum because it is wide
	 *            open range with custom types
	 * @param id error/warning unique id
	 * @param message the text, can be multiline
	 * @param exceptionOrNull optional throwable
	 */
	record Marker(String what, String id, String message, @Nullable
	Throwable exceptionOrNull, int start, int end, int line) {}

	/**
	 * Signal the change of a file. This includes removal and addition. The
	 * files or directories must be synchronized. The actual refresh must happen
	 * fast or in another thread.
	 * <p>
	 * A map with markers is returned, if a file had errors during the
	 * refreshing, also compile errors etc, then it will have an entry with a
	 * description of these errors.
	 *
	 * @param files the files to synchronize
	 * @return a promise of a map that lists errors during the update of the
	 *         file
	 */
	Promise<Map<File, List<Marker>>> refresh(File... files);
}
