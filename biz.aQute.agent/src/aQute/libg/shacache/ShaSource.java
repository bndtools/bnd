package aQute.libg.shacache;

import java.io.InputStream;

public interface ShaSource {
	boolean isFast();
	InputStream get(String sha) throws Exception;
}
