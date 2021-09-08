package aQute.bnd.build;

import java.io.File;
import java.util.Map.Entry;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.build.Workspace.ResourceRepositoryStrategy;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.result.Result;
import aQute.bnd.service.library.LibraryNamespace;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter.SetLocation;

/**
 * Implements the `-library` instruction. This will make it possible to include
 * bnd files and binaries from bundles in the repo or file system. This is
 * described in library.md.
 */
class LibraryHandler implements AutoCloseable {

	/*
	 * Base class for library handling. We have a FileLibrary and a RepoLibrary
	 */
	abstract class Library {

		/*
		 * Each 'type' of processor has a different default for its include
		 * file. This default can be overwritten with the `include` property on
		 * the library capability.
		 */
		String getDefault(Processor p) {
			if (p instanceof Workspace) {
				return "workspace.bnd";
			}
			if (p instanceof Run) {
				return "bndrun.bnd";
			}
			if (p instanceof Project) {
				return "project.bnd";
			}
			return "include.bnd";
		}

		/*
		 * Include a file in a processor, processor is a Project, Workspace, or
		 * Bndrun
		 */
		void doInclude(Processor p, File in, String header, String name, Object resource) {

			if (!in.exists()) {
				error(p, header, name, "Cannot include %s from library %s in resource %s", in, name, resource);
				return;
			}
			if (in.isDirectory()) {
				for (File sub : in.listFiles()) {
					if (sub.getName()
						.endsWith(".bnd")) {
						doInclude(p, sub, header, name, resource);
					}
				}
				return;
			}

			if (in.isFile())
				try {
					p.doIncludeFile(in, false, p.getProperties());
					return;
				} catch (Exception e) {
					error(p, header, name, "Failed to include %s from library %s in resource %s", in, name, resource);
					return;
				}
			error(p, header, name, "No cached expansion for %s", resource);
		}

		abstract void process(Processor p, Attrs attrs, String header);
	}

	/*
	 * Represents a library with version=file. The name is either a path to a
	 * directory or a JAR.
	 */
	class FileLibrary extends Library {

		final File dir;

		public FileLibrary(File dir) {
			this.dir = dir;
		}

		@Override
		void process(Processor p, Attrs attrs, String header) {
			String path = attrs.getOrDefault("include", getDefault(p));
			File incl = IO.getFile(dir, path);
			doInclude(p, incl, header, dir.getName(), dir);
		}

	}

	/*
	 * Represents a library in the repositories in the workspace. One bundle can
	 * contain many libraries. Each library is represented by a {@link
	 * LibraryNamespace} capability. Special object since we need to compare
	 * them. Notice comparison will sort with the HIGHEST version first in the
	 * sorted set so we can use 'first' after the stream sort.
	 */
	class RepoLibrary extends Library implements Comparable<RepoLibrary> {
		final String	where;
		final Resource	resource;
		final String	name;
		final Version	version;

		RepoLibrary(String name, Capability cap) {
			this.name = name;
			this.where = (String) cap.getAttributes()
				.getOrDefault(LibraryNamespace.CAPABILITY_PATH_ATTRIBUTE, "library");
			version = ResourceUtils.getVersion(cap);
			this.resource = cap.getResource();
		}

		@Override
		void process(Processor p, Attrs attrs, String header) {

			Result<File> cache = ws.getExpandedInCache(resource);
			if (cache.isErr()) {
				error(p, header, name, cache.error()
					.get());
				return;
			}

			File root = IO.getFile(cache.unwrap(), this.where);
			if (!root.isDirectory()) {
				error(p, header, name, "No cached expansion for library %s in resource %s", name, resource);
				return;
			}

			String path = attrs.getOrDefault("include", getDefault(p));
			File incl = IO.getFile(root, path);

			doInclude(p, incl, header, name, resource);
		}

		/*
		 * compare reverse so that the highest version is the first in the
		 * sorted list
		 */
		@Override
		public int compareTo(RepoLibrary o) {
			return o.version.compareTo(this.version);
		}
	}

	final Workspace ws;

	LibraryHandler(Workspace ws) {
		try {
			this.ws = ws;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/*
	 * Called from workspace to update the processor
	 */
	void update(Processor p, String library, String header) {

		Parameters es = new Parameters(library);

		for (Entry<String, Attrs> entry : es.entrySet())
			try {
				String name = Processor.removeDuplicateMarker(entry.getKey());
				boolean ignoreErrors = false;
				if (name.startsWith("-")) {
					name = name.substring(1);
					ignoreErrors = true;
				}

				String versionString = entry.getValue()
					.getOrDefault(LibraryNamespace.CAPABILITY_VERSION_ATTRIBUTE, null);

				Library we;
				if ("file".equals(versionString)) {
					File dir = p.getFile(name);
					if (!dir.isDirectory()) {
						Result<File> result = ws.getExpandedInCache("urn:" + dir.toURI(), dir);
						if (result.isErr()) {
							error(p, header, name,
								"version = file but the file %s is not a directory nor can it be exanpanded: %s", dir,
								result.error()
									.get());

							continue;
						}
						dir = result.unwrap();
					}
					String where = entry.getValue()
						.getOrDefault("where", null);
					if (where != null) {
						dir = IO.getFile(dir, where);
					}
					we = new FileLibrary(dir);
				} else {
					if (versionString != null && !VersionRange.isOSGiVersionRange(versionString)) {
						error(p, header, name, "Invalid version %s", versionString);
						continue;
					}
					we = getLibrary(name, versionString);
				}

				if (we == null) {
					if (!ignoreErrors)
						error(p, header, name, "No %s for %s-%s", header, name, versionString);
					continue;
				}
				we.process(p, entry.getValue(), header);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void error(Processor p, String header, String clause, String format, Object... args) {
		SetLocation error = p.error(format, args);
		try {
			FileLine fl = p.getHeader(header, clause);
			if (fl != null)
				fl.set(error);
		} catch (Exception e) {
			// ignore
		}
	}

	private RepoLibrary getLibrary(String name, String versionRange) throws Exception {
		String filter = LibraryNamespace.filter(name, versionRange);
		return ws.findProviders(LibraryNamespace.NAMESPACE, filter, ResourceRepositoryStrategy.ALL)
			.map(c -> new RepoLibrary(name, c))
			.sorted()
			.findFirst()
			.orElse(null);
	}

	@Override
	public void close() throws Exception {
		// leave it, good practice, one day we need it.
	}
}
