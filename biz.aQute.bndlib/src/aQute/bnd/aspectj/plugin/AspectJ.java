package aQute.bnd.aspectj.plugin;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.WriteResource;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.export.Exporter;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.command.Command;
import aQute.libg.glob.Glob;
import aQute.libg.tuple.Pair;
import aQute.service.reporter.Reporter.SetLocation;

/**
 * Provides an aspectj plugin that can export an executable JAR while weaving in
 * aspect bundles.
 */

@BndPlugin(hide = false, name = "AspectJ")
public class AspectJ implements Exporter {
	private static Logger		log							= LoggerFactory.getLogger(AspectJ.class);
	private static Pattern		AJC_WARNING_P				= Pattern.compile("^(.*) \\[warning\\] (.*)$",
		Pattern.MULTILINE);
	private static Pattern		AJC_ERROR_P					= Pattern.compile("^(.*) \\[error\\] (.*)$",
		Pattern.MULTILINE);

	private static final String	TYPE						= "bnd.executablejar.aspectj";
	private static final String	ORG_ASPECTJ_ASPECTJTOOLS	= "org.aspectj:aspectjtools";
	private static final String	ORG_ASPECTJ_ASPECTJRT		= "org.aspectj:aspectjrt";

	@Override
	public String[] getTypes() {
		return new String[] {
			TYPE
		};
	}

	@Override
	public Entry<String, Resource> export(String type, Project project, Map<String, String> options) throws Exception {
		Workspace workspace = project.getWorkspace();
		List<Closeable> closeable = new ArrayList<>();
		List<File> aspectpath = new ArrayList<>();
		List<File> extraBundles = new ArrayList<>();
		Glob match = new Glob(options.getOrDefault("match", "*"));

		Pair<File, Version> acj = get(workspace, ORG_ASPECTJ_ASPECTJTOOLS);
		if (acj == null) {
			project.error("export %s, no %s in repo", project, ORG_ASPECTJ_ASPECTJTOOLS);
			return null;
		}

		Pair<File, Version> aspectjrt = get(workspace, ORG_ASPECTJ_ASPECTJRT);
		if (aspectjrt == null) {
			project.error("export %s, no %s in repo", project, ORG_ASPECTJ_ASPECTJRT);
			return null;
		}

		Entry<String, Resource> entry = project.export("bnd.executablejar.pack", options);
		File tmp = Files.createTempDirectory(entry.getKey())
			.toFile();

		File output = IO.getFile(project.getTarget(), "aspect-" + entry.getKey());

		File aspectjrtBundle = wrap(project, new File(tmp, "biz.aQute.aspectjrt.jar"), aspectjrt);
		if (aspectjrtBundle == null) {
			return null;
		}
		aspectpath.add(aspectjrt.getFirst());
		extraBundles.add(aspectjrtBundle);

		List<File> cp = new ArrayList<>();
		cp.add(aspectjrt.getFirst());
		cp.add(acj.getFirst());

		String aspectPathString = options.getOrDefault("aspectpath", "");
		List<Container> aspectBundles = project.getBundles(Strategy.HIGHEST, aspectPathString, null);
		for (Container c : Container.flatten(aspectBundles)) {
			if (c.isOk()) {
				aspectpath.add(c.getFile());
				File ab = new File(tmp, c.getFile()
					.getName());
				String result = ajc(project, cp, c.getFile(), Collections.emptyList(),
					Collections.singletonList(c.getFile()), ab, options, false);
				if (result != null) {
					project.error("export: %s", result);
					continue;
				}
				extraBundles.add(ab);
			} else {
				project.error("export, dependency error %s", c);
			}
		}

		try (Jar jar = new Jar(entry.getKey(), entry.getValue()
			.openInputStream())) {

			Resource properties = jar.getResource("launcher.properties");
			if (properties == null) {
				project.error("export %s, no launcher.properties found", project);
				return null;
			}

			UTF8Properties launchProperties = new UTF8Properties();
			launchProperties.load(properties.openInputStream());
			String bundles = launchProperties.getProperty("launch.bundles");
			if (bundles == null) {
				project.error("export: no 'launch.bundles' found in launch.properties: ", launchProperties);
				return null;
			}

			Parameters updated = new Parameters(bundles);
			List<String> toWeave = updated.keyList();

			for (File ap : extraBundles) {
				String path = "jar/" + ap.getName();
				jar.putResource(path, new FileResource(ap));
				updated.put(path, new Attrs());
			}

			launchProperties.setProperty("launch.bundles", updated.toString());
			jar.putResource("launcher.properties", propertiesResource(properties, launchProperties));

			for (String path : toWeave) {

				String simple = simple(path);
				if (match.finds(simple) < 0) {
					continue;
				}

				Resource resource = jar.getResource(path);
				if (resource == null) {
					project.error("export: bundle in 'launch.bundles' but not in jar ", path);
					continue;
				}

				File inpath = IO.getFile(tmp, path);
				inpath.getParentFile()
					.mkdirs();
				IO.copy(resource.openInputStream(), inpath);

				Manifest manifest;
				Set<String> resources = new HashSet<>();
				try (Jar in = new Jar(inpath)) {
					manifest = in.getManifest();
					resources.addAll(in.getResources()
						.keySet());
				}

				File outpath = IO.getFile(tmp, path + ".tmp");
				outpath.getParentFile()
					.mkdirs();

				String result = ajc(project, cp, inpath, Collections.emptyList(), aspectpath, outpath, options, true);
				if (result != null) {
					project.error("export: %s", result);
					continue;
				}

				Jar bundle = new Jar(outpath);
				bundle.getResources()
					.keySet()
					.retainAll(resources);

				manifest.getMainAttributes()
					.putValue(Constants.DYNAMICIMPORT_PACKAGE, "*");
				bundle.setManifest(manifest);
				resource = new JarResource(bundle);
				jar.putResource(path, resource);
				closeable.add(bundle);
			}
			if (project.isOk())
				jar.write(output);
		}
		closeable.forEach(c -> IO.close(c));
		IO.delete(tmp);
		if (project.isOk()) {
			FileResource result = new FileResource(output);
			return new AbstractMap.SimpleEntry<>(entry.getKey(), result);
		}
		return null;
	}

