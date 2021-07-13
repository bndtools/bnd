package aQute.bnd.maven;

import java.util.Objects;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.version.MavenVersion;

public interface MavenCapability extends Capability {

	String	MAVEN_NAMESPACE					= "bnd.maven";
	String	CAPABILITY_GROUPID_ATTRIBUTE	= "maven-groupId";
	String	CAPABILITY_ARTIFACTID_ATTRIBUTE	= "maven-artifactId";
	String	CAPABILITY_VERSION_ATTRIBUTE	= "maven-version";
	String	CAPABILITY_CLASSIFIER_ATTRIBUTE	= "maven-classifier";
	String	CAPABILITY_EXTENSION_ATTRIBUTE	= "maven-extension";
	String	CAPABILITY_REPOSITORY_ATTRIBUTE	= "maven-repository";

	String maven_groupId();

	String maven_artifactId();

	MavenVersion maven_version();

	String maven_classifier();

	String maven_extension();

	String maven_repository();

	static void addMavenCapability(ResourceBuilder rb, String groupId, String artifactId, MavenVersion version,
		String extension, String classifier, String repository) {
		try {
			CapabilityBuilder c = new CapabilityBuilder(MAVEN_NAMESPACE);
			c.addAttribute(CAPABILITY_GROUPID_ATTRIBUTE, Objects.requireNonNull(groupId));
			c.addAttribute(CAPABILITY_ARTIFACTID_ATTRIBUTE, Objects.requireNonNull(artifactId));
			c.addAttribute(CAPABILITY_VERSION_ATTRIBUTE, Objects.requireNonNull(version));
			if (classifier != null) {
				c.addAttribute(CAPABILITY_CLASSIFIER_ATTRIBUTE, classifier);
			}
			if (extension != null) {
				c.addAttribute(CAPABILITY_EXTENSION_ATTRIBUTE, extension);
			}
			if (repository != null) {
				c.addAttribute(CAPABILITY_REPOSITORY_ATTRIBUTE, repository);
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
