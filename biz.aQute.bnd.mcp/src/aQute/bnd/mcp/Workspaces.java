package aQute.bnd.mcp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.header.Parameters;
import aQute.bnd.result.Result;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.wstemplates.FragmentTemplateEngine;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateInfo;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateUpdater;
import aQute.bnd.wstemplates.FragmentTemplateEngine.Update;
import aQute.bnd.wstemplates.TemplateID;
import aQute.lib.io.IO;

/**
 * Creates and inspects bnd workspaces. Hides the cnf layout, template fragment
 * indexes, fragment requirements, the third-party trust policy and the bndlib
 * workspace lifecycle.
 * <p>
 * Creating is idempotent: an existing workspace is extended, and existing
 * non-empty files are never overwritten but reported as kept.
 */
public final class Workspaces implements AutoCloseable {

	public record Fragment(String name, String id, String description, boolean official, boolean archived,
		List<String> requires) {}

	public record FragmentList(List<Fragment> fragments, Diagnostics diagnostics) implements Diagnostics.Carrier {}

	public record Spec(@Param(value = "Directory of the workspace; created when missing", required = true) String dir,
		@Param("Template fragments to install, by name or id; required fragments are added automatically") List<String> fragments,
		@Param("Extra template index URLs, e.g. https://raw.githubusercontent.com/peterkir/bnd-workspace-template/refs/heads/main/index.bnd") List<String> indexes,
		@Param("Allow fragments not published by the bndtools organisation; they can run code at build time") boolean trustThirdParty) {

		public Spec {
			fragments = fragments == null ? List.of() : List.copyOf(fragments);
			indexes = indexes == null ? List.of() : List.copyOf(indexes);
		}
	}

	public record Info(String dir, List<String> projects, List<String> bndruns, List<String> repositories,
		List<String> installedFragments, List<String> keptFiles, Diagnostics diagnostics)
		implements Diagnostics.Carrier {

		static Info failed(String dir, Diagnostics diagnostics) {
			return new Info(dir, List.of(), List.of(), List.of(), List.of(), List.of(), diagnostics);
		}
	}

	private final List<URI>	defaultIndexes;
	private Workspace		scratch;

	/**
	 * @param defaultIndexes template indexes always consulted, normally
	 *            {@link FragmentTemplateEngine#DEFAULT_INDEX}
	 */
	public Workspaces(List<URI> defaultIndexes) {
		this.defaultIndexes = List.copyOf(defaultIndexes);
	}

	/**
	 * List the fragments of the default indexes plus the given ones. An
	 * unreachable index is a warning, not a failure.
	 */
	public synchronized FragmentList fragments(List<String> indexes, boolean includeArchived) {
		Catalog catalog = catalog(scratch(), indexes);
		List<Fragment> fragments = catalog.templates.stream()
			.filter(t -> includeArchived || !t.archived())
			.map(Workspaces::fragment)
			.toList();
		return new FragmentList(fragments, catalog.diagnostics);
	}

	/**
	 * Create the workspace if needed and install the selected fragments. Nothing
	 * is written when a fragment is unknown or needs trust that was not given.
	 */
	public synchronized Info create(Spec spec) {
		Path dir = Path.of(spec.dir())
			.toAbsolutePath()
			.normalize();
		Catalog catalog = catalog(scratch(), spec.indexes());
		Selection selection = catalog.select(spec.fragments());
		Diagnostics diagnostics = catalog.diagnostics.plus(selection.diagnostics);
		if (!selection.diagnostics.ok()) {
			return Info.failed(dir.toString(), diagnostics);
		}
		List<String> untrusted = selection.resolved.stream()
			.filter(t -> !t.isOfficial())
			.map(t -> t.name() + " (" + idOf(t.id()) + ")")
			.toList();
		if (!untrusted.isEmpty() && !spec.trustThirdParty()) {
			return Info.failed(dir.toString(), diagnostics.plus(Diagnostics.error(
				"Third-party fragments can run code during the build and need trustThirdParty=true: %s",
				String.join(", ", untrusted))));
		}

		try {
			ensureCnf(dir);
		} catch (IOException e) {
			return Info.failed(dir.toString(), diagnostics.plus(Diagnostics.error("Cannot create %s: %s", dir, e)));
		}

		List<String> kept = new ArrayList<>();
		if (!selection.selected.isEmpty()) {
			try (Workspace ws = new Workspace(dir.toFile())) {
				FragmentTemplateEngine engine = new FragmentTemplateEngine(ws);
				catalog.templates.forEach(engine::add);
				try (TemplateUpdater updater = engine.updater(dir.toFile(), selection.selected)) {
					keepExistingFiles(dir, updater, kept);
					updater.commit();
				}
			} catch (Exception e) {
				return Info.failed(dir.toString(),
					diagnostics.plus(Diagnostics.error("Installing fragments failed: %s", e)));
			}
		}

		Info info = describe(dir.toString());
		return new Info(info.dir(), info.projects(), info.bndruns(), info.repositories(), selection.resolved.stream()
			.map(TemplateInfo::name)
			.toList(), kept, diagnostics.plus(info.diagnostics()));
	}