	private File wrap(Project project, File aspectjrtBundle, Pair<File, Version> aspectjrt)
		throws Exception, IOException {
		try (Builder b = new Builder()) {
			try (Jar aj = new Jar(aspectjrt.getFirst())) {
				Manifest manifest = new Manifest(aj.getManifest());
				aj.setManifest(new Manifest());
				b.setJar(aj);
				b.setProperty(Constants.BUNDLE_SYMBOLICNAME, "biz.aQute.aspectjrt");
				b.setProperty(Constants.BUNDLE_DESCRIPTION, "Generated from Aspectj runtime");
				b.setProperty(Constants.BUNDLE_VERSION, aspectjrt.getSecond()
					.toString());
				b.setProperty("-exportcontents", "*;version='" + aspectjrt.getSecond() + "'");
				Jar jar = b.build();
				if (!b.isOk()) {
					project.getInfo(b);
					return null;
				}
				Manifest newer = jar.getManifest();
				for (Entry<String, Attributes> e : manifest.getEntries()
					.entrySet()) {
					newer.getEntries()
						.put(e.getKey(), e.getValue());
				}
				jar.write(aspectjrtBundle);
			}
		}
		return aspectjrtBundle;
	}

	private WriteResource propertiesResource(Resource properties, UTF8Properties launchProperties) {
		return new WriteResource() {

			@Override
			public void write(OutputStream out) throws Exception {
				launchProperties.store(out);
			}

			@Override
			public long lastModified() {
				return properties.lastModified();
			}
		};
	}

	private String simple(String path) {
		int n = path.lastIndexOf('/');
		if (n < 0)
			return path;
		return path.substring(n + 1);
	}

	private Pair<File, Version> get(Workspace workspace, String bsn) throws Exception {
		for (RepositoryPlugin rp : workspace.getPlugins(RepositoryPlugin.class)) {
			SortedSet<Version> list = rp.versions(bsn);
			if (list.isEmpty())
				continue;

			return new Pair<>(rp.get(bsn, list.last(), null), list.last());
		}
		return null;
	}

	private String ajc(Project project, List<File> cp, File inpath, List<File> sourceroots, List<File> aspectpath,
		File outpath, Map<String, String> options, boolean warnings) {
		Command acjt = new Command();
		acjt.add(IO.getJavaExecutablePath("java"));
		acjt.add("-classpath");
		acjt.add(path(cp));
		acjt.add("org.aspectj.tools.ajc.Main");
		acjt.add("-inpath");
		acjt.add(inpath.getAbsolutePath());
		acjt.add("-sourceroots");
		acjt.add(path(sourceroots));
		acjt.add("-aspectpath");
		acjt.add(path(aspectpath));
		acjt.add("-outjar");
		acjt.add(outpath.getAbsolutePath());
		acjt.setTimeout(2, TimeUnit.MINUTES);

		String source = project.getProperty(Constants.JAVAC_SOURCE,
			options.getOrDefault(Constants.JAVAC_SOURCE, "1.8"));
		String target = project.getProperty(Constants.JAVAC_TARGET,
			options.getOrDefault(Constants.JAVAC_SOURCE, source));
		acjt.add("-source");
		acjt.add(source);
		acjt.add("-target");
		acjt.add(target);

		String coptions = options.get("ajc");
		if (coptions != null) {
			for (String coption : Strings.split(coptions)) {
				acjt.add(coption);
			}
		}

		StringBuffer stdout = new StringBuffer();
		System.out.println(acjt);
		try {
			int n = acjt.execute(stdout, stdout);
			String output = stdout.toString();
			doErrors(project, warnings, output);
			if (n != 0)
				return stdout.toString();
			else {
				System.out.println(stdout);
				return null;
			}
		} catch (Exception e) {
			SetLocation location = project.exception(e, "failed to run acj: %s on %s\n%s", e.getMessage(), acjt,
				stdout);
			setLocation(project, location);
			return stdout.toString();
		} finally {
			project.trace("console output %s", stdout);
		}
	}

	private void doErrors(Project project, boolean warnings, String output) throws Exception {
		if (warnings) {
			Matcher m = AJC_WARNING_P.matcher(output);
			while (m.find()) {
				SetLocation location = project.warning("%s", m.group(2));
				setLocation(project, location);
			}
		}
		Matcher m = AJC_ERROR_P.matcher(output);
		while (m.find()) {
			SetLocation location = project.error("%s", m.group(2));
			setLocation(project, location);
		}
	}

	private void setLocation(Project project, SetLocation location) {
		try {
			Project bnd = project.getWorkspace()
				.getProject(project.getBase()
					.getName());
			if (bnd == null)
				bnd = project;
			FileLine header = bnd.getHeader(Constants.EXPORT, project.getPropertiesFile()
				.getName());
			header.set(location);
		} catch (Exception e) {
			log.error("failed to set location {}", e, e);
		}
	}

	private String path(List<File> paths) {
		return Strings.join(File.pathSeparator, paths);
	}

}
