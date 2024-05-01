package aQute.bnd.repository.maven.provider;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.lib.justif.Justif;
import aQute.maven.api.Archive;

/**
 * Functionality originating from MbrCommand and used by RepoActions to allow
 * updating revisions in a MavenBndRepository.
 */
public class MbrUpdater {

	private MavenBndRepository repo;

	public MbrUpdater(MavenBndRepository repo) {
		this.repo = repo;
	}

	public Map<Archive, MavenVersion> calculateUpdateRevisions(MultiMap<Archive, MavenVersion> updates,
		PrintStream out) {
		Map<Archive, MavenVersion> content = new HashMap<>();

		for (Archive archive : new TreeSet<>(repo.getArchives())) {
			List<MavenVersion> list = updates.get(archive);
			if (list == null || list.isEmpty()) {
				content.put(archive, archive.revision.version);
			} else {
				MavenVersion version = list.get(list.size() - 1);

				if (out != null) {
					out.format(" %-70s %20s -> %s%n", archive.getRevision().program, archive.getRevision().version,
						version);
				}
				content.put(archive, version);
			}
		}
		return content;
	}

	public enum Scope {
		micro,
		minor,
		major,
		all
	}

	final static Pattern					SNAPSHOTLIKE_P				= Pattern
		.compile("(-[^.]+)|(.*(beta|alfa|alpha|rc).?\\d+$)", Pattern.CASE_INSENSITIVE);
	final static Predicate<MavenVersion>	notSnapshotlikePredicate	= v -> !SNAPSHOTLIKE_P.matcher(v.toString())
		.find();

	public boolean update(MavenBndRepository repo, Map<Archive, MavenVersion> translations) throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean changes = buildGAVString(sb, repo, translations);
		if (!changes)
			return false;

		repo.getIndexFile()
			.getParentFile()
			.mkdirs();
		// bnd.trace("writing %s", repo.getIndexFile());
		IO.store(sb.toString(), repo.getIndexFile());
		return changes;
	}

	/**
	 * @param sb contains a list of Maven GAVs like in a central.mvn file
	 * @param repo
	 * @param translations
	 * @return <code>true</code> when there were changes / updates, otherwise
	 *         <code>false</code>
	 * @throws IOException
	 */
	public boolean buildGAVString(StringBuilder sb, MavenBndRepository repo, Map<Archive, MavenVersion> translations)
		throws IOException {
		boolean changes = false;
		Iterator<String> lc;
		if (repo.getIndexFile()
			.isFile()) {
			lc = IO.reader(repo.getIndexFile())
				.lines()
				.iterator();
			// bnd.trace("reading %s", repo.getIndexFile());
		} else {
			lc = Collections.emptyIterator();
		}

		for (Iterator<String> i = lc; i.hasNext();) {
			String line = i.next()
				.trim();
			if (!line.startsWith("#") && !line.isEmpty()) {

				Archive archive = Archive.valueOf(line);
				if (archive != null) {
					MavenVersion version = translations.get(archive);
					if (version != null) {
						if (!archive.revision.version.equals(version)) {
							Archive updated = archive.update(version);
							sb.append(updated)
								.append("\n");
							changes = true;
							continue;
						}
					}
				}
			}
			sb.append(line)
				.append("\n");
		}
		return changes;
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
	public String preview(Scope scope, Collection<Archive> archives) throws Exception {
		MultiMap<Archive, MavenVersion> overlap = getUpdates(scope, Collections.singletonList(repo), archives, false);

		return format(overlap);
	}

	private String format(MultiMap<Archive, MavenVersion> overlap) {
		Justif j = new Justif(140, 50, 60, 70, 80, 90, 100, 110);
		j.formatter()
			.format("%n## %60s%n", "Updates available");
		j.table(overlap, "");
		return j.wrap();
	}

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
