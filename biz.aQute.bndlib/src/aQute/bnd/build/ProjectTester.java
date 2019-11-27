package aQute.bnd.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import aQute.bnd.build.ProjectLauncher.NotificationListener;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;
import aQute.libg.qtokens.QuotedTokenizer;

public abstract class ProjectTester {
	private final Project			project;
	private final ProjectLauncher	launcher;
	private final List<String>		tests		= new ArrayList<>();
	private File					reportDir;
	private boolean					continuous	= true;

	public ProjectTester(Project project) throws Exception {
		this.project = project;
		launcher = project.getProjectLauncher();
		launcher.addRunVM("-ea");
		continuous = project.is(Constants.TESTCONTINUOUS);

		reportDir = new File(project.getTarget(), project.getProperty("test-reports", "test-reports"));
	}

	public ProjectLauncher getProjectLauncher() {
		return launcher;
	}

	public void addTest(String test) {
		if ((test == null) || (test = test.trim()).isEmpty()) {
			return;
		}
		test = new QuotedTokenizer(test, "", true).nextToken();
		if ((test == null) || (test = test.trim()).isEmpty()) {
			return;
		}
		tests.add(test);
	}

	public Collection<String> getTests() {
		return tests;
	}

	public Collection<File> getReports() {
		List<File> reports = new ArrayList<>();
		for (File report : reportDir.listFiles()) {
			if (report.isFile())
				reports.add(report);
		}
		return reports;
	}

	public File getReportDir() {
		return reportDir;
	}

	public void setReportDir(File reportDir) {
		this.reportDir = reportDir;
	}

	public Project getProject() {
		return project;
	}

	public boolean getContinuous() {
		return continuous;
	}

	public void setContinuous(boolean b) {
		this.continuous = b;
	}

	public File getCwd() {
		return launcher.getCwd();
	}

	public void setCwd(File dir) {
		launcher.setCwd(dir);
	}

	public boolean prepare() throws Exception {
		IO.mkdirs(reportDir);
		return true;
	}

	public abstract int test() throws Exception;

	public void registerForNotifications(NotificationListener listener) {
		getProjectLauncher().registerForNotifications(listener);
	}
}
