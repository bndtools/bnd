package aQute.bnd.remoteworkspace.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.lib.aspects.Aspects;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.exceptions.FunctionWithException;
import aQute.lib.io.IO;
import aQute.lib.link.Link;

/**
 * A class that can setup a 2-way link to a {@link RemoteWorkspace} on the same
 * machine.
 */
public class RemoteWorkspaceClientFactory {
	final static Logger				logger			= LoggerFactory.getLogger(RemoteWorkspaceClientFactory.class);
	final static ExecutorService	executorService	= Executors.newFixedThreadPool(6);

	/**
	 * Create a Remote Workspace object that communicates with a Remote
	 * Workspace server on the same machine on the loopback interface.
	 * <p>
	 * This class will search in the {@code {dir}/cnf/cache/remotews} directory
	 * for registered workspaces. (Multiple can be registered.) It will try to
	 * contact these remote workspace servers in order of last modified, newest
	 * first. The first one that responds will be returned.
	 *
	 * @param dir The directory of the workspace
	 * @param client the client API
	 * @return a RemoteWorkspace
	 */
	public static RemoteWorkspace create(File dir, RemoteWorkspaceClient client) {
		return findRemoteWorkspace(dir, p -> {
			return create(p, client);
		});
	}

	/**
	 * Create a Remote Workspace on a specific port.
	 *
	 * @param port the port to use
	 * @param client the client API
	 * @return a Workspace
	 * @throws IOException when something goes wrong
	 */
	public static RemoteWorkspace create(int port, RemoteWorkspaceClient client) throws IOException {
		Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
		@SuppressWarnings("resource")
		Link<RemoteWorkspaceClient, RemoteWorkspace> link = new Link<>(RemoteWorkspace.class, socket, executorService);
		link.open(client);
		RemoteWorkspace remote = link.getRemote();
		return Aspects.intercept(RemoteWorkspace.class, remote)
			.intercept(() -> {
				logger.debug("Closing remote worksapace link on port {}", port);
				IO.close(link::close);
			}, "close")
			.intercept(() -> {
				return "RemoteWorkspace[port=" + port + "]";
			}, "toString")
			.build();

	}

	private static RemoteWorkspace findRemoteWorkspace(File dir,
		FunctionWithException<Integer, RemoteWorkspace> attach) {
		File remotews = getPortDirectory(dir, dir);
		if (remotews.isDirectory()) {

			File[] portFiles = remotews.listFiles();
			if (portFiles.length > 0) {
				//
				// sort newest first. This means that if we run Gradle and
				// Eclipse
				// is running as well we contact Gradle, not eclipse
				//

				Arrays.sort(portFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
				for (File portFile : portFiles)
					try {
						if (!portFile.getName()
							.matches("[0-9]+")) {
							throw new IllegalArgumentException("Port number not a number: " + portFile.getName());
						}

						int port = Integer.parseInt(portFile.getName());
						if (port <= 0 || port >= 0xFFFF) {
							throw new IllegalArgumentException("Port number not in range 1-0xFFFF");
						}

						RemoteWorkspace rws = attach.apply(port);
						logger.info("Found remote workspace {}", rws);
						return rws;

					} catch (Exception e) {
						logger.warn("Found stale or wrong workspace port reference in {}", portFile);

						if (portFile.lastModified() + TimeUnit.HOURS.toMillis(4) < System.currentTimeMillis()) {
							IO.delete(portFile);
							logger.warn(
								"Purged portFile due to not working and likely stale. Can happen when a process quits unexpectedly. File was {}",
								portFile);
						}
					} catch (Throwable t) {
						throw Exceptions.duck(t);
					}
			} else {
				throw new IllegalArgumentException("No remote workspaces registered in   " + remotews);
			}
		}
		throw new IllegalArgumentException("Cannot find remote workspace from directory  " + dir);
	}

	/**
	 * Get the directory where the ports are registered in
	 *
	 * @param dir the directory to start from.
	 * @param org the original directory started from
	 * @return the directory (cnf/cache/remotews) in the first workspace
	 *         encountered
	 */
	public static File getPortDirectory(File dir, File org) {
		if (dir != null && dir.exists()) {
			boolean isWorkspace = IO.getFile(dir, "cnf/build.bnd")
				.isFile();
			if (isWorkspace)
				return IO.getFile(dir, "cnf/cache/remotews");
			return getPortDirectory(dir.getParentFile(), org);
		}
		throw new IllegalArgumentException("No cnf/cache/remotews reachable from " + org);
	}
}
