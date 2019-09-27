package aQute.bnd.remoteworkspace.server;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.bnd.service.specifications.RunSpecification;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.aspects.Aspects;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.link.Link;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.glob.Glob;

/**
 * Implements an RPC interface to a workspace. When a workspace is created then
 * it can create a Remote Workspace Server to allow remote access.
 * <p>
 * This server will register the ephemeral port it uses in the
 * {@code cnf/cache/remotews} directory so that it can be found by clients. This
 * registration is deleted when the process properly exits.
 */
public class RemoteWorkspaceServer implements Closeable {
	final Logger				logger	= LoggerFactory.getLogger(RemoteWorkspaceServer.class);
	final Closeable				server;
	final File					remotewsPort;
	final Workspace				workspace;
	final ScheduledFuture<?>	registerPort;
	private long				startingTime;

	/**
	 * Create a new Remote Workspace Server. This will create a server socket on
	 * a random port. The protocole over this socket is defined by the
	 * {@link RemoteWorkspace} interface. The port number will be registered in
	 * {@code cnf/cache/remotews/<portnr>}. That is, it is possible to have
	 * multiple workspaces open on the system workspace. (Locking between
	 * workspaces is not handled though.)
	 *
	 * @param workspace the given workspace
	 */
	public RemoteWorkspaceServer(Workspace workspace) throws UnknownHostException, IOException {
		this.workspace = workspace;
		ServerSocket server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());

		RemoteWorkspace workspaceLocker = Aspects.intercept(RemoteWorkspace.class, new Instance())
			.around((inv, c) -> {
				return workspace.readLocked(() -> {
					try {
						return c.call();
					} catch (Throwable e) {
						throw Exceptions.duck(e);
					}
				});
			})
			.build();

		this.server = Link.server("remotews", RemoteWorkspaceClient.class, server, (l) -> workspaceLocker, true,
			Processor.getExecutor());

		File remotews = RemoteWorkspaceClientFactory.getPortDirectory(workspace.getBase(), workspace.getBase());

		remotews.mkdirs();
		if (!remotews.isDirectory())
			throw new IllegalStateException(
				"Cannot create the remote workspace directory with port numbers " + remotews);

