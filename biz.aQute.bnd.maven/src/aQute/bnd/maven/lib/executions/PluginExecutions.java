package aQute.bnd.maven.lib.executions;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import aQute.bnd.unmodifiable.Lists;

public class PluginExecutions {

	static final List<String> PACKAGING_GOALS = Lists.of("jar", "test-jar", "war");

	private PluginExecutions() {}

	public static String defaultClassifier(PluginExecution pluginExecution) {
		List<String> goals = pluginExecution.getGoals();

		return goals.contains("jar") ? "" : (goals.contains("test-jar") ? "tests" : "");
	}

	public static String extractClassifier(PluginExecution pluginExecution) {
		Xpp3Dom rootConfiguration = (Xpp3Dom) pluginExecution.getConfiguration();

		if (rootConfiguration == null) {
			return defaultClassifier(pluginExecution);
		}

		Xpp3Dom classifierConfiguration = rootConfiguration.getChild("classifier");

		if (classifierConfiguration == null) {
			return defaultClassifier(pluginExecution);
		}

		return Optional.ofNullable(classifierConfiguration.getValue())
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.orElse("");
	}

	public static boolean isPackagingGoal(PluginExecution pluginExecution) {
		return pluginExecution.getGoals()
			.stream()
			.anyMatch(PluginExecutions::isPackagingGoal);
	}

	public static boolean isPackagingGoal(String goal) {
		return PACKAGING_GOALS.contains(goal);
	}

	public static boolean matchesClassifier(PluginExecution pluginExecution, String classifier) {
		return Objects.equals(extractClassifier(pluginExecution), classifier);
	}

}
