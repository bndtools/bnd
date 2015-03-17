package aQute.bnd.build;

import java.util.*;


public interface RunSession {

	String getName();

	int getJdbPort();

	void stdin(Appendable app);

	void stdout(Appendable app);

	void stdin(String input);

	int launch() throws Exception;

	void cancel();

	Map<String,String> getAttrs();
}
