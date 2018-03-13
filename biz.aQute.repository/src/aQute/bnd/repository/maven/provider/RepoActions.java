package aQute.bnd.repository.maven.provider;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.util.promise.Promise;

import aQute.bnd.osgi.Jar;
import aQute.bnd.repository.maven.provider.IndexFile.BundleDescriptor;
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
		map.put("Delete All from Index", new Runnable() {

			@Override
			public void run() {
				try {
					repo.index.remove(bsn);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

		});

		return map;
	}

	Map<String, Runnable> getRevisionActions(final BundleDescriptor bd) throws Exception {
		Map<String, Runnable> map = new LinkedHashMap<>();
		map.put("Clear from Cache", new Runnable() {

			@Override
			public void run() {
				File dir = repo.storage.toLocalFile(bd.archive)
					.getParentFile();
				IO.delete(dir);
			}

		});
		map.put("Delete from Index", new Runnable() {

			@Override
			public void run() {
				try {
					repo.index.remove(bd.archive);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

		});
		map.put("Add Compile Dependencies", new Runnable() {

			@Override
			public void run() {
				try {
					addDependency(bd.archive, MavenScope.compile);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

		});
		map.put("Add Runtime Dependencies", new Runnable() {

			@Override
			public void run() {
				try {
					addDependency(bd.archive, MavenScope.runtime);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

		});

		map.put("Refresh", new Runnable() {

			@Override
			public void run() {
				try {
					repo.index.updateAsync(bd.archive);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

		});

		addUpdate(bd, map);

		addSources(bd, map);

		return map;
	}

	void addSources(final BundleDescriptor bd, Map<String, Runnable> map) throws Exception {
		Promise<File> pBinary = repo.storage.get(bd.archive);
		if (pBinary.getFailure() == null) {
			final File binary = pBinary.getValue();
			final File out = new File(binary.getParentFile(), "+" + binary.getName());
			if (!out.isFile()) {
				Archive a = bd.archive.revision.archive("jar", "sources");
				Promise<File> pSources = repo.storage.get(a);
				if (pSources.getFailure() == null) {
					final File sources = pSources.getValue();
					if (sources.isFile() && sources.length() > 1000) {
						map.put("Add Sources", new Runnable() {
							@Override
							public void run() {
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
							}
						});
						return;
					}
				}
			}
		}

		map.put("-Add Sources", null);
	}

	void addUpdate(final BundleDescriptor bd, Map<String, Runnable> map) throws Exception {
		try {
			Revision rev = bd.archive.revision;
			Program prog = rev.program;

			List<Revision> revisions = repo.storage.getRevisions(prog);
			if (revisions.size() > 1) {
				final Revision last = revisions.get(revisions.size() - 1);

				if (!rev.equals(last)) {
					map.put("Update to " + last, new Runnable() {

						@Override
						public void run() {
							try {
								repo.index.remove(bd.archive);
								repo.index.add(last.archive(bd.archive.extension, bd.archive.classifier));
								addDependency(bd.archive, MavenScope.runtime);
							} catch (Exception e) {
								throw Exceptions.duck(e);
							}
						}

					});
				} else
					map.put("-Update", null);
			}
		} catch (Exception e) {
			map.put("-Update [" + e + "]", null);
		}
	}

	private void addDependency(Archive archive, MavenScope scope) throws Exception {
		IPom pom = repo.storage.getPom(archive.revision);
		Map<Program, Dependency> dependencies = pom.getDependencies(scope, false);
		for (Dependency d : dependencies.values()) {

			BundleDescriptor add = repo.index.add(d.program.version(d.version)
				.archive("jar", null));
			if (d.error != null)
				add.error = d.error;
		}
	}
}
