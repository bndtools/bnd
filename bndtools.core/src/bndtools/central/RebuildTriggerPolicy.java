package bndtools.central;

import java.io.File;
import java.security.MessageDigest;
import java.util.jar.Manifest;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;

import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.diff.Tree;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;

/**
 * Encapsulates the logic for determining whether a built artifact (JAR) should
 * retain its existing filesystem timestamp after a rebuild.
 * <p>
 * This is used to avoid unnecessary downstream rebuilds in systems that rely on
 * timestamp-based staleness checks. Instead of always updating the output file
 * timestamp, this policy compares digests of the newly built artifact against
 * previously stored values.
 * </p>
 * <h2>Supported policies</h2>
 * <ul>
 * <li><b>always</b> – Always treat the build as changed. The output file
 * receives a new timestamp.</li>
 * <li><b>api</b> – Preserve the timestamp when either:
 * <ul>
 * <li>The full content digest is unchanged (byte-identical JAR), or</li>
 * <li>The public API surface is unchanged, even if implementation details
 * differ.</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * The API-based optimization helps prevent rebuild cascades in dependent
 * projects when only internal implementation changes occur.
 * <h2>Timestamp Preservation Strategy</h2>
 * <p>
 * To avoid unnecessary cascade rebuilds when only non-API changes occur, this
 * implementation preserves the output JAR's timestamp when:
 * </p>
 * <ul>
 * <li>Content digest matches (byte-identical), OR</li>
 * <li>API digest matches (exported API unchanged)</li>
 * </ul>
 * <h3>Mechanism</h3>
 * <p>
 * The system computes content and API digests <strong>before</strong> writing
 * the JAR, compares them against previously stored digests, and if a match is
 * found, restores the old timestamp after the JAR is written. This prevents
 * downstream projects from seeing a "new" dependency based on file timestamp
 * alone.
 * </p>
 * <h3>Limitations</h3>
 * <p>
 * This pragmatic approach trades perfect accuracy for simplicity:
 * </p>
 * <ul>
 * <li>Timestamps are preserved by directly setting {@code lastModified()}. This
 * works well in most incremental build scenarios but may not perfectly reflect
 * semantic changes in all edge cases.</li>
 * <li>Attempting to perfectly distinguish between "this JAR actually changed"
 * vs. "this build cycle recomputed the same thing" across multiple asynchronous
 * build tool invocations is complex and fragile.</li>
 * <li><strong>Not recommended for:</strong> highly dynamic build environments,
 * complex classpath interdependencies, or scenarios requiring perfect timestamp
 * accuracy for external build tools.</li>
 * </ul>
 * <p>
 * In practice, this approach provides substantial benefits by preventing most
 * unnecessary rebuilds while maintaining reliability.
 * </p>
 *
 * @see RebuildTriggerPolicy#doRebuildTriggerPolicy(Jar, File)
 */
public class RebuildTriggerPolicy {

	public static final String		REBUILDTRIGGERPOLICY_ALWAYS	= "always";
	public static final String		REBUILDTRIGGERPOLICY_API	= "api";

	private static final ILogger	logger	= Logger.getLogger(RebuildTriggerPolicy.class);
	private final String			rebuildTriggerPolicyKey;
	private final DiffPluginImpl	differ						= new DiffPluginImpl();

	/**
	 * Result of evaluating the build change policy.
	 * <p>
	 * This record contains both the decision (whether to preserve the
	 * timestamp) and the computed digest values needed for persisting state for
	 * future builds.
	 * </p>
	 *
	 * @param preserveTimestamp The timestamp to restore on the output file, or
	 *            {@code 0} if the file should receive a new timestamp.
	 * @param contentDigestFile The file used to store the content digest, or
	 *            {@code null} if not applicable.
	 * @param newContentDigestHex The newly computed content digest
	 *            (hex-encoded), or {@code null} if not computed.
	 * @param apiDigestFile The file used to store the API digest, or
	 *            {@code null} if not applicable.
	 * @param newApiDigestHex The newly computed API digest (hex-encoded), or
	 *            {@code null} if not computed.
	 */
	record RebuildTriggerPolicyResult(long preserveTimestamp, File contentDigestFile, String newContentDigestHex,
		File apiDigestFile, String newApiDigestHex) {

		static final RebuildTriggerPolicyResult REBUILD_ALWAYS = new RebuildTriggerPolicyResult(0, null, null, null,
			null);
	}

