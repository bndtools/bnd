package aQute.bnd.maven;

import java.util.Objects;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.version.MavenVersion;
import aQute.lib.exceptions.Exceptions;

public interface MavenCapability extends Capability {

	String MAVEN_NAMESPACE = "bnd.maven";

	String maven_groupId();

	String maven_artifactId();

	MavenVersion maven_version();

	String maven_classifier();

	String maven_repository();

	static void addMavenCapability(ResourceBuilder rb, String groupId, String artifactId, MavenVersion version,
		String classifier, String repository) {
		try {
			CapabilityBuilder c = new CapabilityBuilder(MAVEN_NAMESPACE);
			c.addAttribute("maven-groupId", Objects.requireNonNull(groupId));
			c.addAttribute("maven-artifactId", Objects.requireNonNull(artifactId));
			c.addAttribute("maven-version", Objects.requireNonNull(version));
			if (classifier != null) {
				c.addAttribute("maven-classifier", classifier);
			}
			if (repository != null) {
				c.addAttribute("maven-repository", repository);
			}
			rb.addCapability(c);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	static MavenCapability getMavenCapability(Resource resource) {
		return ResourceUtils.capabilityStream(resource, MAVEN_NAMESPACE, MavenCapability.class)
			.findFirst()
			.orElse(null);
	}

}
