package aQute.tester.plugin;

import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.junit.constants.*;

public class ProjectTesterImpl extends ProjectTester implements TesterConstants, EclipseJUnitTester {
	int					port	= -1;
	String				host;
	Project				project;
	boolean				prepared;
	private Container	me;

	public ProjectTesterImpl(Project project, Container me) throws Exception {
		super(project);
		this.project = project;
		this.me = me;
	}

	@Override
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
			launcher.getRunProperties().put(TESTER_UNRESOLVED, project.getProperty(Constants.TESTUNRESOLVED, "true"));

			launcher.getRunProperties().put(TESTER_DIR, getReportDir().getAbsolutePath());
			launcher.getRunProperties().put(TESTER_CONTINUOUS, "" + getContinuous());
			if (Processor.isTrue(project.getProperty(Constants.RUNTRACE)))
				launcher.getRunProperties().put(TESTER_TRACE, "true");

			Collection<String> testnames = getTests();
			if (testnames.size() > 0) {
				launcher.getRunProperties().put(TESTER_NAMES, Processor.join(testnames));
			}

			//
			// We used to add this bundle to the -runpath. However, now we add
			// it
			// ad the add the end of the -runbundles
			//

			launcher.addRunBundle(me.getFile().getAbsolutePath());
			launcher.prepare();
		}
		return true;
	}

	@Override
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
