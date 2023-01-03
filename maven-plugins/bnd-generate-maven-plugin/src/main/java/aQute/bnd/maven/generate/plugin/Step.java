package aQute.bnd.maven.generate.plugin;

import java.util.Properties;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * A 'generate' instruction will be created from this configuration as described
 * in the <a href="https://bnd.bndtools.org/instructions/generate.html">bnd
 * generate documentation</a>
 */
public class Step {

	/**
	 * This is the clause in the resulting
	 * <a href="https://bnd.bndtools.org/instructions/generate.html"> generate
	 * instruction</a>. It is used to establish a fileset where wildcards are
	 * supported. If one of the files is newer then any of the files in the
	 * output, code will be regenerated.
	 */
	@Parameter
	private String		trigger;

	/**
	 * This is the output directory in the resulting
	 * <a href="https://bnd.bndtools.org/instructions/generate.html"> generate
	 * instruction</a>
	 */
	@Parameter
	private String	output;

	/**
	 * The generate option in the resulting
	 * <a href="https://bnd.bndtools.org/instructions/generate.html"> generate
	 * instruction. It is used to find a external plugin which may hold a
	 * generator or a declared Main-Class</a>
	 */
	@Parameter(required = false)
	private String		generateCommand	= null;

	/**
	 * The system option in the resulting
	 * <a href="https://bnd.bndtools.org/instructions/generate.html"> generate
	 * instruction. It will be executed as a system command.</a>
	 */
	@Parameter(required = false)
	private String		systemCommand	= null;

	/**
	 * The clear option in the resulting
	 * <a href="https://bnd.bndtools.org/instructions/generate.html"> generate
	 * instruction. It instruct the generator to not clear the output folder
	 * before a run. The default is true.</a>
	 */
	@Parameter(required = false)
	private boolean		clear			= true;

	/**
	 * Any additional properties that the specific generate plugin will support.
	 */
	@Parameter(property = "properties", required = false)
	private Properties	properties		= new Properties();

	public String getTrigger() {
		return trigger;
	}

	public String getOutput() {
		return output;
	}

	public String getGenerateCommand() {
		return generateCommand;
	}

	public String getSystemCommand() {
		return systemCommand;
	}

	public Properties getProperties() {
		return properties;
	}

	public boolean isClear() {
		return clear;
	}
}
