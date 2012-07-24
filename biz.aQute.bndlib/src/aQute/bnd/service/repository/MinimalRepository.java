package aQute.bnd.service.repository;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import aQute.bnd.version.*;
import aQute.service.reporter.*;

public interface MinimalRepository {
	public enum Gestalt {
		ADD, REMOTE
	};

	Report add(File f) throws Exception;

	Iterable<String> list(String globbing);

	List<Version> versions(String bsn);

	Future<File> get(String bsn, Version version, Map<String,String> attrs);
	
	boolean is(Gestalt gestalt);
}
