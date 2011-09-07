package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;
import aQute.lib.osgi.*;
import aQute.lib.osgi.eclipse.*;
import aQute.libg.qtokens.*;

/**
 * Example usage
 * <pre>
 * <project name="test path with bnd" default="run-test" basedir=".">
 *    <property file="run-demo.properties"/>
 *    <target name="run-test" description="show bnd usage with classpathref">
 *      <path id="run.demo.id" >
 *        <pathelement location="demo/classes"/>
 *        <fileset dir="${libs.demo.dir}">
 *          <include name="*.jar"/>
 *        </fileset>
 *      </path>
 *      <path id="bnd.path.id" >
 *        <fileset dir="dist">
 *          <include name="*.jar"/>
 *        </fileset>
 *      </path>
 *      <path id="descriptors.id" >
 *        <fileset dir="demo/bnd">
 *          <include name="*.bnd"/>
 *        </fileset>
 *      </path>
 * 
 *      <taskdef classpathref="bnd.path.id" classname="aQute.bnd.ant.BndTask" name="bnd"/>
 *      <bnd classpathref="run.demo.id"  eclipse="false" failok="false" exceptions="true" 
 *               output="demo/generated"  bndFiles="descriptors.id"/>
 * 
 * <!-- sample usage with nested paths -->
 * 
 * 	    <bnd eclipse="false" failok="false" exceptions="true" output="demo/generated">
 * 		  <classpath>
 * 		    <pathelement location="demo/classes"/>
 * 		      <fileset dir="${libs.demo.dir}">
 * 			    <include name="*.jar"/>
 *      	  </fileset>	 	
 * 		  </classpath>
 * 		  <bndfiles>
 * 		    <fileset dir="demo/bnd">
 * 		      <include name="*.bnd"/>
 * 		    </fileset>				
 * 		  <bndfiles>
 *      </bnd> 
 *    </target>
 *  </project>
 * </pre>
 * 
 */
public class BndTask extends BaseTask {
	String			command;
	File			basedir;

	boolean			failok;
	boolean			exceptions;
	boolean			print;

	// flags aiming to know how classpath & bnd descriptors were set
	private boolean	classpathDirectlySet;
	private Path	classpathReference;
	private Path	bndfilePath;

	public void execute() throws BuildException {
		// JME add - ensure every required parameter is present
		// handle cases where mutual exclusion live..
		// this is the ANT tradition ..
		validate();
		updateClasspath();
		updateBndFiles();

		if (command == null) {
			executeBackwardCompatible();
			return;

		}

		if (basedir == null)
			throw new BuildException("No basedir set");

		try {
			Project project = Workspace.getProject(basedir);
			project.setProperty("in.ant", "true");
			project.setProperty("environment", "ant");
			project.setExceptions(true);
			project.setTrace(trace);
			project.setPedantic(pedantic);

			project.action(command);

			if (report(project))
				throw new BuildException("Command " + command + " failed");
		} catch (Throwable e) {
			if (exceptions)
				e.printStackTrace();
			throw new BuildException(e);
		}

	}

	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * Set the base directory of the project. This property MUST be set.
	 * 
	 * @param basedir
	 */
	public void setBasedir(File basedir) {
		this.basedir = basedir;
	}

	// Old shit

	List<File>	files		= new ArrayList<File>();
	List<File>	classpath	= new ArrayList<File>();
	List<File>	sourcepath	= new ArrayList<File>();
	File		output		= null;
	File		testDir		= null;
	boolean		eclipse;
	boolean		inherit		= true;

	private void executeBackwardCompatible() throws BuildException {
		try {
			if (files == null)
				throw new BuildException("No files set");

			if (eclipse) {
				File project = getProject().getBaseDir();
				EclipseClasspath cp = new EclipseClasspath(this, project.getParentFile(), project);
				classpath.addAll(cp.getClasspath());
				classpath.addAll(cp.getBootclasspath());
				sourcepath.addAll(cp.getSourcepath());
				// classpath.add(cp.getOutput());
				if (report())
					throw new BuildException("Errors during Eclipse Path inspection");

			}

			if (output == null)
				output = getProject().getBaseDir();

			for (Iterator<File> f = files.iterator(); f.hasNext();) {
				File file = (File) f.next();
				Builder builder = new Builder();

				builder.setPedantic(isPedantic());
				if (file.exists()) {
					// Do nice property calculations
					// merging includes etc.
					builder.setProperties(file);
				}

				// get them and merge them with the project
				// properties
				Properties projectProperties = new Properties();
				if (inherit)
				        projectProperties.putAll((Map<?, ?>) getProject().getProperties());
				projectProperties.putAll(builder.getProperties());
				builder.setProperties(projectProperties);
				builder.setClasspath(toFiles(classpath, "classpath"));
				builder.setSourcepath(toFiles(sourcepath, "sourcepath"));
				Jar jars[] = builder.builds();

				if (!failok && report() && report(builder)) {
					throw new BuildException("bnd failed", new Location(file.getAbsolutePath()));
				}

				for (int i = 0; i < jars.length; i++) {
					Jar jar = jars[i];
					String bsn = jar.getName();

					File base = file.getParentFile();
					File output = this.output;

					String path = builder.getProperty("-output");

					if (output == null) {
						if (path == null)
							output = getFile(base, bsn + ".jar");
						else {
							output = getFile(base, path);
						}
					} else if (output.isDirectory()) {
						if (path == null)
							output = getFile(this.output, bsn + ".jar");
						else
							output = getFile(this.output, path);
					} else if (output.isFile()) {
						if (files.size() > 1)
							error("Output is a file but there are multiple input files, these files will overwrite the output file: "
									+ output.getAbsolutePath());
					}

					String msg = "";
					if (!output.exists() || output.lastModified() <= jar.lastModified()) {
						jar.write(output);
					} else {
						msg = "(not modified)";
					}
					trace(jar.getName() + " (" + output.getName() + ") "
							+ jar.getResources().size() + " " + msg);
					report();
					jar.close();
				}
				builder.close();
			}
		} catch (Exception e) {
			// if (exceptions)
			e.printStackTrace();
			if (!failok)
				throw new BuildException("Failed to build jar file: ", e);
		}
	}

