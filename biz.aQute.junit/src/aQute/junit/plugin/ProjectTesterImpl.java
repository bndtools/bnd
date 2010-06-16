package aQute.junit.plugin;

import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.service.*;
import aQute.junit.constants.*;
import aQute.lib.osgi.*;

public class ProjectTesterImpl extends ProjectTester implements TesterConstants, EclipseJUnitTester {
	int					port	= -1;
	String				host;
	Project				project;
	String				report;
	boolean				prepared;

	public ProjectTesterImpl(Project project) throws Exception {
		super(project);
		this.project = project;
	}

	public boolean prepare() throws Exception {
		if (!prepared) {
			prepared = true;
			super.prepare();
			ProjectLauncher launcher = getProjectLauncher();
			if (port > 0) {
				launcher.getRunProperties().put(TESTER_PORT, "" + port);
				if (host != null)
					launcher.getRunProperties().put(TESTER_HOST, "" + host);

			}
			launcher.getRunProperties().put(TESTER_DIR, getReportDir().getAbsolutePath());
			launcher.getRunProperties().put(TESTER_CONTINUOUS, "" + getContinuous());
			if ( Processor.isTrue(project.getProperty(Constants.RUNTRACE)))
				launcher.getRunProperties().put(TESTER_TRACE, "true");

			Collection<String> testnames = getTests();
			if (testnames.size() > 0) {
				launcher.getRunProperties().put(TESTER_NAMES, Processor.join(testnames));
			}
			// This is only necessary because we might be picked
			// as default and that implies we're not on the -testpath
			launcher.addDefault(Constants.DEFAULT_TESTER_BSN);
			launcher.prepare();
		}
		return true;
	}

	public int test() throws Exception {
		prepare();
		return getProjectLauncher().launch();
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
