package aQute.junit;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public interface TestReporter extends TestListener {
    void setup(Bundle framework, Bundle targetBundle);
	void begin( List<Test> tests, int realcount);

	void aborted();

	void end();

}
