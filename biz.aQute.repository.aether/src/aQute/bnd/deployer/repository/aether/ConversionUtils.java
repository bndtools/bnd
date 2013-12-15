package aQute.bnd.deployer.repository.aether;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.Version;

final class ConversionUtils {

	public static final String JAR_EXTENSION = "jar";
	
	private ConversionUtils() {}

	public static final Artifact fromBundleJar(Jar jar) throws Exception {
		String groupId;
		String artifactId;

		String bsn = jar.getBsn();

		groupId = jar.getManifest().getMainAttributes().getValue("Maven-GroupId");
		if (groupId != null) {
			String groupPrefix = groupId + ".";
			if (bsn.startsWith(groupPrefix)) {
				if (bsn.length() <= groupPrefix.length())
					throw new IllegalArgumentException("Artifact ID appears to be empty");
				artifactId = bsn.substring(groupPrefix.length());
			} else {
				artifactId = bsn;
			}
		} else {
			int lastDot = bsn.lastIndexOf('.');
			if (lastDot < 0)
				throw new IllegalArgumentException(String.format("Cannot split symbolic name '%s' into group ID and artifact ID", bsn));
			if (lastDot == 0)
				throw new IllegalArgumentException("Group ID appears to be empty");
			if (lastDot >= bsn.length() -1)
				throw new IllegalArgumentException("Artifact ID appear to be empty");
			
			groupId = bsn.substring(0, lastDot);
			artifactId = bsn.substring(lastDot + 1);
		}

		String versionString = jar.getVersion();
		if (versionString == null)
			versionString = "0";
		else if (!Verifier.isVersion(versionString))
			throw new IllegalArgumentException("Invalid version " + versionString);
		Version version = Version.parseVersion(versionString);
		
		return new DefaultArtifact(groupId, artifactId, JAR_EXTENSION, new MvnVersion(version).toString());
	}

	public static String maybeMavenCoordsToBsn(String coords) throws IllegalArgumentException {
		int colonPos = coords.indexOf(':');

		if (colonPos < 0)
			// No colons, it's just a plain old bsn
			return coords;
		if (colonPos == 0)
			throw new IllegalArgumentException(String.format("Cannot convert Maven coordinates to BSN, group ID appears to be empty: '%s'", coords));

		int artifactIdStart = colonPos + 1;
		if (artifactIdStart >= coords.length())
			throw new IllegalArgumentException(String.format("Cannot convert Maven coordinates to BSN, artifact ID appears to be empty: '%s'", coords));

		return String.format("%s.%s", coords.substring(0, colonPos), coords.substring(artifactIdStart));
	}

}
