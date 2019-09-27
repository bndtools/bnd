package aQute.junit.plugin;

import static aQute.junit.constants.TesterConstants.TESTER_CONTINUOUS;
import static aQute.junit.constants.TesterConstants.TESTER_DIR;
import static aQute.junit.constants.TesterConstants.TESTER_HOST;
import static aQute.junit.constants.TesterConstants.TESTER_NAMES;
import static aQute.junit.constants.TesterConstants.TESTER_PORT;
import static aQute.junit.constants.TesterConstants.TESTER_TRACE;
import static aQute.junit.constants.TesterConstants.TESTER_UNRESOLVED;

import java.util.Collection;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.EclipseJUnitTester;

public class ProjectTesterImpl extends ProjectTester implements EclipseJUnitTester {
	int		port	= -1;
	String	host;
	boolean	prepared;

	public ProjectTesterImpl(Project project) throws Exception {
		super(project);
	}

	@Override
	public boolean prepare() throws Exception {
		if (!prepared) {
			prepared = true;
			super.prepare();
			ProjectLauncher launcher = getProjectLauncher();
			if (port > 0) {
				launcher.getRunProperties()
					.put(TESTER_PORT, "" + port);
				if (host != null)
					launcher.getRunProperties()
						.put(TESTER_HOST, "" + host);

			}
			launcher.getRunProperties()
				.put(TESTER_UNRESOLVED, getProject().getProperty(Constants.TESTUNRESOLVED, "true"));

			launcher.getRunProperties()
				.put(TESTER_DIR, getReportDir().getAbsolutePath());
			launcher.getRunProperties()
				.put(TESTER_CONTINUOUS, "" + getContinuous());
			if (getProject().is(Constants.RUNTRACE))
				launcher.getRunProperties()
					.put(TESTER_TRACE, "true");

			Collection<String> testnames = getTests();
			if (testnames.size() > 0) {
				launcher.getRunProperties()
					.put(TESTER_NAMES, Processor.join(testnames));
			}
			// This is only necessary because we might be picked
			// as default and that implies we're not on the -testpath
			launcher.addDefault(Constants.DEFAULT_TESTER_BSN);
			launcher.prepare();
		}
		return true;
	}

	@Override
	public int test() throws Exception {
		prepare();
		return getProjectLauncher().launch();
	}

	@Override
	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}

}
