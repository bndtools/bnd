package aQute.bnd.maven.plugin;

import static aQute.bnd.maven.lib.executions.PluginExecutions.extractClassifier;
import static aQute.bnd.maven.lib.executions.PluginExecutions.matchesClassifier;

import java.util.List;
import java.util.Optional;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import aQute.bnd.maven.lib.executions.PluginExecutions;

/**
 * This lifecycle participant is meant to simplify the changes required to the
 * configuration of the maven packaging plugins when the
 * {@code bnd-maven-plugin} is used. It will silently "scan" projects, and
 * disable the {@code maven-jar-plugin} or the {@code maven-war-plugin}
 * appropriately.
 * <p>
 * Lifecycle participants are only active when the host plugin
 * ({@code bnd-maven-plugin} in this case) has:
 * <p>
 * <code><pre>&lt;extensions>true&lt;/extensions></pre></code>
 * <p>
 * This acts as the opt-in. Without it the {@code bnd-maven-plugin},
 * {@code maven-jar-plugin} and {@code maven-war-plugin} behave in the
 * traditional fashion.
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "aQute.bnd.maven.plugin.BndPackagingLifecycleParticipant")
public class BndPackagingLifecycleParticipant extends AbstractMavenLifecycleParticipant implements LogEnabled {

	public static final String	THIS_GROUP_ID					= "biz.aQute.bnd";

	public static final String	THIS_ARTIFACT_ID				= "bnd-maven-plugin";

	public static final String	MAVEN_JAR_PLUGIN_GROUP_ID		= "org.apache.maven.plugins";

	public static final String	MAVEN_JAR_PLUGIN_ARTIFACT_ID	= "maven-jar-plugin";

	public static final String	MAVEN_WAR_PLUGIN_GROUP_ID		= "org.apache.maven.plugins";

	public static final String	MAVEN_WAR_PLUGIN_ARTIFACT_ID	= "maven-war-plugin";

	private Logger			logger;

	@Override
	public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
		try {
			for (MavenProject project : session.getProjects()) {
				Model model = project.getModel();

				final Plugin bndMavenPlugin = getBndMavenPlugin(model);

				if (bndMavenPlugin != null) {
					final Plugin mavenJarPlugin = getMavenJarPlugin(model);

					if (mavenJarPlugin != null) {
						processExecutions(bndMavenPlugin.getExecutions(), mavenJarPlugin, project);
					}

					final Plugin mavenWarPlugin = getMavenWarPlugin(model);

					if (mavenWarPlugin != null) {
						processExecutions(bndMavenPlugin.getExecutions(), mavenWarPlugin, project);
					}
				}
			}
		} catch (IllegalStateException e) {
			throw new MavenExecutionException(e.getMessage(), e);
		}
	}

	@Override
	public void enableLogging(final Logger logger) {
		this.logger = logger;
	}

	protected Optional<PluginExecution> findMatchingMavenPackagingPluginExecution(Plugin mavenPackagingPlugin,
		String classifier) {
		return mavenPackagingPlugin.getExecutions()
			.stream()
			.filter(ex -> matchesClassifier(ex, classifier))
			.findFirst();
	}

	/**
	 * Returns the bnd-maven-plugin from build/plugins section of model or
	 * {@code null} if not present.
	 */
	protected Plugin getBndMavenPlugin(final Model model) {
		final Build build = model.getBuild();
		if (build != null) {
			return getBndMavenPluginFromContainer(build);
		}
		return null;
	}

	/**
	 * Returns the bnd-maven-plugin from pluginContainer or {@code null} if not
	 * present.
	 */
	protected Plugin getBndMavenPluginFromContainer(final PluginContainer pluginContainer) {
		return getPluginByGAFromContainer(THIS_GROUP_ID, THIS_ARTIFACT_ID, pluginContainer);
	}

	/**
	 * Returns the maven-jar-plugin from build/plugins section of model or
	 * {@code null} if not present.
	 */
	protected Plugin getMavenJarPlugin(final Model model) {
		final Build build = model.getBuild();
		if (build != null) {
			return getMavenJarPluginFromContainer(build);
		}
		return null;
	}

	/**
	 * Returns the maven-jar-plugin from pluginContainer or {@code null} if not
	 * present.
	 */
	protected Plugin getMavenJarPluginFromContainer(final PluginContainer pluginContainer) {
		return getPluginByGAFromContainer(MAVEN_JAR_PLUGIN_GROUP_ID, MAVEN_JAR_PLUGIN_ARTIFACT_ID, pluginContainer);
	}

	/**
	 * Returns the maven-war-plugin from build/plugins section of model or
	 * {@code null} if not present.
	 */
	protected Plugin getMavenWarPlugin(final Model model) {
		final Build build = model.getBuild();
		if (build != null) {
			return getMavenWarPluginFromContainer(build);
		}
		return null;
	}

	/**
	 * Returns the maven-war-plugin from pluginContainer or {@code null} if not
	 * present.
	 */
	protected Plugin getMavenWarPluginFromContainer(final PluginContainer pluginContainer) {
		return getPluginByGAFromContainer(MAVEN_WAR_PLUGIN_GROUP_ID, MAVEN_WAR_PLUGIN_ARTIFACT_ID, pluginContainer);
	}

	protected Plugin getPluginByGAFromContainer(final String groupId, final String artifactId,
		final PluginContainer pluginContainer) {
		Plugin result = null;
		for (Plugin plugin : pluginContainer.getPlugins()) {
			if (nullToEmpty(groupId).equals(nullToEmpty(plugin.getGroupId()))
				&& nullToEmpty(artifactId).equals(nullToEmpty(plugin.getArtifactId()))) {
				if (result != null) {
					throw new IllegalStateException(
						"The build contains multiple versions of plugin " + groupId + ":" + artifactId);
				}
				result = plugin;
			}

		}
		return result;
	}

	protected String nullToEmpty(String str) {
		return Optional.ofNullable(str)
			.orElse("");
	}

	protected void processExecutions(List<PluginExecution> bndMavenPluginExecutions, Plugin mavenPackagingPlugin, MavenProject project) {
		bndMavenPluginExecutions.stream()
			.filter(PluginExecutions::isPackagingGoal)
			.forEach(bndMavenPluginEx -> {
				findMatchingMavenPackagingPluginExecution(mavenPackagingPlugin, extractClassifier(bndMavenPluginEx))
					.ifPresent(packagingPluginEx -> {
						mavenPackagingPlugin.getExecutions()
							.remove(packagingPluginEx);

						if (logger.isDebugEnabled()) {
							logger.debug("Disabled execution of " + packagingPluginEx + " in " + project + " by "
								+ THIS_ARTIFACT_ID);
						}
					});
			});
	}

}
