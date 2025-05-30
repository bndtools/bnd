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
import aQute.lib.io.IO;
import aQute.libg.command.Command;

public class JUnitLauncher extends ProjectLauncher {
	private final static Logger	logger	= LoggerFactory.getLogger(JUnitLauncher.class);
	boolean						junit4Main;
	private Classpath			cp;
	private Command				java;
	private List<String>		fqns	= new ArrayList<>();

	public JUnitLauncher(Project project) throws Exception {
		super(project);
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();
		Pattern tests = Pattern.compile(getProject().getProperty(Constants.TESTSOURCES, "(.*).java"));

		String testDirName = getProject().getProperty("testsrc", "test");
		File testSrc = getProject().getFile(testDirName)
			.getAbsoluteFile();
		if (!testSrc.isDirectory()) {
			logger.debug("no test src directory");
			return;
		}

		if (!traverse(fqns, testSrc, "", tests)) {
			logger.debug("no test files found in {}", testSrc);
			return;
		}

		cp = new Classpath(getProject(), "junit");
		addClasspath(getProject().getTestpath());
		File output = getProject().getOutput();
		if (output.exists()) {
			addClasspath(new Container(getProject(), output));
		}
		addClasspath(getProject().getBuildpath());
	}

	@Override
	public Command getCommand() throws Exception {
		if (java != null) {
			return java;
		}
		java = new Command();
		java.add(getJavaExecutable("java"));

		java.add("-cp");
		java.add(cp.toString());
		java.addAll(getProject().getRunVM());
		java.add(getMainTypeName());
		java.addAll(fqns);
		if (getTimeout() != 0L)
			java.setTimeout(getTimeout() + 1000L, TimeUnit.MILLISECONDS);

		return java;
	}

	private boolean traverse(List<String> fqns, File testSrc, String prefix, Pattern filter) {
		boolean added = false;

		if (testSrc.isDirectory()) {
			for (File sub : IO.listFiles(testSrc)) {
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
}
