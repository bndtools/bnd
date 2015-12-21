package aQute.libg.log;

import java.util.List;

public interface Logger {
	void error(String s, Object... args);

	void warning(String s, Object... args);

	void progress(String s, Object... args);

	List<String> getWarnings();

	List<String> getErrors();

	List<String> getProgress();

	boolean isPedantic();
}
