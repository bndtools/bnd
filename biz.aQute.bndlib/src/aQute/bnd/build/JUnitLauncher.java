package aQute.bnd.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.libg.command.Command;

public class JUnitLauncher extends ProjectLauncher {
	private final static Logger	logger	= LoggerFactory.getLogger(JUnitLauncher.class);
	boolean						junit4Main;
	final Project				project;
	private Classpath			cp;
	private Command				java;
	private long				timeout;
	// private boolean trace;
	private List<String>		fqns	= new ArrayList<>();

	public JUnitLauncher(Project project) throws Exception {
		super(project);
		this.project = project;
	}

	@Override
	public void prepare() throws Exception {
		Pattern tests = Pattern.compile(project.getProperty(Constants.TESTSOURCES, "(.*).java"));

		String testDirName = project.getProperty("testsrc", "test");
		File testSrc = project.getFile(testDirName)
			.getAbsoluteFile();
		if (!testSrc.isDirectory()) {
			logger.debug("no test src directory");
			return;
		}

		if (!traverse(fqns, testSrc, "", tests)) {
			logger.debug("no test files found in {}", testSrc);
			return;
		}

		timeout = Processor.getDuration(project.getProperty(Constants.RUNTIMEOUT), 0);
		// trace = Processor.isTrue(project.getProperty(Constants.RUNTRACE));
		cp = new Classpath(project, "junit");
		addClasspath(project.getTestpath());
		File output = project.getOutput();
		if (output.exists()) {
			addClasspath(new Container(project, output));
		}
		addClasspath(project.getBuildpath());
	}

	@Override
	public int launch() throws Exception {
		java = new Command();
		java.add(project.getProperty("java", "java"));

		java.add("-cp");
		java.add(cp.toString());
		java.addAll(project.getRunVM());
		java.add(getMainTypeName());
		java.addAll(fqns);
		if (timeout != 0)
			java.setTimeout(timeout + 1000, TimeUnit.MILLISECONDS);

		logger.debug("cmd line {}", java);
		try {
			int result = java.execute(System.in, System.err, System.err);
			if (result == Integer.MIN_VALUE)
				return TIMEDOUT;
			reportResult(result);
			return result;
		} finally {
			cleanup();
		}

	}

	private boolean traverse(List<String> fqns, File testSrc, String prefix, Pattern filter) {
		boolean added = false;

		if (testSrc.isDirectory()) {
			for (File sub : testSrc.listFiles()) {
				return traverse(fqns, sub, prefix + sub.getName() + ".", filter) || added;
			}
		} else if (testSrc.isFile()) {
			String name = testSrc.getName();
			Matcher m = filter.matcher(name);
			if (m.matches()) {
				fqns.add(m.group(1));
				added = true;
			}
		}
		return added;
	}

	@Override
	public String getMainTypeName() {
		return "aQute.junit.Activator";
	}

	@Override
	public void update() throws Exception {
		// TODO Auto-generated method stub

	}
}
