package aQute.junit;

import java.util.List;

import org.osgi.framework.Bundle;

import junit.framework.Test;
import junit.framework.TestListener;

public interface TestReporter extends TestListener {
	void setup(Bundle framework, Bundle targetBundle);

	void begin(List<Test> tests, int realcount);

	void aborted();

	void end();

}
