package aQute.libg.shacache;

import java.io.*;

public interface ShaSource {
	boolean isFast();

	InputStream get(String sha) throws Exception;
}
