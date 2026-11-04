package bndtools.central;

import java.io.File;
import java.util.Map;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.JarLifecycleListener;
import bndtools.central.RebuildTriggerPolicy.RebuildTriggerPolicyResult;

public class RebuildTriggerPolicyPlugin implements JarLifecycleListener {

	/**
	 * The rebuild trigger policy.
	 * <p>
	 * The rebuild trigger policy determines whether a rebuilt bundle's output
	 * JAR file timestamp is preserved when the rebuild produces an unchanged
	 * artifact. This prevents unnecessary cascade rebuilds of dependent
	 * projects in incremental build scenarios.
	 * <p>
	 * Two policies are supported:
	 * <ul>
	 * <li><b>always</b> – (default) Every rebuild updates the JAR timestamp,
	 * triggering rebuilds in all downstream projects.</li>
	 * <li><b>api</b> – Preserves the JAR timestamp when the content is
	 * byte-identical or the exported API surface is unchanged. Only projects
	 * that consume the exported API will rebuild when it changes; internal-only
	 * changes do not cascade.</li>
	 * </ul>
	 * <p>
	 * This setting is workspace-wide and applies to all projects in the
	 * workspace. It is typically set by the build environment (e.g. Eclipse IDE
	 * preferences, Maven/Gradle plugin configuration) rather than hardcoded in
	 * build files.
	 *
	 */
	private final String				rebuildTriggerPolicy;
	private final RebuildTriggerPolicy	policy;

	public RebuildTriggerPolicyPlugin(String rebuildTriggerPolicyKey) {
		this.rebuildTriggerPolicy = rebuildTriggerPolicyKey;
		this.policy = new RebuildTriggerPolicy(this.rebuildTriggerPolicy);
    }

    @Override
	public void beforeWrite(Project project, Jar jar, File outputFile, Map<String, Object> context) {
        // Compute digests before write
		RebuildTriggerPolicyResult res = policy
			.doRebuildTriggerPolicy(jar, outputFile);
		context.put("result", res);
    }

    @Override
	public void afterWrite(Project project, File outputFile, Jar jar, Map<String, Object> context) {
		// Persist digests and restore timestamp if needed
		RebuildTriggerPolicyResult res = (RebuildTriggerPolicyResult) context.get("result");
		policy.persistRebuildTriggerPolicyResult(outputFile, res);
    }

}