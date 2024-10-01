package aQute.bnd.wstemplates;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * multiple templates, there could be conflicts. The caller can remove
 * {@link Update} objects if they conflict. Otherwise, multiple updates will be
 * concatenated during {@link TemplateUpdater#commit()}
 */
public class FragmentTemplateEngine {
	private static final String	TAG					= "tag";
	private static final String	REQUIRE				= "require";
	private static final String	DESCRIPTION			= "description";
	private static final String	WORKSPACE_TEMPLATES	= "-workspace-templates";
	private static final String	NAME				= "name";

	final static Logger			log					= LoggerFactory.getLogger(FragmentTemplateEngine.class);
	final List<TemplateInfo>	templates			= new ArrayList<>();
	final HttpClient			httpClient;
	final Workspace				workspace;

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

	public record TemplateInfo(TemplateID id, String name, String description, String[] require,
		String... tag)
		implements Comparable<TemplateInfo> {

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
			return id.organisation()
				.equals("bndtools");
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
			String require[] = toArray(attrs.get(REQUIRE));
			String tags[] = toArray(attrs.get(TAG));

			templates.add(new TemplateInfo(templateId, name, description, require, tags));
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
		final List<TemplateInfo>	templates;
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
			updates.remove(update.to, update);
			return this;
		}

		/**
		 * Commit the updates
		 */
		public void commit() {
			try (Processor processor = new Processor(workspace)) {
				updates.forEach((k, us) -> {
					if (us.isEmpty())
						return;
					k.getParentFile()
						.mkdirs();
					try (FileOutputStream fout = new FileOutputStream(k)) {
						for (Update r : us) {
							if (r.actions.contains(Action.delete)) {
								IO.delete(r.to);
							}
							if (r.actions.contains(Action.preprocess)) {
								String s = IO.collect(r.from.openInputStream());
								String preprocessed = processor.getReplacer()
									.process(s);
								fout.write(preprocessed.getBytes(StandardCharsets.UTF_8));
							} else {
								IO.copy(r.from.openInputStream(), fout);
							}
							if (r.actions.contains(Action.exec)) {
								k.setExecutable(true);
							}
						}

					} catch (Exception e) {
						throw Exceptions.duck(e);
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

		List<Update> make(TemplateInfo template) {

			TemplateID id = template.id();

			Jar jar = getFiles(id
				.uri());
			closeables.add(jar);

			String prefix = fixup(id
				.path());

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
		return new TemplateUpdater(folder, templates);
	}

	String[] toArray(String string) {
		if (string == null || string.isBlank())
			return new String[0];

		return Strings.split(string)
			.toArray(String[]::new);
	}

}
