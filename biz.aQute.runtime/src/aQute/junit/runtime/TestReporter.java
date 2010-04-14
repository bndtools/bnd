package aQute.junit.runtime;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public interface TestReporter extends TestListener {

    void begin(Bundle framework, Bundle targetBundle, List tests, int realcount);
    void aborted();
    void end();

}
