package aQute.bnd.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.version.Version;
import aQute.libg.qtokens.QuotedTokenizer;

/**
 * Task to wrap a JAR as an OSGi bundle. You can specify the following
 * properties:
 * <ul>
 * <li>bsn and version = Will set the appropriate properties</li>
 * <li>a classpath</li>
 * <li>an output directory or an output file if only one JAR is specified</li>
 * <li>A search directory (definitions) for bnd files named the same as the
 * source which are used for info</li>
 * </ul>
 */
public class WrapTask extends BaseTask {
	/**
	 * List of jars to wrap
	 */
	List<File>	jars		= new ArrayList<>();

	/**
	 * Output directory or file (directory must be used
	 */
	File		output		= null;
	File		definitions	= null;
	List<File>	classpath	= new ArrayList<>();
	String		bsn;
	Version		version;
	boolean		force;
	boolean		failok;
	boolean		exceptions;

	@Override
	public void execute() throws BuildException {
		boolean failed = false;

		try {
			if (jars == null)
				throw new BuildException("No files set", getLocation());

			if (output != null && jars.size() > 1 && !output.isDirectory()) {
				throw new BuildException(
					"Multiple jars must be wrapped but the output given is not a directory " + output, getLocation());
			}

			if (definitions != null && jars.size() > 1 && !definitions.isDirectory()) {
				throw new BuildException(
					"Multiple jars must be wrapped but the definitions parameters is not a directory " + definitions,
					getLocation());
			}

			for (File file : jars) {

				if (!file.isFile()) {
					failed = true;
					System.err.println("Non existent file to wrap " + file);
					continue;
				}

				try (Analyzer wrapper = new Analyzer()) {
					wrapper.setPedantic(isPedantic());
					wrapper.setTrace(isTrace());
					wrapper.setExceptions(exceptions);
					wrapper.setBase(getProject().getBaseDir());
					wrapper.addClasspath(classpath);

					if (failok)
						wrapper.setFailOk(true);

					wrapper.setJar(file);
					wrapper.addProperties(getProject().getProperties());
					wrapper.setDefaults(bsn, version);

					File outputFile = wrapper.getOutputFile(output == null ? null : output.getAbsolutePath());

					if (definitions != null) {
						File properties = definitions;
						if (properties.isDirectory()) {
							String pfile = wrapper.replaceExtension(outputFile.getName(),
								Constants.DEFAULT_JAR_EXTENSION, Constants.DEFAULT_BND_EXTENSION);
							properties = new File(definitions, pfile);
						}
						if (properties.isFile()) {
							wrapper.setProperties(properties);
						}
					}

					Manifest manifest = wrapper.calcManifest();
					if (wrapper.isOk()) {
						wrapper.getJar()
							.setManifest(manifest);
						boolean saved = wrapper.save(outputFile, force);
						log(String.format("%30s %6d %s%n", wrapper.getJar()
							.getBsn() + "-"
							+ wrapper.getJar()
								.getVersion(),
							outputFile.length(), saved ? "" : "(not modified)"));
					}

					failed |= report(wrapper);
				}
			}
		} catch (Exception e) {

			if (exceptions)
				e.printStackTrace();

			if (!failok)
				throw new BuildException("Failed to build jar file: " + e, getLocation());
		}
		if (failed && !failok)
			throw new BuildException("Failed to wrap jar file", getLocation());
	}

	public void setJars(String files) {
		addAll(this.jars, files, ",");
	}

	void addAll(List<File> list, String files, String separator) {
		QuotedTokenizer qt = new QuotedTokenizer(files, separator);
		String entries[] = qt.getTokens();
		File project = getProject().getBaseDir();
		for (int i = 0; i < entries.length; i++) {
			File f = getFile(project, entries[i]);
			if (f.exists())
				list.add(f);
			else
				messages.NoSuchFile_(f.getAbsoluteFile());
		}
	}

	public void setClasspath(String files) {
		addAll(classpath, files, File.pathSeparator + ",");
	}

	boolean isFailok() {
		return failok;
	}

	public void setFailok(boolean failok) {
		this.failok = failok;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	@Override
	public void setExceptions(boolean exceptions) {
		this.exceptions = exceptions;
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public void setDefinitions(File out) {
		definitions = out;
	}

	public void addConfiguredFileSet(FileSet list) {
		DirectoryScanner scanner = list.getDirectoryScanner(getProject());
		String files[] = scanner.getIncludedFiles();
		for (int i = 0; i < files.length; i++) {
			File f = getFile(scanner.getBasedir(), files[i]);
			this.jars.add(f);
		}
	}

	public void setVersion(String version) {
		this.version = new Version(version);
	}

	public void setBsn(String bsn) {
		this.bsn = bsn;
	}
}
