package aQute.bnd.repository.maven.provider;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.util.promise.Promise;

import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.maven.api.Archive;
import aQute.maven.api.IPom;
import aQute.maven.api.IPom.Dependency;
import aQute.maven.api.MavenScope;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;

class RepoActions {

	private MavenBndRepository repo;

	RepoActions(MavenBndRepository mavenBndRepository) {
		this.repo = mavenBndRepository;
	}

	Map<String, Runnable> getProgramActions(final String bsn) throws Exception {
		Map<String, Runnable> map = new LinkedHashMap<>();

		if (bsn.indexOf(':') < 0) {
			map.put("Delete All from Index", () -> {
				try {
					repo.index.remove(bsn);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});
		}
		return map;
	}

	Map<String, Runnable> getRevisionActions(final Archive archive) throws Exception {
		Map<String, Runnable> map = new LinkedHashMap<>();
		map.put("Clear from Cache", () -> {
			File dir = repo.storage.toLocalFile(archive)
				.getParentFile();
			IO.delete(dir);
		});
		map.put("Delete from Index", () -> {
			try {
				repo.index.remove(archive);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		});
		map.put("Add Compile Dependencies", () -> {
			try {
				addDependency(archive, MavenScope.compile);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		});
		map.put("Add Runtime Dependencies", () -> {
			try {
				addDependency(archive, MavenScope.runtime);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		});

		addUpdate(archive, map);

		addSources(archive, map);

		return map;
	}

	void addSources(final Archive archive, Map<String, Runnable> map) throws Exception {
		Promise<File> pBinary = repo.storage.get(archive);
		if (pBinary.getFailure() == null) {
			final File binary = pBinary.getValue();
			final File out = new File(binary.getParentFile(), "+" + binary.getName());
			if (!out.isFile()) {
				Archive a = archive.revision.archive("jar", "sources");
				Promise<File> pSources = repo.storage.get(a);
				if (pSources.getFailure() == null) {
					final File sources = pSources.getValue();
					if (sources.isFile() && sources.length() > 1000) {
						map.put("Add Sources", () -> {
							try {
								try (Jar src = new Jar(sources)) {
									try (Jar bin = new Jar(binary)) {
										bin.setDoNotTouchManifest();
										for (String path : src.getResources()
											.keySet())
											bin.putResource("OSGI-OPT/src/" + path, src.getResource(path));
										bin.write(out);
									}
									out.setLastModified(System.currentTimeMillis());
								}
							} catch (Exception e) {
								throw Exceptions.duck(e);
							}
						});
						return;
					}
				}
			}
		}

		map.put("-Add Sources", null);
	}

	void addUpdate(final Archive archive, Map<String, Runnable> map) throws Exception {
		try {
			Revision rev = archive.revision;

			repo.storage.getRevisions(rev.program)
				.stream()
				.filter(v -> v.compareTo(rev) > 0)
				.collect(groupingBy(v -> {
					Version ov = v.version.getOSGiVersion();
					return new Version(ov.getMajor(), ov.getMinor());
				}, maxBy(naturalOrder())))
				.values()
				.stream()
				.filter(Optional::isPresent)
				.map(Optional::get)
				.sorted()
				.forEachOrdered(candidate -> {
					map.put("Update to " + candidate, () -> {
						try {
							repo.index.remove(archive);
							repo.index.add(candidate.archive(archive.extension, archive.classifier));
							addDependency(archive, MavenScope.runtime);
						} catch (Exception e) {
							throw Exceptions.duck(e);
						}
					});
				});
		} catch (Exception e) {
			map.put("-Update [" + e + "]", null);
		}
	}

	private void addDependency(Archive archive, MavenScope scope) throws Exception {
		IPom pom = repo.storage.getPom(archive.revision);
		Map<Program, Dependency> dependencies = pom.getDependencies(scope, false);
		for (Dependency d : dependencies.values()) {

			repo.index.add(d.program.version(d.version)
				.archive("jar", null));
		}
	}
}
