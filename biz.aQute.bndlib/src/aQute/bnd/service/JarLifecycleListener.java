package aQute.bnd.service;

import java.io.File;
import java.util.Map;

import aQute.bnd.build.Project;
import aQute.bnd.exceptions.ConsumerWithException;
import aQute.bnd.osgi.Jar;

/**
 * A plugin interface for hooking into the JAR artifact lifecycle during project
 * builds.
 * <p>
 * Implementations can observe and participate in key phases of JAR artifact
 * handling: before the JAR is written to disk and after it has been written.
 * This enables plugins to compute metadata, validate artifacts, sign JARs, or
 * perform other side-effects without modifying the core build logic.
 * <p>
 * Listeners are called in order of registration. If one listener throws an
 * exception, the exception is logged but does not prevent other listeners from
 * running.
 * <p>
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations must be thread-safe. Each JAR write receives a fresh context
 * map, so listeners can use the context to pass data between the before and
 * after phases without storing mutable state.
 * <p>
 * <h2>Typical Use Cases</h2>
 * <ul>
 * <li><b>Artifact metadata</b> – Compute and store digests, checksums, or
 * signatures as sidecar files (e.g., `.digest`, `.asc`, `.md5`).</li>
 * <li><b>Rebuild optimization</b> – Detect whether the JAR content or API has
 * changed to avoid unnecessary cascade rebuilds in incremental build
 * scenarios.</li>
 * <li><b>JAR signing</b> – Sign the JAR post-write using cryptographic
 * keys.</li>
 * <li><b>Build artifacts indexing</b> – Log or index built artifacts in a
 * repository or build database.</li>
 * <li><b>Binary validation</b> – Post-write validation or security
 * scanning.</li>
 * <li><b>Bytecode enhancement</b> – Transform JAR contents before write (e.g.,
 * AspectJ weaving, code coverage instrumentation).</li>
 * </ul>
 * <p>
 * <h2>Usage Example</h2>
 * <p>
 * To compute and store a digest before writing, then validate and restore a
 * timestamp after writing:
 * <p>
 *
 * <pre>
 * public class DigestListener implements JarLifecycleListener {
 *
 * 	&#64;Override
 * 	public void beforeWrite(Project project, Jar jar, File outputFile, Map&lt;String, Object&gt; context)
 * 		throws Exception {
 * 		// Compute digest before the JAR is written to disk
 * 		byte[] digest = jar.getTimelessDigest();
 * 		String digestHex = Hex.toHexString(digest);
 * 		context.put("digestHex", digestHex);
 * 	}
 *
 * 	&#64;Override
 * 	public void afterWrite(Project project, File outputFile, Jar jar, Map&lt;String, Object&gt; context)
 * 		throws Exception {
 * 		// Retrieve the computed digest
 * 		String digestHex = (String) context.get("digestHex");
 *
 * 		// Store it in a sidecar file
 * 		File digestFile = new File(outputFile.getParentFile(), outputFile.getName() + ".digest");
 * 		IO.store(digestHex, digestFile);
 * 	}
 * }
 * </pre>
 * <p>
 * <h2>Context Map</h2>
 * <p>
 * The {@code context} parameter is a mutable map that persists across the
 * before and after phases for a single JAR write. Listeners can use this to
 * exchange data:
 * <ul>
 * <li>The {@code beforeJarWrite} phase can compute and store values in the
 * context.</li>
 * <li>The {@code afterJarWrite} phase can retrieve those values to perform
 * post-write actions.</li>
 * <li>Multiple listeners can coordinate through shared context keys (with
 * appropriate synchronization if needed).</li>
 * </ul>
 * <p>
 * Each JAR write receives a fresh context map; there is no data leakage between
 * different JAR writes.
 * <p>
 * <h2>Order of Execution</h2>
 * <ol>
 * <li>All {@code beforeJarWrite} methods are invoked in registration
 * order.</li>
 * <li>The JAR is written to disk (via
 * {@link Project#write(ConsumerWithException, File)}).</li>
 * <li>All {@code afterJarWrite} methods are invoked in registration order.</li>
 * </ol>
 * <p>
 * <h2>Error Handling</h2>
 * <p>
 * If a listener throws an exception:
 * <ul>
 * <li>The exception is caught and logged.</li>
 * <li>Other listeners continue to run.</li>
 * <li>The JAR write itself is not affected (exceptions do not roll back the
 * write).</li>
 * </ul>
 * <p>
 * Implementations should avoid throwing exceptions unless they represent truly
 * exceptional conditions. Use logging for informational messages.
 * <p>
 *
 * @see Project#saveBuild(Jar)
 */
public interface JarLifecycleListener {

	/**
	 * Invoked before the JAR is written to the output file.
	 * <p>
	 * This phase is suitable for:
	 * <ul>
	 * <li>Computing metadata (digests, checksums, API signatures) that should
	 * be based on the in-memory JAR state.</li>
	 * <li>Preparing data structures for the post-write phase.</li>
	 * <li>Validating the JAR before it is persisted.</li>
	 * </ul>
	 * <p>
	 * The listener can store computed values in the {@code context} map for use
	 * in the {@link #afterWrite afterJarWrite} phase.
	 * <p>
	 *
	 * @param project the project being built
	 * @param jar the in-memory JAR object, before being written to disk
	 * @param outputFile the file where the JAR will be written
	 * @param context a mutable map for sharing data between before and after
	 *            phases; persists across both phases for a single JAR write
	 * @throws Exception if an error occurs; the exception is logged but does
	 *             not prevent other listeners or the JAR write from proceeding
	 */
	default void beforeWrite(Project project, Jar jar, File outputFile, Map<String, Object> context)
		throws Exception {}

	/**
	 * Invoked after the JAR has been successfully written to the output file.
	 * <p>
	 * This phase is suitable for:
	 * <ul>
	 * <li>Storing metadata (digests, signatures) in sidecar files.</li>
	 * <li>Modifying file properties (timestamps, permissions).</li>
	 * <li>Triggering post-write notifications or side-effects.</li>
	 * <li>Validating the written file.</li>
	 * </ul>
	 * <p>
	 * The listener can retrieve data computed in the {@link #beforeWrite
	 * beforeJarWrite} phase from the {@code context} map.
	 * <p>
	 * <b>Note:</b> The JAR object may be in a different state after writing
	 * (e.g., resources may have been closed). Listeners should not rely on the
	 * JAR being fully functional; use the {@code outputFile} and the
	 * {@code context} map instead.
	 * <p>
	 *
	 * @param project the project that was built
	 * @param outputFile the file to which the JAR was written; the file is
	 *            guaranteed to exist on the filesystem
	 * @param jar the JAR object that was written (state may have changed)
	 * @param context the same mutable map passed to {@link #beforeWrite
	 *            beforeJarWrite}, allowing access to data computed in that
	 *            phase
	 * @throws Exception if an error occurs; the exception is logged but does
	 *             not prevent other listeners from running
	 */
	default void afterWrite(Project project, File outputFile, Jar jar, Map<String, Object> context)
		throws Exception {}


}