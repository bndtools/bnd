package aQute.bnd.maven.lib.configuration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileTree {

	private final aQute.lib.io.FileTree fileTree;

	public FileTree() {
		fileTree = new aQute.lib.io.FileTree();
	}

	/**
	 * Can be used to add specific files to the return value of
	 * {@link #getFiles(File, String...)} and {@link #getFiles(File, List)}.
	 *
	 * @param file A file to include in the return value of
	 *            {@link #getFiles(File, String...)} and
	 *            {@link #getFiles(File, List)}.
	 */
	public void addFile(File file) {
		fileTree.addFile(file);
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 *
	 * @param includes Add an Ant-style glob
	 */
	public void addIncludes(List<String> includes) {
		fileTree.addIncludes(includes);
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 *
	 * @param includes Add an Ant-style glob
	 */
	public void addIncludes(String... includes) {
		fileTree.addIncludes(includes);
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 *
	 * @param excludes Add an Ant-style glob
	 */
	public void addExcludes(String... excludes) {
		fileTree.addExcludes(excludes);
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 *
	 * @param excludes Add an Ant-style glob
	 */
	public void addExcludes(List<String> excludes) {
		fileTree.addExcludes(excludes);
	}

	/**
	 * Return a list of files using the specified baseDir and the configured
	 * include and exclude Ant-style glob expressions.
	 *
	 * @param baseDir The base directory for locating files.
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A list of files.
	 * @throws IOException If an exception occurs.
	 */
	public List<File> getFiles(File baseDir, String... defaultIncludes) throws IOException {
		return fileTree.getFiles(baseDir, defaultIncludes);
	}

	/**
	 * Return a list of files using the specified baseDir and the configured
	 * include and exclude Ant-style glob expressions.
	 *
	 * @param baseDir The base directory for locating files.
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A list of files.
	 * @throws IOException If an exception occurs.
	 */
	public List<File> getFiles(File baseDir, List<String> defaultIncludes) throws IOException {
		return fileTree.getFiles(baseDir, defaultIncludes);
	}

	/**
	 * Add an Ant-style glob to the include patterns.
	 *
	 * @param include Add an Ant-style glob
	 */
	public void setInclude(String include) {
		fileTree.addIncludes(include);
	}

	/**
	 * Add an Ant-style glob to the exclude patterns.
	 *
	 * @param exclude Add an Ant-style glob
	 */
	public void setExclude(String exclude) {
		fileTree.addExcludes(exclude);
	}

}
