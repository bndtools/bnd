package aQute.bnd.runtime.api;

import java.io.Closeable;

public interface SnapshotProvider extends Closeable {
	Object getSnapshot() throws Exception;
}
