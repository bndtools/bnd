package biz.aQute.ai.assistant.provider;

import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.lit;
import static aQute.libg.re.Catalog.opt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.maven.provider.MavenBndRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.filechanges.FileChangesPlugin;
import aQute.bnd.service.filechanges.FileChangesPlugin.Marker;
import aQute.bnd.version.Version;
import aQute.lib.collections.ExtList;
import aQute.lib.fileset.FileSet;
import aQute.lib.io.IO;
import aQute.libg.re.Catalog;
import aQute.libg.re.RE;
import biz.aQute.jsonschema.api.Description;
import biz.aQute.openai.assistant.api.Tool;

@Component(immediate = true)
public class BndToolImpl implements Tool {
	final static RE			token			= Catalog.re("[a-zA-Z0-9_\\-\\.]+");
	final static RE			GROUP_P			= g("group", token);
	final static RE			ARTIFACT_P		= g("artifact", token);
	final static RE			PACKAGING_P		= g("packaging", token);
	final static RE			CLASSIFIER_P	= g("classifier", token);
	final static RE			VERSION_P		= g("version", token);
	final static RE			GAV_P			= g(GROUP_P, lit(":"), ARTIFACT_P,
		opt(lit(":"), PACKAGING_P, opt(lit(":"), CLASSIFIER_P)), lit(":"), VERSION_P);

	final Workspace			workspace;
	final Path				base;
	final FileChangesPlugin	fileChanges;
	final BndClient			client;

	@Activate
	public BndToolImpl(@Reference
	Workspace workspace) {
		this.workspace = workspace;
		FileChangesPlugin fileChanges = workspace.getPlugin(FileChangesPlugin.class);
		if (fileChanges == null)
			fileChanges = new FileChangesPlugin() {

				@Override
				public Promise<Map<File, List<Marker>>> refresh(File... files) {
					return Processor.getPromiseFactory()
						.resolved(Collections.emptyMap());
				}
			};
		this.fileChanges = fileChanges;
		this.base = workspace.getBase()
			.toPath();

		HttpClient client = workspace.getPlugin(HttpClient.class);
		this.client = new BndClient(client);
	}

	@Override
	public ToolInstance newInstance() {
		return new ToolInstance(BndTool.class, new BndToolInstance(), null);
	}

	public interface BndTool {
		record BundleId(String bsn, Version version) {}

		//@formatter:off
		@Description("""
		        Find a file. Will recurse the directories to find a file
		        name pattern.
		        For example, finding all java source files in a a project
		        would be like for example, to get the files in project
		        `com.example` call `find( "com.example/src", "*.java")`
		        """)
		List<String> find(
			@Description("The path to the directory where the find will start finding")
			String path,
			@Description("The file name or file name pattern")
			String fileName
		) throws Exception;

		@Description("Return the list of files from the relativePath. Directories paths and in /")
		List<String> ls(
			@Description("The path to the directory where the find will start finding")
			String relativePath) throws Exception;

		@Description("""
			Read a file, e.g. "com.some.project/bnd.bnd"
			will return the bnd file. If the file does not exist, null is returned.
			""")
		String read(
			@Description("The path to a file to read. Always relative from the workspace")
			String relativePathFromWorkspace
		) throws Exception;

		@Description("""
		        Write a file with the given content. Return the
		        error/info/warning markers that the compiler and other tools
		        set on a file. A marker has a
		        type (ERROR,WARNING,...), a message, a line, a start and end""")
		Map<String, List<Marker>> write(
			@Description("The relative path in the workspace to a file to write. Directories will be automatically created")
			String path,
			@Description("new content of the file")
			String content
		) throws Exception;

		@Description("Delete a file or directory in the workspace. Returns true if the file/directory existed and was deleted")
		boolean delete(
			@Description("The path to a file to delete")
			String path);

		@Description("Get any markers on the give file or directory. This is a map from a path of a file to its issues")
		Map<String, List<Marker>> markers(
			@Description("The path to a file/dir to get the markers on")
			String path) throws Exception;

