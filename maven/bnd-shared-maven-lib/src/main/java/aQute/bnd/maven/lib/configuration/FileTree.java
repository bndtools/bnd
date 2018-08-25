package aQute.bnd.maven.lib.configuration;

public class FileTree extends aQute.lib.io.FileTree {

	public FileTree() {
		super();
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 * 
	 * @param include Add an Ant-style glob
	 */
	public void setInclude(String include) {
		addIncludes(include);
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 * 
	 * @param exclude Add an Ant-style glob
	 */
	public void setExclude(String exclude) {
		addExcludes(exclude);
	}

}
