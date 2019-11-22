package aQute.bnd.build;

import static java.lang.invoke.MethodType.methodType;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

import aQute.bnd.osgi.Processor;
import aQute.lib.exceptions.Exceptions;

public class Run extends Project {

	/**
	 * So far we have not included the resolver code inside bndlib. This means
	 * that anybody inside bndlib cannot rely on the resolver code present.
	 * However, we get more functions that take advantage of this tool. The
	 * following code looks if the resolving counterpart of Run is present in
	 * the classloader. Since this is a 1:1 replacement for this class, we try
	 * to load the resolving Bndrun.
	 */
	private final static MethodHandle createBndrun;

	static {
		MethodHandles.Lookup lookup = MethodHandles.publicLookup();
		MethodHandle handle = null;
		try {
			Class<?> clazz = Run.class.getClassLoader()
				.loadClass("biz.aQute.resolve.Bndrun");
			MethodType type = methodType(clazz, Workspace.class, File.class);
			handle = lookup.findStatic(clazz, "createBndrun", type);
		} catch (Throwable e) {
			handle = null;
		}
		createBndrun = handle;
	}

	/**
	 * Create a Run that will be stand alone if it contains -standalone. In that
	 * case the given workspace is ignored. Otherwise, the workspace must be a
	 * valid workspace.
	 */
	public static Run createRun(Workspace workspace, File file) throws Exception {
		if (createBndrun == null)
			return createRun0(workspace, file);

		try {
			return (Run) createBndrun.invoke(workspace, file);
		} catch (Throwable e) {
			throw Exceptions.duck(e);
		}
	}

	private static Run createRun0(Workspace workspace, File file) throws Exception {
		Processor processor;
		if (workspace != null) {
			Run run = new Run(workspace, file);
			if (run.getProperties()
				.get(STANDALONE) == null) {
				return run;
			}
			// -standalone specified
			processor = run;
		} else {
			processor = new Processor();
			processor.setProperties(file);
		}

		Workspace standaloneWorkspace = Workspace.createStandaloneWorkspace(processor, file.toURI());
		Run run = new Run(standaloneWorkspace, file);
		return run;
	}

	public Run(Workspace workspace, File projectDir, File propertiesFile) throws Exception {
		super(workspace, projectDir, propertiesFile);
	}

	public Run(Workspace workspace, File propertiesFile) throws Exception {
		super(workspace, propertiesFile == null ? null : propertiesFile.getParentFile(), propertiesFile);
	}

	@Override
	public void report(Map<String, Object> table) throws Exception {
		super.report(table, false);
	}

	@Override
	public String getName() {
		return getPropertiesFile().getName();
	}
}
