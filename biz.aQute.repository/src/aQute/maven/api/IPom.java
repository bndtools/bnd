package aQute.maven.api;

import java.util.Comparator;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersionRange;
import aQute.maven.provider.MavenRepository;

@ProviderType
public interface IPom {
	class Dependency extends DTO {
		public boolean		optional;
		public Program		program;
		public String		version;
		public String		systemPath;
		public MavenScope	scope;
		public String		error;
		public String		type;
		public String		classifier;

		public Archive getArchive() {
			Revision revision = getRevision();
			if (revision == null)
				return null;

			return revision.archive(type, classifier);
		}

		public Revision getRevision() {
			if (version == null)
				return null;

			if (MavenVersionRange.isRange(version))
				throw new IllegalArgumentException("Version is a range, to make a revision you need a version");

			return program.version(version);
		}

		@Override
		public String toString() {
			return String.format("Dependency [program=%s, version=%s, type=%s, classifier=%s, scope=%s, error=%s]",
				program, version, type, classifier, scope, error);
		}

		public void bindToVersion(MavenRepository repo) throws Exception {
			if (MavenVersionRange.isRange(version)) {
				MavenVersionRange range = new MavenVersionRange(version);
				repo.getRevisions(program)
					.stream()
					.filter(r -> range.includes(r.version))
					.max(Comparator.naturalOrder())
					.ifPresent(highest -> {
						version = highest.version.toString();
					});
			}
		}
	}

	Revision getRevision();

	IPom getParent();

	String getPackaging();

	Archive binaryArchive();

	Map<Program, Dependency> getDependencies(MavenScope scope, boolean transitive) throws Exception;

	boolean hasValidGAV();
}
