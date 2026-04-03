package aQute.bnd.repository.maven.provider;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.collections.MultiMap;
import aQute.lib.justif.Justif;
import aQute.maven.api.Archive;

/**
 * Functionality originating from MbrCommand and used by RepoActions to allow
 * updating revisions in a MavenBndRepository.
 */
public class MbrUpdater {

	private final static Pattern					SNAPSHOTLIKE_P				= Pattern
		.compile("(-[^.]+)|(.*(beta|alfa|alpha|rc).?\\d+$)", Pattern.CASE_INSENSITIVE);
	private final static Predicate<MavenVersion>	notSnapshotlikePredicate	= v -> !SNAPSHOTLIKE_P
		.matcher(v.toString())
		.find();

	private final MavenBndRepository				repo;

	public MbrUpdater(MavenBndRepository repo) {
		this.repo = repo;
	}

	public enum Scope {
		micro,
		minor,
		major,
		all
	}

	/**
	 * Container for a MavenVersion which may or may not be available from maven
	 * central.
	 */
	public record MavenVersionResult(MavenVersion mavenVersion, boolean mavenVersionAvailable) {}

	public static MultiMap<Archive, MavenVersion> getUpdates(Scope scope, Collection<MavenBndRepository> repos,
		Collection<Archive> archives, boolean snapshotlike) throws Exception {
		MultiMap<Archive, MavenVersion> overlap = new MultiMap<>();

		for (Archive archive : archives) {
			for (MavenBndRepository r : repos) {
				if (r.getArchives()
					.contains(archive)) {
					MavenVersion version = archive.revision.version;
					r.getRevisions(archive.revision.program)
						.stream()
						.map(revision -> revision.version)
						.filter(snapshotlike ? x -> true : notSnapshotlikePredicate)
						.filter(v -> v.compareTo(version) > 0)
						.forEach(v -> {
							overlap.add(archive, v);
						});
				}
			}
		}
		overlap.entrySet()
			.forEach(e -> {
				List<MavenVersion> filtered = filter(e.getValue(), e.getKey().revision.version.getOSGiVersion(), scope);
				e.setValue(filtered);
			});
		return overlap;
	}

	/**
	 * Calculates the new revisions for each archive.
	 *
	 * @param updates
	 * @return a map the revision for each archive.
	 */
	public Map<Archive, MavenVersionResult> calculateUpdateRevisions(MultiMap<Archive, MavenVersion> updates) {
		Map<Archive, MavenVersionResult> content = new LinkedHashMap<>();

		for (Archive archive : new TreeSet<>(repo.getArchives())) {
			List<MavenVersion> list = updates.get(archive);
			if (list == null || list.isEmpty()) {
				content.put(archive, new MavenVersionResult(archive.revision.version, false));
			} else {
				MavenVersion version = list.get(list.size() - 1);
				content.put(archive, new MavenVersionResult(version, true));
			}
		}
		return content;
	}

	/**
	 * For each archive in the index, show the available higher versions. The
	 * result is similar to MbrCommand#_check
	 *
	 * @param scope
	 * @param archives
	 * @return a formatted string with the available updates for each archive.
	 * @throws Exception
	 */
	String preview(Scope scope, Collection<Archive> archives) throws Exception {
		MultiMap<Archive, MavenVersion> overlap = getUpdates(scope, Collections.singletonList(repo), archives, false);
		return format(overlap);
	}

	/**
	 * Updates the repo with the new revisions from the map.
	 *
	 * @param map
	 * @return <code>true</code> if there were changes. <code>false</code>
	 *         otherwise.
	 * @throws IOException
	 */
	public boolean update(Map<Archive, MavenVersionResult> map) throws IOException {
		boolean changes = false;
		for (Map.Entry<Archive, MavenVersionResult> e : map.entrySet()) {
			Archive old = e.getKey();
			MavenVersionResult result = e.getValue();
			if (result.mavenVersionAvailable() && !old.revision.version.equals(result.mavenVersion())) {
				Archive updated = old.update(result.mavenVersion());
				try {
					repo.index.replaceArchive(old, updated);
				} catch (Exception ex) {
					throw new IOException("Failed to update " + old + " to " + result.mavenVersion(), ex);
				}
				changes = true;
			}
		}
		return changes;
	}

	void convertTextXml() throws Exception {
		repo.index.convertTextXml();
	}

	private String format(MultiMap<Archive, MavenVersion> overlap) {
		Justif j = new Justif(140, 50, 60, 70, 80, 90, 100, 110);
		j.formatter()
			.format("%n## %60s%n", "Updates available");
		j.table(overlap, "");
		return j.wrap();
	}

	private static List<MavenVersion> filter(List<MavenVersion> versions, Version current, Scope show) {

		if (versions.isEmpty())
			return versions;

		MavenVersion major = null;
		MavenVersion minor = null;
		MavenVersion micro = null;

		for (MavenVersion v : versions) {
			major = v;
			if (v.getOSGiVersion()
				.getMajor() == current.getMajor()) {
				minor = v;
				if (v.getOSGiVersion()
					.getMinor() == current.getMinor()) {
					micro = v;
				}
			}
		}

		switch (show) {
			default :
			case all :
				return versions;

			case major :
				return Collections.singletonList(major);

			case minor :
				if (minor == null)
					return Collections.emptyList();
				else
					return Collections.singletonList(minor);

			case micro :
				if (micro == null)
					return Collections.emptyList();
				else
					return Collections.singletonList(micro);

		}
	}
}