		@Description("""
		        Get a list of bundle symbolic names and versions that are
		        available in the repositories. These dependencies can be
		        used on the -buildpath and -testpath, etc""")
		List<BundleId> dependencies() throws Exception;

		@Description("""
	        Create a new default project in the workspace with the given name""")
		Map<String, List<Marker>> createProject(
			@Description("The name of the project. This is expected to be a Bundle Symbolic Name")
			String projectName) throws Exception;

		@Description("""
		        Remove the project with the given name. Returns true if the
		        project existed""")
		boolean removeProject(
			@Description("The name of the project tor remove")
			String projectName) throws Exception;

		@Description("""
		        List the actual project in this workspace. The order of the
		        projects is their dependency order, the first project
		        depends on nothing""")
		List<String> listProjects() throws Exception;


		@Description("""
	        Add a new GAV dependency. """)
		void addMavenDependency(
			@Description("A Maven GAV (Group:Artifact[:classifier]:Version) format")
			String gav) throws Exception;


		record GAV( String groupId, String artifactId, String packaging, String classifier, String version) {
			@Override
			public String toString() {
				 StringBuilder sb = new StringBuilder();
				 sb.append(groupId).append(":").append(groupId);
				 if ( packaging != null) {
					 sb.append(":").append(packaging);
					 if ( classifier != null) {
						 sb.append(":").append(classifier);
					 }
				 }
				 sb.append(":").append(version);
				 return sb.toString();
			 }
		}

		@Description("""
	        Search Maven Central for dependencies """)
		List<GAV> searchMavenCentral(

			@Description("""
		        The query can use `g:<group>', `a:<artifact>`, `v:<version>`, `p:<packaging>`, `l:<classifier>`, `c:<simple class name>`, `fc:<package>, `1:<sha-1>`
		        For example, "bndlib -g:biz.aQute.bnd v:6.4.0" """)
			String query
			);

		//@formatter:on
	}

	class BndToolInstance implements BndTool {

		@Override
		public List<String> ls(String path) throws IOException {
			File where = getWhere(path);
			if (!where.exists())
				return null;

			return normalize(new ExtList<>(where.listFiles()));
		}

		private List<String> normalize(Collection<File> listFiles) {
			if (listFiles == null)
				return Collections.emptyList();

			return listFiles.stream()
				.map(file -> {
					return base.relativize(file.toPath())
						.toString();
				})
				.toList();
		}

		private File getWhere(String path) throws IOException {
			while (path.startsWith("."))
				path = path.substring(1);
			while (path.startsWith("/") || path.startsWith("\\"))
				path = path.substring(1);
			return IO.getBasedFile(workspace.getBase(), path)
				.getAbsoluteFile();
		}

		@Override
		public String read(String path) throws IOException {
			File where = getWhere(path);
			if (!where.isFile())
				return null;

			return IO.collect(where);
		}

		@Override
		public Map<String, List<Marker>> write(String path, String content) throws Exception {
			File where = getWhere(path);
			if (where.isDirectory()) {
				throw new IllegalArgumentException("the path " + path + " is a directory, to write you need a file");
			}
			where.getParentFile()
				.mkdirs();
			IO.store(content, where);
			Promise<Map<File, List<Marker>>> refresh = fileChanges.refresh(where);
			return toMarkers(refresh);
		}

		@Override
		public List<String> find(String path, String fileName) throws Exception {
			FileSet set = new FileSet(workspace.getFile(path), "**/" + fileName);
			return normalize(set.getFiles());
		}

