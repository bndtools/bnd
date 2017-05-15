package aQute.maven.api;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersionRange;
import aQute.maven.provider.MavenRepository;

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
				List<Revision> revisions = repo.getRevisions(program);

				for (Iterator<Revision> it = revisions.iterator(); it.hasNext();) {
					Revision r = it.next();
					if (!range.includes(r.version))
						it.remove();
				}

				if (!revisions.isEmpty()) {

					Collections.sort(revisions);
					Revision highest = revisions.get(revisions.size() - 1);
					version = highest.version.toString();
				}
			}
		}
	}

	Revision getRevision();

	IPom getParent();

	String getPackaging();

	Archive binaryArchive();

	Map<Program,Dependency> getDependencies(MavenScope scope, boolean transitive) throws Exception;
}
