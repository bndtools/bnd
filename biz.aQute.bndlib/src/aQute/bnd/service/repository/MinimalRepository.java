package aQute.bnd.service.repository;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import aQute.bnd.version.Version;
import aQute.service.reporter.Report;

public interface MinimalRepository {
	public enum Gestalt {
		ADD,
		REMOTE
	}

	Report add(File f) throws Exception;

	Iterable<String> list(String globbing);

	List<Version> versions(String bsn);

	Future<File> get(String bsn, Version version, Map<String, String> attrs);

	boolean is(Gestalt gestalt);
}