	RebuildTriggerPolicy(String rebuildTriggerPolicyKey) {
		this.rebuildTriggerPolicyKey = rebuildTriggerPolicyKey;
	}

	/**
	 * Applies the configured build change policy to determine whether the
	 * output file's timestamp should be preserved.
	 * <p>
	 * Depending on the selected policy, this method may compute one or both of
	 * the following:
	 * </p>
	 * <ul>
	 * <li>A <b>content digest</b> — a stable hash of the entire JAR
	 * contents.</li>
	 * <li>An <b>API digest</b> — a hash of the exported public/protected API
	 * surface.</li>
	 * </ul>
	 * <p>
	 * The method attempts to reuse previously stored digests (if present) to
	 * detect whether the newly built artifact is equivalent to the previous
	 * one, either byte-for-byte or at the API level.
	 * </p>
	 * <p>
	 * The returned {@link RebuildTriggerPolicyResult} contains both the
	 * decision (timestamp preservation) and the newly computed digest values,
	 * which callers are expected to persist for future comparisons.
	 * </p>
	 * <h3>Behavior summary</h3>
	 * <ul>
	 * <li>If policy is {@code "always"}, no comparison is performed and the
	 * output is treated as changed.</li>
	 * <li>If the output file does not yet exist, it is treated as a new
	 * build.</li>
	 * <li>If the content digest matches the previous build, the timestamp is
	 * preserved.</li>
	 * <li>If the policy allows API comparison and the API digest matches, the
	 * timestamp is also preserved.</li>
	 * <li>Otherwise, the output file receives a new timestamp.</li>
	 * </ul>
	 *
	 * @param ws The processor providing configuration (notably the
	 *            {@code -buildchangepolicy} setting).
	 * @param jar The newly built JAR to analyze.
	 * @param outputFile The output file whose timestamp may be preserved.
	 * @return a {@link RebuildTriggerPolicyResult} describing the preservation
	 *         decision and the computed digest values
	 */
	RebuildTriggerPolicyResult doRebuildTriggerPolicy(Jar jar, File outputFile) {
		if (REBUILDTRIGGERPOLICY_ALWAYS.equals(rebuildTriggerPolicyKey)) {
			return RebuildTriggerPolicyResult.REBUILD_ALWAYS;
		}

		File contentDigestFile = getContentDigestFile(outputFile);
		String newContentDigestHex = calcContentDigest(jar);

		// Fast path: no existing output means nothing to preserve.
		long existingTimestamp = outputFile.lastModified();

		if (existingTimestamp == 0) {
			// existingTimestamp==0 means file does not exist
			return new RebuildTriggerPolicyResult(0, contentDigestFile, newContentDigestHex, null, null);
		}


		// Check 1: byte-identical content -> preserve immediately.
		if (digestMatches(contentDigestFile, newContentDigestHex, outputFile, "stored content digest")) {
			return new RebuildTriggerPolicyResult(existingTimestamp, contentDigestFile, newContentDigestHex, null,
				null);
		}

		// Check 2: compute API digest
		File apiDigestFile = getApiDigestFile(outputFile);
		String newApiDigestHex = calcApiDigest(jar);

		if (digestMatches(apiDigestFile, newApiDigestHex, outputFile, "stored API digest")) {
			// Content changed but API unchanged -> preserving timestamp"
			return new RebuildTriggerPolicyResult(existingTimestamp, contentDigestFile, newContentDigestHex,
				apiDigestFile, newApiDigestHex);
		}

		return new RebuildTriggerPolicyResult(0, contentDigestFile, newContentDigestHex, apiDigestFile,
			newApiDigestHex);

	}

