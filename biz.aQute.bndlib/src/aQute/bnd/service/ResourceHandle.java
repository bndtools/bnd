package aQute.bnd.service;

import java.io.File;
import java.io.IOException;

public interface ResourceHandle {

	public enum Location {
		local,
		remote_cached,
		remote
	}

	String getName();

	Location getLocation();

	File request() throws IOException, Exception;
}
