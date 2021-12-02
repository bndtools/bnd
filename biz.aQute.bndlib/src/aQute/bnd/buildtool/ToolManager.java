package aQute.bnd.buildtool;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;

/**
 * Install gradle from a repository in this workspace.
 */
public class ToolManager extends Processor {

	final Workspace						workspace;
	final BiConsumer<String, Object[]>	reporter;

	public ToolManager(Workspace workspace, BiConsumer<String, Object[]> reporter) {
		super(workspace);
		this.reporter = reporter;
		use(workspace);
		this.workspace = workspace;
		setBase(workspace.getBase());
	}

	public String install(String url, Map<String, String> parameters, boolean force) {
		HttpClient client = workspace.getPlugin(HttpClient.class);
		try {
			if (client.isOffline()) {
				return "The workspace is off line";
			}

			URI uri = new URI(url);

			progress("URI is %s", uri);

			File file = client.build()
				.maxRedirects(2)
				.useCache()
				.get()
				.go(uri);

			progress("Downloaded %s", file);
			try (Jar jar = new Jar(file)) {

				Optional<String> properties = jar.getResourceNames(s -> s.endsWith("tool.bnd"))
					.findAny();

				if (!properties.isPresent()) {
					return "cannot find 'tool.bnd' in zip: " + jar.getResources()
						.keySet();
				}

				String toolPath = properties.get();
				String prefix = Strings.stripSuffix(properties.get(), "tool.bnd");
				progress("tool path = %s", toolPath);

				UTF8Properties props = new UTF8Properties();
				props.load(jar.getResource(toolPath)
					.openInputStream());
				setProperties(props);

				Instructions copyInstructions = new Instructions(getMergedParameters("-tool"));

				for (Entry<String, Resource> entry : jar.getResources()
					.entrySet()) {
					String path = entry.getKey();
					Resource resource = entry.getValue();

					if (!path.startsWith(prefix)) {
						progress("not included %s", path);
						continue;
					}

					String newPath = Strings.stripPrefix(path, prefix);
					Instruction matcher = copyInstructions.matcher(newPath);
					Attrs attrs = copyInstructions.get(matcher);
					String actions = newPath;

					if (attrs.containsKey("skip")) {
						actions += " skip";
					} else {

						File destination = getFile(newPath);
						if (destination.isFile()) {
							if (!force)
								return "cannot overwrite " + destination;

							actions += " overwrite";
						}

						destination.getParentFile()
							.mkdirs();
						if (attrs.containsKey("macro")) {
							actions += " macro";
							String content = IO.collect(resource.openInputStream());
							content = getReplacer().process(content);
							resource = new EmbeddedResource(content.getBytes(StandardCharsets.UTF_8), 0);
						}
						if (attrs.containsKey("append")) {
							StringBuilder sb = new StringBuilder(IO.collect(resource.openInputStream()));
							sb.append("\n\n# Appended by tool manager at ")
								.append(Instant.now())
								.append("\n\n");
							for (Map.Entry<String, String> e : parameters.entrySet()) {
								sb.append(e.getKey())
									.append(" = ")
									.append(e.getValue())
									.append("\n");
							}
							resource = new EmbeddedResource(sb.toString()
								.getBytes(StandardCharsets.UTF_8), 0);
						}

						IO.copy(resource.openInputStream(), destination);

						if (attrs.containsKey("exec")) {
							actions += " exec";
							Set<PosixFilePermission> permissions = new HashSet<>();
							permissions.add(PosixFilePermission.OWNER_EXECUTE);
							permissions.add(PosixFilePermission.OWNER_WRITE);
							permissions.add(PosixFilePermission.OWNER_READ);
							Files.setPosixFilePermissions(destination.toPath(), permissions);
						}
					}
					progress(actions);
				}
			}
			return null;
		} catch (Exception e) {
			return "failed to install gradle tool: " + e;
		}
	}

	@Override
	public void progress(String format, Object... args) {
		if (reporter != null)
			reporter.accept(format, args);
	}

	@Override
	public String toString() {
		return "ToolManager [workspace=" + workspace.getBase() + "]";
	}

}
