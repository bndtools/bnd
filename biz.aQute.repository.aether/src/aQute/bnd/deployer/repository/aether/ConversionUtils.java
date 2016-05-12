package aQute.bnd.deployer.repository.aether;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;

final class ConversionUtils {

	public static final String JAR_EXTENSION = "jar";

	private ConversionUtils() {}

	public static final Artifact fromBundleJar(Jar jar) throws Exception {
		String groupId;
		String artifactId;

		String bsn = jar.getBsn();

		groupId = jar.getManifest().getMainAttributes().getValue("Maven-GroupId");
		artifactId = jar.getManifest().getMainAttributes().getValue("Maven-ArtifactId");
		if (groupId == null && artifactId == null) {
			int lastDot = bsn.lastIndexOf('.');
			if (lastDot < 0)
				throw new IllegalArgumentException(
						String.format("Cannot split symbolic name '%s' into group ID and artifact ID", bsn));
			if (lastDot == 0)
				throw new IllegalArgumentException("Group ID appears to be empty");
			if (lastDot >= bsn.length() - 1)
				throw new IllegalArgumentException("Artifact ID appear to be empty");

			groupId = bsn.substring(0, lastDot);
			artifactId = bsn.substring(lastDot + 1);
		} else if (groupId != null && artifactId == null) {
			String groupPrefix = groupId + ".";
			if (bsn.startsWith(groupPrefix)) {
				if (bsn.length() <= groupPrefix.length())
					throw new IllegalArgumentException("Artifact ID appears to be empty");
				artifactId = bsn.substring(groupPrefix.length());
			} else {
				artifactId = bsn;
			}
		} else if (groupId == null) {
			throw new IllegalArgumentException("Group ID appears to be empty");
		}

		String versionString = jar.getVersion();
		if (versionString == null)
			versionString = "0";
		else if (!Verifier.isVersion(versionString))
			throw new IllegalArgumentException("Invalid version " + versionString);
		Version version = Version.parseVersion(versionString);

		return new DefaultArtifact(groupId, artifactId, JAR_EXTENSION, new MavenVersion(version).toString());
	}

	public static String maybeMavenCoordsToBsn(String coords) throws IllegalArgumentException {
		int colonPos = coords.indexOf(':');

		if (colonPos < 0)
			// No colons, it's just a plain old bsn
			return coords;
		if (colonPos == 0)
			throw new IllegalArgumentException(String
					.format("Cannot convert Maven coordinates to BSN, group ID appears to be empty: '%s'", coords));

		int artifactIdStart = colonPos + 1;
		if (artifactIdStart >= coords.length())
			throw new IllegalArgumentException(String
					.format("Cannot convert Maven coordinates to BSN, artifact ID appears to be empty: '%s'", coords));

		return String.format("%s.%s", coords.substring(0, colonPos), coords.substring(artifactIdStart));
	}

	public static String[] getGroupAndArtifactForBsn(String bsn) {
		int dotIndex = bsn.lastIndexOf(':');
		if (dotIndex < 0)
			throw new IllegalArgumentException("Cannot split bsn into group and artifact IDs: " + bsn);
		String groupId = bsn.substring(0, dotIndex);
		String artifactId = bsn.substring(dotIndex + 1);

		return new String[] {
				groupId, artifactId
		};
	}

}
