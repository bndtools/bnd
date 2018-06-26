package aQute.bnd.eclipse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.component.TagResource;
import aQute.bnd.eclipse.EclipseBuildProperties.Library;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.plugin.git.GitPlugin;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

public class LibPde extends Processor {
	private static DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();
	private static DocumentBuilder			db;

	static {
		try {
			db = dbf.newDocumentBuilder();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private static final String				TODO		= "\n# TODO ";
	final BndConversionPaths				mainSources;
	final BndConversionPaths				mainResources;
	final BndConversionPaths				testSources;
	final BndConversionPaths				testResources;
	final Workspace							workspace;
	String									workingset;
	boolean									clean;

	public LibPde(Workspace ws, File pdeProject) throws IOException {
		super(ws);
		File file = getFile(pdeProject, "build.properties");
		setProperties(file);
		ws.addBasicPlugin(new GitPlugin());

		this.workspace = ws;

		mainSources = new BndConversionPaths(ws, Constants.DEFAULT_PROP_SRC_DIR, "src/main/java", "src=src/main/java");
		mainResources = new BndConversionPaths(ws, Constants.DEFAULT_PROP_RESOURCES_DIR, "src/main/resources", null);
		testSources = new BndConversionPaths(ws, Constants.DEFAULT_PROP_TESTSRC_DIR, "src/test/java",
				"test=src/test/java");
		testResources = new BndConversionPaths(ws, Constants.DEFAULT_PROP_TESTRESOURCES_DIR, "src/test/resources",
				null);
	}

	public String convert(Jar content) throws Exception {
		EclipseBuildProperties ebp = new EclipseBuildProperties(this);

		Library lib = ebp.getLibraries().iterator().next();
		EclipseManifest manifest = lib.getManifest();

		lib.move(content, mainSources, mainResources, testSources, testResources);

		Set<String> sourcePackages = mainSources.getRelative(content.getResources().keySet()).stream().map(s -> {
			int n = s.lastIndexOf("/");
			if (n >= 0) {
				return s.substring(0, n).replace('/', '.');
			} else
				return null;
		}).filter(s -> s != null).//
				distinct().//
				collect(Collectors.toSet());

		String bnd = manifest.toBndFile(sourcePackages, workingset);

		try (Formatter model = new Formatter()) {
			model.format("%s\n", bnd);
			mainSources.update(model);
			testSources.update(model);
		}

		content.putResource("bnd.bnd", new EmbeddedResource(bnd.toString().getBytes(StandardCharsets.UTF_8), 0));

		mainResources.remove(content, "META-INF/MANIFEST.MF");

		lib.removeOutputs(content);

		Set<String> remove = content.getResources().keySet().stream().filter(s -> s.endsWith(".DS_store")).collect(
				Collectors.toSet());
		remove.add("pom.xml");
		content.getResources().keySet().removeAll(remove);

		return manifest.getBsn();
	}

	public Project write() throws Exception {
		try (Jar content = new Jar(getBase())) {

			content.setReporter(this);

			String bsn = convert(content);
			if (clean) {
				File projectDir = workspace.getFile(bsn);
				IO.delete(projectDir);
			}

			Project p = workspace.createProject(bsn);

			content.putResource(".classpath", new TagResource(
					EclipseLifecyclePlugin.toClasspathTag(p, toDoc(content.getResource(".classpath")))));
			content.putResource(".project",
					new TagResource(EclipseLifecyclePlugin.toProjectTag(p, toDoc(content.getResource(".project")))));

			content.expand(p.getBase());
			p.forceRefresh();
			EclipseLifecyclePlugin.updateSettingsJDT(p);

			if (!p.getErrors().isEmpty() || !p.getWarnings().isEmpty()) {
				File bndFile = p.getFile("bnd.bnd");
				String bnd = IO.collect(bndFile);
				bnd = TODO + //
						Strings.join(TODO, p.getErrors()) + //
						Strings.join(TODO, p.getWarnings()) + //
						"\n####\n\n" + bnd;
				IO.store(bnd, bndFile);
				p.forceRefresh();
			}
			p.getInfo(this, bsn + ": ");
			return p;
		}
	}

	private Document toDoc(Resource resource) throws Exception {
		if (resource == null)
			return null;

		return db.parse(resource.openInputStream());
	}

	public void verify() {

	}

	public void clean() {
		this.clean = true;
	}

	public void setWorkingset(String workingset) {
		this.workingset = workingset;
	}
}