	/**
	 * Describe projects, bndrun files, repositories and the workspace problems.
	 * Repositories are initialized, which may download indexes.
	 */
	public synchronized Info describe(String directory) {
		Path dir = Path.of(directory)
			.toAbsolutePath()
			.normalize();
		if (!Files.isRegularFile(dir.resolve(Workspace.CNFDIR)
			.resolve(Workspace.BUILDFILE))) {
			return Info.failed(dir.toString(), Diagnostics.error("Not a bnd workspace (no cnf/build.bnd): %s", dir));
		}
		try (Workspace ws = new Workspace(dir.toFile())) {
			Collection<Project> projects = ws.getAllProjects()
				.stream()
				.filter(p -> !p.isCnf())
				.toList();
			List<String> bndruns = projects.stream()
				.flatMap(p -> bndruns(dir, p.getBase()))
				.sorted()
				.toList();
			List<String> repositories = ws.getRepositories()
				.stream()
				.filter(r -> !(r instanceof WorkspaceRepository))
				.map(RepositoryPlugin::getName)
				.toList();
			return new Info(dir.toString(), projects.stream()
				.map(Project::getName)
				.sorted()
				.toList(), bndruns, repositories, List.of(), List.of(),
				new Diagnostics(ws.getErrors(), ws.getWarnings()));
		} catch (Exception e) {
			return Info.failed(dir.toString(), Diagnostics.error("Cannot open workspace %s: %s", dir, e));
		}
	}

	@Override
	public synchronized void close() {
		if (scratch != null) {
			File base = scratch.getBase();
			IO.close(scratch);
			IO.delete(base);
			scratch = null;
		}
	}

	private static Stream<String> bndruns(Path root, File projectDir) {
		File[] files = projectDir.listFiles((d, name) -> name.endsWith(".bndrun"));
		return Stream.of(files == null ? new File[0] : files)
			.map(f -> root.relativize(f.toPath())
				.toString()
				.replace('\\', '/'));
	}

	/** Existing user content wins; an empty file (e.g. a fresh build.bnd) does not count. */
	private static void keepExistingFiles(Path dir, TemplateUpdater updater, List<String> kept) {
		Map<File, List<Update>> existing = new LinkedHashMap<>();
		updater.updaters()
			.forEach((file, updates) -> {
				if (file.isFile() && file.length() > 0) {
					existing.put(file, List.copyOf(updates));
				}
			});
		existing.forEach((file, updates) -> {
			updates.forEach(updater::remove);
			kept.add(dir.relativize(file.toPath())
				.toString()
				.replace('\\', '/'));
		});
	}

	private static void ensureCnf(Path dir) throws IOException {
		Path cnf = dir.resolve(Workspace.CNFDIR);
		Files.createDirectories(cnf.resolve(Workspace.EXT));
		Path build = cnf.resolve(Workspace.BUILDFILE);
		if (!Files.exists(build)) {
			Files.writeString(build, "");
		}
		Path cnfBnd = cnf.resolve(Project.BNDFILE);
		if (!Files.exists(cnfBnd)) {
			Files.writeString(cnfBnd, "-nobundles: true\n");
		}
	}

	private Workspace scratch() {
		if (scratch == null) {
			try {
				Path dir = Files.createTempDirectory("bnd-mcp-");
				ensureCnf(dir);
				scratch = new Workspace(dir.toFile());
			} catch (Exception e) {
				throw new IllegalStateException("Cannot create scratch workspace", e);
			}
		}
		return scratch;
	}

	private Catalog catalog(Workspace ws, List<String> extraIndexes) {
		FragmentTemplateEngine engine = new FragmentTemplateEngine(ws);
		List<String> warnings = new ArrayList<>();
		Stream.concat(defaultIndexes.stream()
			.map(URI::toString), extraIndexes.stream())
			.distinct()
			.forEach(index -> {
				try {
					Result<List<TemplateInfo>> result = engine.read(URI.create(index)
						.toURL());
					result.accept(list -> list.forEach(engine::add),
						error -> warnings.add("Index " + index + " not readable: " + error));
				} catch (Exception e) {
					warnings.add("Index " + index + " not readable: " + e);
				}
			});
		engine.read(new Parameters(ws.getProperty("-workspace-templates")))
			.forEach(engine::add);
		return new Catalog(engine, engine.getAvailableTemplates(), new Diagnostics(List.of(), warnings));
	}

	private static Fragment fragment(TemplateInfo t) {
		return new Fragment(t.name(), idOf(t.id()), t.description(), t.isOfficial(), t.archived(),
			List.of(t.require()));
	}

	static String idOf(TemplateID id) {
		if (id.other() != null) {
			return id.other();
		}
		String path = id.path()
			.isEmpty() ? "" : "/" + id.path();
		String ref = "master".equals(id.ref()) ? "" : "#" + id.ref();
		return id.organisation() + "/" + id.repository() + path + ref;
	}

	private record Selection(List<TemplateInfo> selected, List<TemplateInfo> resolved, Diagnostics diagnostics) {}

	private record Catalog(FragmentTemplateEngine engine, List<TemplateInfo> templates, Diagnostics diagnostics) {

		Selection select(List<String> wanted) {
			List<TemplateInfo> selected = new ArrayList<>();
			List<String> unknown = new ArrayList<>();
			for (String w : wanted) {
				templates.stream()
					.filter(t -> w.equals(t.name()) || w.equals(idOf(t.id())))
					.findFirst()
					.ifPresentOrElse(selected::add, () -> unknown.add(w));
			}
			if (!unknown.isEmpty()) {
				String available = String.join(", ", templates.stream()
					.map(TemplateInfo::name)
					.toList());
				return new Selection(List.of(), List.of(),
					Diagnostics.error("Unknown fragment(s): %s. Available: %s", String.join(", ", unknown),
						available.isEmpty() ? "none (check the indexes)" : available));
			}
			List<TemplateInfo> resolved = engine.resolveRequirements(selected);
			List<String> warnings = resolved.stream()
				.flatMap(t -> Stream.of(t.require())
					.map(String::trim)
					.filter(r -> resolved.stream()
						.noneMatch(x -> x.id()
							.equals(TemplateID.from(r))))
					.map(r -> "Fragment " + t.name() + " requires " + r + ", which is in no index"))
				.toList();
			return new Selection(selected, resolved, new Diagnostics(List.of(), warnings));
		}
	}
}