		remotewsPort = new File(remotews, server.getLocalPort() + "");
		remotewsPort.deleteOnExit();
		startingTime = System.currentTimeMillis();
		register();
		registerPort = Processor.getScheduledExecutor()
			.scheduleAtFixedRate(this::register, 2 /* nice for testing */, 5, TimeUnit.SECONDS);
	}

	void register() {
		if (remotewsPort.isFile())
			return;

		try {
			IO.delete(remotewsPort);
			IO.mkdirs(remotewsPort.getParentFile());
			IO.store(remotewsPort.getName(), remotewsPort);
			remotewsPort.setLastModified(startingTime);
			logger.info("Registering remote workspace server {}", remotewsPort);
		} catch (IOException e) {
			logger.warn("Cannot open remote workspace server {} {}", remotewsPort, e);
		}
	}

	/**
	 * Close the server. This generally happens when the corresponding workspace
	 * is closed. It will release the ephemeral port and delete the registration
	 * file.
	 */
	@Override
	public void close() throws IOException {
		logger.info("Closing remote workspace server {}", remotewsPort);
		registerPort.cancel(false);
		IO.delete(remotewsPort);
		server.close();
	}

	/**
	 * Holds the implementations of the {@link RemoteWorkspace}
	 */
	class Instance implements RemoteWorkspace {
		@Override
		public String getBndVersion() {
			return About.CURRENT.toString();
		}

		@Override
		public RunSpecification getRun(String pathToBndOrBndrun) {
			try {
				File file = new File(pathToBndOrBndrun);

				if (file.isDirectory()) {
					Project project = workspace.getProjectFromFile(file);
					if (project == null)
						throw new IllegalArgumentException(
							"No such project " + pathToBndOrBndrun + " in workspace " + workspace);

					return project.getSpecification();

				} else {
					try (Run run = new Run(workspace, file)) {
						return run.getSpecification();
					}
				}
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		@Override
		public List<String> getLatestBundles(String projectDir, String specification) {
			try {
				Project project = getProject(projectDir);
				return Container.toPaths(null, project.getBundles(Strategy.HIGHEST, specification, "remote"));
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		@Override
		public RunSpecification analyzeTestSetup(String projectDir) {
			try {
				Project project = getProject(projectDir);
				RunSpecification r = project.getSpecification();

				try (Builder a = new Builder()) {
					a.setJar(project.getTestOutput());
					a.setConditionalPackage("!java.*,*");
					a.setProperty(Constants.EXPORT_CONTENTS, "*");

					a.addClasspath(project.getOutput());

					for (Container c : project.getTestpath()) {
						if (c.getError() != null)
							r.errors.add("Not a valid testpath entry " + c + " " + c.getError());
						else
							a.addClasspath(c.getFile());
					}

					for (Container c : project.getBuildpath()) {
						if (c.getError() != null)
							r.errors.add("Not a valid buildpath entry " + c + " " + c.getError());
						else
							a.addClasspath(c.getFile());
					}

					a.build();
					r.errors.addAll(a.getErrors());

					String clauses = Processor.printClauses(a.getExports());

					Parameters extraPackages = new Parameters(clauses);

					r.extraSystemPackages.putAll(extraPackages.toBasic());
					return r;
				}
			} catch (Throwable e) {
				throw Exceptions.duck(e);
			}
		}

		@Override
		public byte[] build(String projectPath, BuilderSpecification spec) {
			try {
				File projectDir = new File(projectPath);
				Project project = workspace.getProjectFromFile(projectDir);
				if (project == null)
					throw new IllegalArgumentException("No such project " + projectPath);

				try (Builder builder = getBuilder(project, spec.parent)) {

					builder.setBase(project.getBase());

					Jar build = builder.from(spec)
						.build();

					if (!builder.isOk()) {
						throw new IllegalStateException(builder.getErrors()
							.stream()
							.collect(Collectors.joining("\n")));
					}

					if (spec.testBundle != null) {
						File[] buildFiles = project.getBuildFiles(false);
						if (buildFiles == null)
							throw new IllegalStateException(projectPath + ": merge requested but has no build JAR");

						Glob glob = new Glob(spec.testBundle);
						boolean found = false;
						buildfiles: for (File f : buildFiles) {
							if (glob.matches(f.getName())) {
								found = true;

								Jar jar = new Jar(buildFiles[0]);
								builder.addClose(jar);
								jar.addAll(build);
								Manifest manifest = jar.getManifest();
								manifest.getMainAttributes()
									.putValue("DynamicImport-Package", "*");
								merge(manifest, build.getManifest(), Constants.SERVICE_COMPONENT);
								jar.setManifest(manifest);
								build = jar;
								break buildfiles;
							}
						}
					}
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					build.write(bout);
					return bout.toByteArray();
				}
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		private void merge(Manifest a, Manifest b, String hdr) {
			if (a == null)
				return;

			if (b == null)
				return;

			String aa = a.getMainAttributes()
				.getValue(hdr);
			String bb = b.getMainAttributes()
				.getValue(hdr);
			if (aa == null && bb == null)
				return;

			if (bb == null)
				return;

			if (aa == null) {
				a.getMainAttributes()
					.putValue(hdr, bb);
				return;
			}

			String cc = aa + "," + bb;
			a.getMainAttributes()
				.putValue(hdr, cc);
		}

		/**
		 * Calculate the builder with the properly inheritance structure.
		 *
		 * @param project The context project
		 * @param parent The comma separated parent string. The last entry may
		 *            be either PROJECT or WORKSPACE
		 */
		private Builder getBuilder(Project project, List<String> parent) throws Exception {

			if (parent == null || parent.isEmpty())
				return new Builder();

			Builder builder = new Builder();
			builder.setBase(project.getBase());

			List<String> paths = new ArrayList<>(parent);
			String last = paths.get(paths.size() - 1);

			boolean workspaceParent = BuilderSpecification.WORKSPACE.equals(last);
			boolean projectParent = BuilderSpecification.PROJECT.equals(last);
			if (workspaceParent || projectParent) {

				paths.remove(paths.size() - 1);

				if (projectParent)
					builder.setParent(project);
				else if (workspaceParent)
					builder.setParent(project.getWorkspace());
			}

			workspaceParent = paths.remove(BuilderSpecification.WORKSPACE);
			projectParent = paths.remove(BuilderSpecification.PROJECT);

			if (workspaceParent || projectParent) {
				builder.error("PROJECT or WORKSPACE parent can only be specified as the last entry");
			}

			// earlier entries are prioritized
			Collections.reverse(paths);

			paths.forEach(path -> {
				File file = new File(path);
				try {
					if (file.isFile()) {
						UTF8Properties p = new UTF8Properties();
						p.load(file, builder);
						builder.setProperties(p);
					} else
						builder.error("specified file %s as parent for build but no such file exist", file.toString());
				} catch (Exception e) {
					builder.exception(e, "Reading properties %s", file);
				}
			});
			return builder;
		}

		void doPackage(Parameters extraPackages, PackageRef p, Attrs a) {
			Attrs attrs = new Attrs(a);

			String v = attrs.getVersion();

			if (v != null) {
				VersionRange vr = VersionRange.parseOSGiVersionRange(v);
				Version version = vr.getLow();
				attrs.put("version", version.toString());
			}

			extraPackages.put(p.getFQN(), attrs);
		}

		Project getProject(String projectDir) {
			File dir = new File(projectDir);
			if (!dir.isDirectory())
				throw new IllegalArgumentException("Not a directory " + projectDir);

			Project project = workspace.getProjectFromFile(dir);
			if (project == null || !project.isValid())
				throw new IllegalArgumentException("Not a valid project directory " + projectDir);
			return project;
		}

		@Override
		public List<String> getProjects() {
			try {
				return workspace.getAllProjects()
					.stream()
					.map(p -> p.toString())
					.collect(Collectors.toList());
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		@Override
		public void close() throws IOException {}
	}
}