	public void setFiles(String files) {
		files = files.replaceAll("\\.jar(,|$)", ".bnd");
		addAll(this.files, files, ",");
	}

	void addAll(List<File> list, String files, String separator) {
		trace("addAll '%s' with %s", files, separator);
		QuotedTokenizer qt = new QuotedTokenizer(files, separator);
		String entries[] = qt.getTokens();
		File project = getProject().getBaseDir();
		for (int i = 0; i < entries.length; i++) {
			File f = getFile(project, entries[i]);
			if (f.exists())
				list.add(f);
			else
				error("Can not find bnd file to process: " + f.getAbsolutePath());
		}
	}

	public void setClasspath(String value) {
		Path p = (Path) getProject().getReference(value);
		if (p == null)
			addAll(classpath, value, File.pathSeparator + ",");
		else {
			String[] path = p.list();
			for (int i = 0; i < path.length; i++)
				classpath.add(new File(path[i]));
		}
		classpathDirectlySet = true;
	}

	public void setEclipse(boolean eclipse) {
		this.eclipse = eclipse;
	}

	boolean isFailok() {
		return failok;
	}

	public void setFailok(boolean failok) {
		this.failok = failok;
	}

	boolean isExceptions() {
		return exceptions;
	}

	public void setExceptions(boolean exceptions) {
		this.exceptions = exceptions;
	}

	boolean isPrint() {
		return print;
	}

	void setPrint(boolean print) {
		this.print = print;
	}

	public void setSourcepath(String sourcepath) {
		addAll(this.sourcepath, sourcepath, File.pathSeparator + ",");
	}

	static File[]	EMPTY_FILES	= new File[0];

	File[] toFiles(List<File> files, String what) throws IOException {
		return files.toArray(EMPTY_FILES);
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public void setDestFile(File output) {
		this.output = output;
	}

	public void setTestDir(File testDir) {
		this.testDir = testDir;
	}

	public void setInherit(boolean inherit) {
		this.inherit = inherit;
	}

	public void setClasspathref(Reference reference) {
		classpathReference = createPath(reference);
	}

	public void setBndfilePath(Reference reference) {
		assertPathNotSet(bndfilePath, "bnd files are already set");
		bndfilePath = createPath(reference);
	}

	public void addClasspath(Path path) {
		assertPathNotSet(classpathReference, "Classpath reference is already set");
		classpathReference = path;
	}

	public void addBndfiles(Path path) {
		assertPathNotSet(bndfilePath, "bnd files are already set");
		bndfilePath = path;
	}

	private Path createPath(Reference r) {
		Path path = new Path(getProject()).createPath();
		path.setRefid(r);
		return path;
	}

	private void assertPathNotSet(Path path, String message) {
		if (path != null) {
			throw new BuildException(message);
		}
	}

	/**
	 * validate required parameters before starting execution
	 * 
	 * @throws BuildException
	 *             , if build is impossible
	 */
	protected void validate() {
		// no one of the 2 classpaths handling styles are defined
		// how could bnd work ?
		if (classpath == null && classpathReference == null) {
			log("Unable to get a classpath ...attributes not set");
			throw new BuildException("No one of the classpath or classpathref defined...");
		}
		if (classpathDirectlySet == true && classpathReference != null) {
			log("Unable to choose between classpath & classpathref !!");
			throw new BuildException("Can't choose between classpath & classpathref");
		}
	}

	// updates classpath for classpathref and nested classpath

	private void updateClasspath() {
		log("Updating classpath after classpathref setting");
		if (classpathReference == null) {
			return;
		}
		addFilesFrom(classpathReference, classpath);
	} // updateClasspath()

	private void updateBndFiles() {
		if (bndfilePath == null) {
			return;
		}
		addFilesFrom(bndfilePath, files);
	}

	private void addFilesFrom(Path path, List<File> files) {
		for (String fileName : path.list()) {
			files.add(new File(fileName.replace('\\', '/')));
		}
	}
}
