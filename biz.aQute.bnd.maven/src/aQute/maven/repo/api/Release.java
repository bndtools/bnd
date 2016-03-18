package aQute.maven.repo.api;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;

public interface Release extends Closeable {

	void abort();

	void add(Archive archive, InputStream in) throws Exception;

	void add(Archive archive, File in) throws Exception;

	void add(String extension, String classifier, InputStream in) throws Exception;

	void setBuild(long timestamp, String build);

	void setBuild(String timestamp, String build);

	void setLocalOnly();
}
