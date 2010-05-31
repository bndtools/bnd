package aQute.junit;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public interface TestReporter extends TestListener {

	void begin(Bundle framework, Bundle targetBundle, List<Test> tests, int realcount);

	void aborted();

	void end();

}
