package aQute.bnd.service.remoteworkspace;

import java.io.Closeable;
import java.util.List;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Builder;
import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.bnd.service.specifications.RunSpecification;

/**
 * A remote RPC interface to a workspace. This API is designed to run on the
 * same machine as the caller so that file paths can be used.
 * <p>
 * Paths in this API must all be absolute and formatted in the operating system
 * specific format. (I.e. File.getAbsolutePath()). They must also reside in the
 * the workspace that this object is attached to.
 */
public interface RemoteWorkspace extends Closeable {

	/**
	 * Get the bnd version of the workspace. This is the {@link About#CURRENT}
	 * value.
	 */
	String getBndVersion();

	/**
	 * Parse a bndrun file (which can also a plain bnd file with -run*
	 * instructions) and provide the resulting run specification.
	 *
	 * @param pathToBndOrBndrun the path to a bnd or bndrun file, never null
	 */
	RunSpecification getRun(String pathToBndOrBndrun);

	/**
	 * Analyze the project given as a parameter and provide the setup
	 * information. This is intended to be used by a JUnit test project that
	 * wants to provide its test classes and imports from the
	 * -buildpath/-testpath to the framework to be exported. The idea is that
	 * this allows the classes from the JUnit tests to reside outside the
	 * framework but leverage the same classes inside. This significantly
	 * changes testing.
	 *
	 * @param projectDir the absolute path in
	 */
	RunSpecification analyzeTestSetup(String projectDir);

	/**
	 * Get the latest bundles from a specification. The specification is in the
	 * format used for a -buildpath/-testpath/-runbundles, etc. It can contain
	 * multiple bundles.
	 *
	 * @param projectDir The absolute path to the project directory where
	 *            {@link Project#getBundles(aQute.bnd.service.Strategy, String, String)}
	 *            is called.
	 * @param specification A specification for bundles/
	 * @return A list with absolute paths (in OS specific form) to the
	 *         JARs/bundles specified.
	 */
	List<String> getLatestBundles(String projectDir, String specification);

	/**
	 * Build a bundle based on a BuilderSpecification. This allows the remote
	 * controller to create bundles on the fly using project specific context.
	 *
	 * @param projectPath The path to the project
	 * @param spec the specification for a Builder, see
	 *            {@link Builder#from(BuilderSpecification)}.
	 * @return the content of the JAR file
	 */
	byte[] build(String projectPath, BuilderSpecification spec);

	/**
	 * Get a list of all projects.
	 *
	 * @return the list of all projects
	 */
	List<String> getProjects();

}
