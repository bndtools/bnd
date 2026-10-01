package aQute.bnd.lsp.services;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;

public class BndWorkspaceManager {
	private static final Logger					logger		= LoggerFactory.getLogger(BndWorkspaceManager.class);

	private final Map<File, Workspace>			workspaces	= new ConcurrentHashMap<>();
	private final Map<File, ReentrantLock>		locks		= new ConcurrentHashMap<>();
	private final ExecutorService				executor	= Executors.newSingleThreadExecutor(r -> {
																Thread t = new Thread(r, "bnd-lsp-workspace");
																t.setDaemon(true);
																return t;
															});

	public BndWorkspaceManager() {}

	public static File getFileFromUri(String uriStr) {
		if (uriStr == null || uriStr.isBlank()) {
			return null;
		}
		try {
			String cleaned = uriStr.trim();
			if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
				|| (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
				cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
			}
			if (cleaned.startsWith("file:")) {
				try {
					URI uri = URI.create(cleaned);
					try {
						return Paths.get(uri).toFile();
					} catch (Exception e) {
						String p = uri.getPath();
						if (p != null) {
							if (p.startsWith("/") && p.length() > 2 && p.charAt(2) == ':') {
								p = p.substring(1);
							}
							return new File(p);
						}
					}
				} catch (Exception e) {
					// Fallback: decode URI string directly
					String decoded = java.net.URLDecoder.decode(cleaned.substring(5), java.nio.charset.StandardCharsets.UTF_8);
					while (decoded.startsWith("/")) {
						if (decoded.length() > 2 && decoded.charAt(2) == ':') {
							decoded = decoded.substring(1);
							break;
						}
						decoded = decoded.substring(1);
					}
					return new File(decoded);
				}
			}
			return new File(cleaned);
		} catch (Exception e) {
			logger.warn("Could not parse file from URI {}", uriStr, e);
			return null;
		}
	}

	public Workspace getWorkspaceForFile(File file) {
		if (file == null)
			return null;

		File cnfDir = findCnfDir(file);
		if (cnfDir == null)
			return null;

		File wsRoot = cnfDir.getParentFile();
		if (wsRoot == null)
			return null;

		return workspaces.computeIfAbsent(wsRoot, root -> {
			try {
				logger.info("Initializing bnd workspace at {}", root);
				return Workspace.getWorkspace(root);
			} catch (Exception e) {
				logger.error("Failed to initialize bnd workspace at {}", root, e);
				return null;
			}
		});
	}

	public Workspace getWorkspaceForUri(String uriStr) {
		File file = getFileFromUri(uriStr);
		return file != null ? getWorkspaceForFile(file) : null;
	}

	public Project getProjectForFile(File file) {
		Workspace ws = getWorkspaceForFile(file);
		if (ws == null)
			return null;

		try {
			return ws.getProjectFromFile(file);
		} catch (Exception e) {
			logger.debug("Could not get project for file {}", file, e);
			return null;
		}
	}

	public Project getProjectForUri(String uriStr) {
		File file = getFileFromUri(uriStr);
		return file != null ? getProjectForFile(file) : null;
	}

	public Processor getProcessorForFile(File file) {
		if (file == null)
			return null;

		if (file.getName()
			.endsWith(".bndrun")) {
			Workspace ws = getWorkspaceForFile(file);
			if (ws != null) {
				try {
					return Run.createRun(ws, file);
				} catch (Exception e) {
					logger.debug("Failed to create Run for {}", file, e);
				}
			}
		}

		Project project = getProjectForFile(file);
		if (project != null)
			return project;

		return getWorkspaceForFile(file);
	}

	public List<RepositoryPlugin> getRepositories(File file) {
		Workspace ws = getWorkspaceForFile(file);
		if (ws == null)
			return Collections.emptyList();

		try {
			return ws.getRepositories();
		} catch (Exception e) {
			logger.debug("Failed to list repositories for {}", file, e);
			return Collections.emptyList();
		}
	}

	public Collection<Project> getAllProjects(File file) {
		Workspace ws = getWorkspaceForFile(file);
		if (ws == null)
			return Collections.emptyList();

		try {
			return ws.getAllProjects();
		} catch (Exception e) {
			logger.debug("Failed to list projects for {}", file, e);
			return Collections.emptyList();
		}
	}

	public String expandMacro(String macroExpression, File file) {
		Processor processor = getProcessorForFile(file);
		if (processor == null) {
			processor = new Processor();
		}

		try {
			Macro macro = new Macro(processor);
			return macro.process(macroExpression);
		} catch (Exception e) {
			logger.debug("Macro expansion error: {}", macroExpression, e);
			return null;
		}
	}

	public void refreshWorkspace(File file) {
		Workspace ws = getWorkspaceForFile(file);
		if (ws != null) {
			ReentrantLock lock = locks.computeIfAbsent(ws.getBase(), k -> new ReentrantLock());
			lock.lock();
			try {
				ws.refresh();
				logger.info("Refreshed bnd workspace at {}", ws.getBase());
			} catch (Exception e) {
				logger.error("Failed to refresh workspace at {}", ws.getBase(), e);
			} finally {
				lock.unlock();
			}
		}
	}

	public <T> Future<T> executeWithWorkspaceLock(File file, WorkspaceTask<T> task) {
		return executor.submit(() -> {
			Workspace ws = getWorkspaceForFile(file);
			if (ws == null) {
				return task.run(null);
			}
			ReentrantLock lock = locks.computeIfAbsent(ws.getBase(), k -> new ReentrantLock());
			lock.lock();
			try {
				return task.run(ws);
			} finally {
				lock.unlock();
			}
		});
	}

	public void shutdown() {
		executor.shutdown();
		for (Workspace ws : workspaces.values()) {
			try {
				ws.close();
			} catch (Exception e) {
				// ignore
			}
		}
		workspaces.clear();
	}

	private static File findCnfDir(File start) {
		File current = start.isDirectory() ? start : start.getParentFile();
		while (current != null && current.exists()) {
			File cnf = new File(current, "cnf");
			if (cnf.isDirectory() && new File(cnf, "build.bnd").isFile()) {
				return cnf;
			}
			current = current.getParentFile();
		}
		return null;
	}

	@FunctionalInterface
	public interface WorkspaceTask<T> {
		T run(Workspace ws) throws Exception;
	}
}
