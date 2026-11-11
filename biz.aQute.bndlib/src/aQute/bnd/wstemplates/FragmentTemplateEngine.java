package aQute.bnd.wstemplates;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.result.Result;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.stream.MapStream;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 * Manages a set of workspace template fragments. A template fragment is a zip
 * file or a Github repo. A template index file (parameters format) can be used
 * to provide the meta information. For example, a file on Github can hold the
 * overview of available templates. This class is optimized to add multiple
 * indexes and provide a unified overview of templates. It is expected that the
 * users will select what templates to apply.
 * <p>
 * Once the set of templates is know, an {@link TemplateUpdater} is created.
 * This takes a folder and analyzes the given templates. Since there can be
 * multiple templates, several of them can contribute the same file.
 * Contributions with identical content collapse into a single write.
 * Contributions with different content are only merged when they are marked
 * {@link Action#append}; at most one contribution per file may lack that
 * marker and it is written first. Anything else is a conflict, see
 * {@link TemplateUpdater#conflicts()}. The caller can remove {@link Update}
 * objects to resolve conflicts, {@link TemplateUpdater#commit()} refuses to
 * write while conflicts remain.
 */
public class FragmentTemplateEngine {

	public static final String		DEFAULT_INDEX		= "https://raw.githubusercontent.com/bndtools/workspace-templates/master/index.bnd";

	private static final String		TAG					= "tag";
	private static final String		REQUIRE				= "require";
	private static final String		DESCRIPTION			= "description";
	private static final String		ARCHIVED			= "archived";
	private static final String		WORKSPACE_TEMPLATES	= "-workspace-templates";
	private static final String		NAME				= "name";
	private static final Pattern	COMMIT_SHA			= Pattern.compile("[a-f0-9]{40}$");

	final static Logger				log					= LoggerFactory.getLogger(FragmentTemplateEngine.class);
	final List<TemplateInfo>		templates			= new ArrayList<>();
	final HttpClient				httpClient;
	final Workspace					workspace;

	/**
	 * The conflict status.
	 */
	public enum UpdateStatus {
		WRITE,
		CONFLICT
	}

	/**
	 * Info about a template, comes from the index files.
	 */

	public record TemplateInfo(TemplateID id, String name, String description, boolean archived, String[] require,
		String... tag)
		implements Comparable<TemplateInfo> {

		public TemplateInfo(TemplateID id, String name, String description, String[] require, String... tag) {
			this(id, name, description, false, require, tag);
		}

		@Override
		public int compareTo(TemplateInfo o) {
			return id.compareTo(o.id);
		}

		/**
		 * @return <code>true</code> if this template is from the github
		 *         bndtools organisation, which we consider "officially provided
		 *         by us".
		 */
		public boolean isOfficial() {
			return "bndtools".equals(id.organisation());
		}

		/**
		 * Check if the id points to a specific commit SHA e.g.
		 * githuborg/myrepo/subfolder/workspace-template#b96e0a8877bad1c68cdc050d5854829253ef63bb
		 * In this case b96e0a8877bad1c68cdc050d5854829253ef63bb would be the
		 * SHA.
		 *
		 * @return <code>true</code> if the repoUrl points to a specific commit
		 *         SHA.
		 */
		public boolean isCommitSHA() {
			String ref = id.ref();
			return ref != null && COMMIT_SHA.matcher(ref)
				.matches();
		}

	}

	public enum Action {
		skip,
		append,
		exec,
		preprocess,
		delete
	}

	/**
	 * A single update operation
	 */
	public record Update(UpdateStatus status, File to, Resource from, Set<Action> actions, TemplateInfo info)
		implements Comparable<Update> {

		@Override
		public int compareTo(Update o) {
			return info.compareTo(o.info());
		}
	}

	/**
	 * Constructor
	 *
	 * @param workspace
	 */
	public FragmentTemplateEngine(Workspace workspace) {
		this.workspace = workspace;
		HttpClient httpClient = workspace.getPlugin(HttpClient.class);
		this.httpClient = httpClient;
	}

	/**
	 * Read a template index from a URL. The result is **not** added to this
	 * class. See {@link #read(String)} for the file's format
	 *
	 * @param url to read.
	 * @return the result
	 */
	public Result<List<TemplateInfo>> read(URL url) {
		try {
			TaggedData index = httpClient.build()
				.asTag()
				.go(url);
			if (index.isOk()) {
				return read(IO.collect(index.getInputStream()));
			} else {
				return Result.err(index.toString());
			}
		} catch (Exception e) {
			return Result.err("failed to read %s: %s", url, e);
		}
	}

	/**
	 * <pre>
	 * Parse the file from the source. The format is:
	 *
	 * * key – see {@link TemplateID}
	 * * name – A human readable name
	 * * description – An optional human readable description
	 * * require – An optional comma separated list of {@link TemplateID} that will be included
	 * * tags – An optional comma separated list of tags
	 * </pre>
	 *
	 * @param source the source (Parameters format)
	 * @return the result.
	 */
	public Result<List<TemplateInfo>> read(String source) {
		try (Processor processor = new Processor(workspace)) {
			processor.setProperties(new StringReader(source));
			processor.setBase(workspace.getBase());
			Parameters ps = new Parameters(processor.getProperty(WORKSPACE_TEMPLATES));
			List<TemplateInfo> templates = read(ps);
			return Result.ok(templates);
		} catch (IOException e1) {
			return Result.err("failed to read source %s", e1);
		}
	}

	/**
	 * Read the templates from a Parameters
	 *
	 * @param ps the parameters
	 * @return the list of template info
	 */
	public List<TemplateInfo> read(Parameters ps) {
		List<TemplateInfo> templates = new ArrayList<>();
		for (Map.Entry<String, Attrs> e : ps.entrySet()) {
			String id = Processor.removeDuplicateMarker(e.getKey());
			Attrs attrs = e.getValue();

			TemplateID templateId = TemplateID.from(id);
			String name = attrs.getOrDefault(NAME, id.toString());
			String description = attrs.getOrDefault(DESCRIPTION, "");
			boolean archived = attrs.containsKey(ARCHIVED)
				&& (attrs.get(ARCHIVED) == null || attrs.get(ARCHIVED)
					.isEmpty() || Processor.isTrue(attrs.get(ARCHIVED)));
			String require[] = toArray(attrs.get(REQUIRE));
			String tags[] = toArray(attrs.get(TAG));

			templates.add(new TemplateInfo(templateId, name, description, archived, require, tags));
		}
		return templates;
	}

	/**
	 * Convenience method. Add a {@link TemplateInfo} to a list of available
	 * templates, see {@link #getAvailableTemplates()} internally maintained.
	 */
	public void add(TemplateInfo info) {
		if (!templates.contains(info)) {
			this.templates.add(info);
		}
	}

	/**
	 * Remove template information from the available templates.
	 *
	 * @param infos template information to remove.
	 */
	public void removeAll(Collection<TemplateInfo> infos) {
		templates.removeAll(infos);
	}

	/**
	 * Get the list of available templates
	 */
	public List<TemplateInfo> getAvailableTemplates() {
		return new ArrayList<TemplateInfo>(templates);
	}

	/**
	 * Used to edit the updates. A TemplateUpdater maintains a list of Update
	 * objects indexed by file they affect. The intention that this structure is
	 * used to resolve any conflicts. Calling {@link #commit()} will then
	 * execute the updates.
	 * <p>
	 * An instance must be closed when no longer used to release the JARs.
	 */
	public class TemplateUpdater implements AutoCloseable {
		private static final String		TOOL_BND	= "tool.bnd";
		final List<TemplateInfo>		templates;
		final File						folder;
		final MultiMap<File, Update>	updates		= new MultiMap<>();
		final List<AutoCloseable>		closeables	= new ArrayList<>();

		TemplateUpdater(File folder, List<TemplateInfo> templates) {
			this.folder = folder;
			this.templates = templates;
			templates.forEach(templ -> {
				make(templ).forEach(u -> updates.add(u.to, u));
			});

		}

		/**
		 * Remove an update
		 */
		public TemplateUpdater remove(Update update) {
			updates.removeValue(update.to, update);
			return this;
		}

		/**
		 * Files that receive different content from more than one template
		 * without an {@link Action#append} marker. Identical content never
		 * conflicts. Resolve by {@link #remove(Update)}; {@link #commit()}
		 * fails while this map is not empty.
		 */
		public Map<File, List<Update>> conflicts() {
			Map<File, List<Update>> conflicts = new LinkedHashMap<>();
			updates.forEach((file, us) -> {
				long bases = distinct(us).stream()
					.filter(u -> !u.actions.contains(Action.append))
					.count();
				if (bases > 1)
					conflicts.put(file, new ArrayList<>(us));
			});
			return conflicts;
		}

		/**
		 * Commit the updates
		 *
		 * @throws IllegalStateException when {@link #conflicts()} is not empty
		 */
		public void commit() {
			Map<File, List<Update>> conflicts = conflicts();
			if (!conflicts.isEmpty()) {
				String detail = conflicts.entrySet()
					.stream()
					.map(e -> e.getKey() + " <- " + e.getValue()
						.stream()
						.map(u -> u.info.name())
						.distinct()
						.collect(Collectors.joining(", ")))
					.collect(Collectors.joining("; "));
				throw new IllegalStateException(
					"template fragments write different content to the same file, remove all but one or mark them append in tool.bnd: "
						+ detail);
			}
			try (Processor processor = new Processor(workspace)) {
				updates.forEach((k, us) -> {

					try {
						File canonicalK = k.getCanonicalFile();
						if (!canonicalK.getPath()
							.startsWith(folder.getCanonicalPath())) {
							// Zip Slip attack
							throw new SecurityException("Blocked write outside base folder: " + canonicalK);
						}
					} catch (IOException e) {
						throw Exceptions.duck(e);
					}

					if (us.isEmpty())
						return;

					if (us.stream()
						.anyMatch(u -> u.actions.contains(Action.delete))) {
						IO.delete(k);
					}

					List<Update> writes = distinct(us);
					if (writes.isEmpty())
						return;

					k.getParentFile()
						.mkdirs();
					try (FileOutputStream fout = new FileOutputStream(k)) {
						boolean atLineStart = true;
						for (Update r : writes) {
							byte[] content = content(r);
							if (r.actions.contains(Action.preprocess)) {
								String s = new String(content, StandardCharsets.UTF_8);
								content = processor.getReplacer()
									.process(s)
									.getBytes(StandardCharsets.UTF_8);
							}
							if (content.length == 0)
								continue;
							if (!atLineStart)
								fout.write('\n');
							fout.write(content);
							atLineStart = content[content.length - 1] == '\n';
						}
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
					if (writes.stream()
						.anyMatch(u -> u.actions.contains(Action.exec))) {
						k.setExecutable(true);
					}
				});
			} catch (IOException e1) {
				throw Exceptions.duck(e1);
			}
		}

		/**
		 * Get the current set of updates
		 */
		public Map<File, List<Update>> updaters() {
			return updates;
		}

		/**
		 * Writing contributions for one file: byte-identical duplicates
		 * dropped, delete-only entries dropped, the non-append contribution
		 * first.
		 */
		List<Update> distinct(List<Update> us) {
			List<Update> result = new ArrayList<>();
			List<byte[]> seen = new ArrayList<>();
			for (Update u : us) {
				if (u.from == null)
					continue;
				byte[] content = content(u);
				if (seen.stream()
					.anyMatch(b -> Arrays.equals(b, content)))
					continue;
				seen.add(content);
				result.add(u);
			}
			result.sort(Comparator.comparing(u -> u.actions.contains(Action.append)));
			return result;
		}

		byte[] content(Update u) {
			try {
				return IO.read(u.from.openInputStream());
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		List<Update> make(TemplateInfo template) {

			TemplateID id = template.id();

			Jar jar = getFiles(id.uri());
			closeables.add(jar);

			String prefix = fixup(id.path());

			List<Update> updates = new ArrayList<>();

			Map<String, Resource> resources = MapStream.of(jar.getResources())
				.filterKey(k -> !k.startsWith("META-INF/"))
				.mapKey(k -> adjust(prefix, k))
				.filterKey(Objects::nonNull)
				.collect(MapStream.toMap());

			try (Processor processing = new Processor(workspace)) {
				processing.setBase(folder);
				Resource r = resources.remove(TOOL_BND);
				if (r != null) {
					processing.setProperties(r.openInputStream());
				}
				Instructions copyInstructions = new Instructions(processing.mergeProperties("-tool"));
				Set<Instruction> used = new HashSet<>();
				for (Map.Entry<String, Resource> e : resources.entrySet()) {
					String path = e.getKey();
					Resource resource = e.getValue();

					Instruction matcher = copyInstructions.matcher(path);
					Attrs attrs;
					if (matcher != null) {
						used.add(matcher);

						if (matcher.isNegated())
							continue;
						attrs = copyInstructions.get(matcher);
					} else
						attrs = new Attrs();

					Set<Action> actions = Stream.of(Action.values())
						.filter(action -> attrs.containsKey(action.name()))
						.collect(Collectors.toSet());

					if (actions.contains(Action.skip))
						continue;

					File to = processing.getFile(path);
					UpdateStatus us;
					if (to.isFile())
						us = UpdateStatus.CONFLICT;
					else
						us = UpdateStatus.WRITE;

					Update update = new Update(us, to, resource, actions, template);
					updates.add(update);
				}
				copyInstructions.keySet()
					.removeAll(used);
				copyInstructions.forEach((k, v) -> {
					if (k.isNegated())
						return;
					if (k.isLiteral()) {
						File file = IO.getFile(folder, k.getLiteral());
						if (file.exists()) {
							Update update = new Update(UpdateStatus.CONFLICT, file, null, EnumSet.of(Action.delete),
								template);
							updates.add(update);
						}
					}
				});

			} catch (Exception e) {
				log.error("unexpected exception in templates {}", e, e);
			}
			return updates;
		}

		String fixup(String path) {
			if (path.isEmpty() || path.endsWith("/"))
				return path;
			return path + "/";
		}

		String adjust(String prefix, String resourcePath) {
			int n = resourcePath.indexOf('/');
			if (n < 0) {
				log.error("expected at least one segment at start. Github repos start with `repo-ref/`: {}",
					resourcePath);
				return null;
			}
			String path = resourcePath.substring(n + 1);
			if (!path.startsWith(prefix)) {
				return null;
			}
			return path.substring(prefix.length());
		}

		Jar getFiles(URI uri) {
			try {
				File file = httpClient.build()
					.useCache()
					.go(uri);

				return new Jar(file);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		@Override
		public void close() throws Exception {
			closeables.forEach(IO::close);
		}

	}

	/**
	 * Create a TemplateUpdater
	 */
	public TemplateUpdater updater(File folder, List<TemplateInfo> templates) {
		List<TemplateInfo> resolved = resolveRequirements(templates);
		return new TemplateUpdater(folder, resolved);
	}

	/**
	 * Resolve all required templates recursively. This method processes the
	 * 'require' field of each template and includes all transitively required
	 * templates.
	 *
	 * @param templates the initial list of templates
	 * @return a list containing the original templates plus all required
	 *         templates, with duplicates removed
	 */
	public List<TemplateInfo> resolveRequirements(List<TemplateInfo> templates) {
		Set<TemplateID> seen = new HashSet<>();
		List<TemplateInfo> result = new ArrayList<>();

		for (TemplateInfo template : templates) {
			resolveRequirements(template, seen, result);
		}

		return result;
	}

	/**
	 * Recursively resolve requirements for a single template.
	 *
	 * @param template the template to resolve
	 * @param seen set of already processed template IDs to prevent circular
	 *            dependencies
	 * @param result the accumulated list of templates
	 */
	private void resolveRequirements(TemplateInfo template, Set<TemplateID> seen, List<TemplateInfo> result) {
		// Check if we've already processed this template
		if (seen.contains(template.id())) {
			return;
		}

		seen.add(template.id());

		// First, recursively resolve all required templates
		if (template.require() != null && template.require().length > 0) {
			for (String requiredId : template.require()) {
				TemplateID reqId = TemplateID.from(requiredId.trim());

				// Find the required template in our available templates
				TemplateInfo requiredTemplate = templates.stream()
					.filter(t -> t.id().equals(reqId))
					.findFirst()
					.orElse(null);

				if (requiredTemplate != null) {
					// Recursively resolve this required template
					resolveRequirements(requiredTemplate, seen, result);
				} else {
					log.warn("Required template '{}' for template '{}' not found in available templates",
						requiredId, template.id());
				}
			}
		}

		// Add the current template after its dependencies
		result.add(template);
	}

	String[] toArray(String string) {
		if (string == null || string.isBlank())
			return new String[0];

		return Strings.split(string)
			.toArray(String[]::new);
	}

}