	private boolean digestMatches(File digestFile, String newDigestHex, File outputFile, String digestDescription) {
		if (newDigestHex == null || !digestFile.isFile()) {
			return false;
		}
		try {
			String oldDigestHex = IO.collect(digestFile)
				.trim();
			return newDigestHex.equals(oldDigestHex);
		} catch (Exception e) {
			logger.logWarning("Failed to read " + digestDescription + " for " + outputFile.getName(), e);
			return false;
		}
	}

	private String calcContentDigest(Jar jar) {
		String newDigestHex = null;
		try {
			byte[] digest = jar.getTimelessDigest();
			if (digest != null) {
				newDigestHex = Hex.toHexString(digest);
			}
		} catch (Exception e) {
			logger.logWarning("Failed to compute timeless digest for " + jar.getName(), e);
		}
		return newDigestHex;
	}

	/**
	 * Compute a digest of the exported API surface of the JAR. This captures
	 * the public/protected types, methods, and fields in exported packages.
	 * Internal implementation changes that don't affect the exported API will
	 * produce the same digest, allowing dependent projects to skip rebuilding.
	 *
	 * @param jar the built JAR to analyze
	 * @return hex-encoded SHA-1 digest of the API surface, or null on failure
	 */
	private String calcApiDigest(Jar jar) {
		try {
			Manifest manifest = jar.getManifest();
			if (manifest == null) {
				return null;
			}
			String exportPackage = manifest.getMainAttributes()
				.getValue(Constants.EXPORT_PACKAGE);
			if (exportPackage == null || exportPackage.isEmpty()) {
				return null;
			}
			Tree tree = differ.tree(jar);
			Tree apiTree = tree.get("<api>");
			if (apiTree == null) {
				return null;
			}
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			digestTree(md, apiTree);
			return Hex.toHexString(md.digest());
		} catch (Exception e) {
			logger.logWarning("Failed to compute API digest for " + jar.getName(), e);
			return null;
		}
	}

	/**
	 * Recursively feed the tree's type and name into the digest. Children are
	 * already sorted in the Element constructor, so the digest is
	 * deterministic.
	 */
	private void digestTree(MessageDigest md, Tree tree) {
		md.update(tree.getType()
			.name()
			.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		md.update((byte) ':');
		md.update(tree.getName()
			.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		md.update((byte) '\n');
		for (Tree child : tree.getChildren()) {
			digestTree(md, child);
		}
	}

	static File getContentDigestFile(File outputFile) {
		return new File(outputFile.getParentFile(), outputFile.getName() + ".digest");
	}

	static File getApiDigestFile(File outputFile) {
		return new File(outputFile.getParentFile(), outputFile.getName() + ".api-digest");
	}

	void persistRebuildTriggerPolicyResult(File outputFile, RebuildTriggerPolicyResult result) {
		if (REBUILDTRIGGERPOLICY_ALWAYS.equals(this.rebuildTriggerPolicyKey)) {
			// do not store any .digest files by default
			// to avoid polluting / confusing existing projects
			return;
		}

		// Store the content digest for future comparisons
		if (result.newContentDigestHex() != null) {
			try {
				IO.store(result.newContentDigestHex(), result.contentDigestFile());
			} catch (Exception e) {
				logger.logWarning("Failed to store content digest for " + outputFile.getName(), e);
			}
		}

		// Store the API digest for future comparisons
		if (result.newApiDigestHex() != null) {
			try {
				IO.store(result.newApiDigestHex(), result.apiDigestFile());
			} catch (Exception e) {
				logger.logWarning("Failed to store API digest for " + outputFile.getName(), e);
			}
		}

		// If the content or API was unchanged, restore the old timestamp
		// to prevent downstream cascade rebuilds
		if (result.preserveTimestamp() > 0) {
			// Preserved timestamp: This is the main thing this
			// RebuildTriggerPolicy is about
			outputFile.setLastModified(result.preserveTimestamp());
		}
	}
}
