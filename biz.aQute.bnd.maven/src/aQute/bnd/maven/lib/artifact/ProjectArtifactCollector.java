package aQute.bnd.maven.lib.artifact;

import static aQute.bnd.maven.lib.executions.PluginExecutions.extractClassifier;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import aQute.bnd.maven.lib.executions.PluginExecutions;

public class ProjectArtifactCollector {

	public Set<Artifact> collect(final MavenProject mavenProject) {
		final Build build = mavenProject.getBuild();
		final String targetDir = build.getDirectory();
		final String finalName = build.getFinalName();

		return Stream.of(mavenProject.getPlugin("biz.aQute.bnd:bnd-maven-plugin"),
			mavenProject.getPlugin("org.apache.maven.plugins:maven-jar-plugin"),
			mavenProject.getPlugin("org.apache.maven.plugins:maven-war-plugin"))
			.filter(Objects::nonNull)
			.map(Plugin::getExecutions)
			.flatMap(Collection::stream)
			.filter(PluginExecutions::isPackagingGoal)
			.map(ex -> getArtifactFromConfiguration(mavenProject, targetDir, finalName, extractClassifier(ex)))
			.filter(Objects::nonNull)
			.collect(toSet());
	}

	private Artifact getArtifactFromConfiguration(MavenProject mavenProject, String targetDir, String finalName,
		String classifier) {

		StringBuilder fileName = new StringBuilder(finalName);
		if (!classifier.isEmpty()) {
			fileName.append('-')
				.append(classifier);
		}
		fileName.append('.')
			.append(mavenProject.getPackaging());

		File file = new File(targetDir, fileName.toString());
		if (file.exists()) {
			return new DefaultArtifact(mavenProject.getGroupId(), mavenProject.getArtifactId(), classifier,
				mavenProject.getPackaging(), mavenProject.getVersion(),
				Collections.singletonMap("from", mavenProject.toString()), file);
		}

		return null;
	}

}