		@Override
		public boolean delete(String path) {
			try {
				File where = getWhere(path);
				if (!where.isFile())
					return false;
				IO.delete(where);
				return true;
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		@Override
		public Map<String, List<Marker>> markers(String path) throws Exception {
			File where = getWhere(path);
			if (where == null)
				return null;

			Promise<Map<File, List<Marker>>> refresh = fileChanges.refresh(where);
			return toMarkers(refresh);
		}

		public Map<String, List<Marker>> toMarkers(Promise<Map<File, List<Marker>>> refresh)
			throws InvocationTargetException, InterruptedException {
			Map<File, List<Marker>> markers = refresh.timeout(20_000)
				.getValue();
			Map<String, List<Marker>> result = new HashMap<>();
			markers.forEach((k, v) -> {
				result.put(toPath(k), v);
			});
			return result;
		}

		@Override
		public List<BundleId> dependencies() throws Exception {
			List<BundleId> bundleIds = new ArrayList<>();
			List<RepositoryPlugin> repos = workspace.getRepositories();
			repos.add(workspace.getWorkspaceRepository());

			for (RepositoryPlugin r : repos) {
				for (String bsn : r.list(null)) {
					for (aQute.bnd.version.Version v : r.versions(bsn)) {
						BundleId b = new BundleId(bsn, v);
						bundleIds.add(b);
					}
				}
			}
			return bundleIds;
		}

		@Override
		public Map<String, List<Marker>> createProject(String projectName) throws Exception {
			return workspace.writeLocked(() -> {

				Project project = workspace.createProject(projectName);
				return markers(projectName);
			});
		}

		@Override
		public boolean removeProject(String projectName) throws Exception {
			return workspace.writeLocked(() -> {
				Project project = workspace.getProject(projectName);
				if (project == null)
					return false;
				workspace.removeProject(project);
				return true;
			});
		}

		@Override
		public List<String> listProjects() throws Exception {
			return workspace.readLocked(() -> {
				return workspace.getBuildOrder()
					.stream()
					.map(Project::getName)
					.toList();
			});
		}

		private String toPath(File f) {
			Path relativize = base.relativize(f.toPath());
			if (relativize.isAbsolute())
				throw new IllegalArgumentException("File outside the workspace is accessed");
			return relativize.toString();
		}

		@Override
		public void addMavenDependency(String gav) throws Exception {
			if (GAV_P.matches(gav)
				.isEmpty())
				throw new IllegalArgumentException("The GAV " + gav
					+ " is not a valid GAV. The format is G:A:V for the basic GroupId, ArtifactId, and Version. When including packaging and classifier, the syntax extends to G:A:P:V or G:A:P:C:V. Each token is [a-zA-Z0-9_\\-\\.]+ ");
			workspace.getRepositories()
				.stream()
				.map(r -> {
					if (r instanceof MavenBndRepository mbr) {
						return mbr;
					} else
						return null;
				})
				.filter(Objects::nonNull)
				.findAny()
				.ifPresent(mbr -> {
					try {
						workspace.writeLocked(() -> {
							File f = mbr.getIndexFile();
							String s = "";
							if (f.isFile()) {
								s = IO.collect(f);
							}
							if (!s.contains(gav)) {
								s = s.concat("\n")
									.concat(gav);
								IO.store(s, f);
								mbr.refresh();
							}
							Path relativize = base.relativize(f.toPath());
							if (!relativize.isAbsolute()) {
								fileChanges.refresh(f);
							}
							return null;
						});
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
				});
		}

		public record ResponseHeader(int status, int QTime, Map<String, String> params) {}

		public record Document(String id, String g, String a, String latestVersion, String repositoryId, String p,
			long timestamp, int versionCount, List<String> text, List<String> ec) {}

		public record Response(int numFound, int start, List<Document> docs) {}

		public record Spellcheck(List<String> suggestions) {}

		public record JsonResponse(ResponseHeader responseHeader, Response response, Spellcheck spellcheck) {}

		@Override
		public List<GAV> searchMavenCentral(String query) {
			String url = "https://search.maven.org/solrsearch/select?q="
				+ URLEncoder.encode(query, StandardCharsets.UTF_8);
			JsonResponse jsonResponse = client.webrequest(url, "GET", null, null, JsonResponse.class);
			return jsonResponse.response().docs.stream()
				.map(doc -> new GAV(doc.g, doc.a, doc.p, null, doc.latestVersion()))
				.toList();
		}

	}

}
