package aQute.bnd.remoteworkspace.server;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map.Entry;
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
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
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

public class RemoteWorkspaceServer implements Closeable {
	final Logger	logger	= LoggerFactory.getLogger(RemoteWorkspaceServer.class);
	final Closeable	server;
	final File		remotewsPort;
	final Workspace	workspace;

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

		this.server = Link.server("remotews", RemoteWorkspaceClient.class, server, (l) -> workspaceLocker,
			true, Processor.getExecutor());

		File remotews = RemoteWorkspaceClientFactory.getPortDirectory(workspace.getBase(), workspace.getBase());
		remotews.mkdirs();
		if (!remotews.isDirectory())
			throw new IllegalStateException(
				"Cannot create the remote workspace directory with port numbers " + remotews);

		remotewsPort = new File(remotews, server.getLocalPort() + "");
		IO.store(server.getLocalPort() + "", remotewsPort);
		logger.info("Opening remote workspace server {}", remotewsPort);
		remotewsPort.deleteOnExit();
	}

	@Override
	public void close() throws IOException {
		logger.info("Closing remote workspace server {}", remotewsPort);
		IO.delete(remotewsPort);
		server.close();
	}

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
				try (Analyzer a = new Analyzer()) {
					a.setJar(project.getTestOutput());
					RunSpecification r = project.getSpecification();

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

					a.calcManifest();
					r.errors.addAll(a.getErrors());

					Parameters extraPackages = new Parameters();

					for (Entry<PackageRef, Attrs> e : a.getImports()
						.entrySet()) {

						doPackage(extraPackages, e.getKey(), e.getValue());
					}

					for (Entry<PackageRef, Attrs> e : a.getContained()
						.entrySet()) {
						doPackage(extraPackages, e.getKey(), e.getValue());
					}

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

				try (Builder builder = spec.inherit ? project.getBuilder(null) : new Builder()) {

					Jar build = builder.from(spec)
						.build();

					build.getManifest()
						.write(System.out);
					if (!builder.isOk()) {
						throw new IllegalStateException(builder.getErrors()
							.stream()
							.collect(Collectors.joining("\n")));
					}

					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					build.write(bout);
					return bout.toByteArray();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw Exceptions.duck(e);
			}
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
