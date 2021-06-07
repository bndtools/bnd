package aQute.bnd.build;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import aQute.bnd.build.api.BuildInfo;
import aQute.bnd.build.api.OnWorkspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.collections.MultiMap;
import aQute.lib.concurrent.serial.SerialExecutor;

/**
 * Implements the event bus for the workspace, projects and repositories. It can
 * create {@link OnWorkspace} objects that client can use to get events. Clients
 * can close and this will clean up. The class is private to the workspace.
 */
class WorkspaceNotifier implements Closeable {

	final MultiMap<ET, Receipt>	ons			= new MultiMap<>();
	final Map<ET, Object>		last		= new HashMap<>();
	final Object				lock		= new Object();
	final Workspace				workspace;
	final List<Runnable>		events		= new ArrayList<>();
	final SerialExecutor		serial		= new SerialExecutor(Processor.getExecutor());
	int							revision	= 1000;
	boolean						closed;
	volatile boolean			mute		= true;

	class Receipt {
		final ET				type;
		final Consumer<Object>	callback;
		final String			name;
		boolean					closed	= false;

		@SuppressWarnings("unchecked")
		<T> Receipt(ET type, Consumer<T> cb, String name) {
			this.type = type;
			this.name = name;
			this.callback = (Consumer<Object>) cb;
		}

		synchronized void close() {
			if (closed)
				return;
			closed = true;

			ons.get(type)
				.remove(this);
		}

		synchronized void callback(Object o) {
			if (closed)
				return;
			try {
				callback.accept(o);
			} catch (Exception e) {
				Workspace.logger.error("{} callback from workspace notifier for type {} failed. Ignored", name, type,
					e);
			}
		}
	}

	enum ET {
		INIT,
		MESSAGE,
		CLOSING,
		REPOS,
		BUILD,
		PROJECTS,
		CHANGEDPROJECT,
		CHANGEDBNDRUN;
	}

	WorkspaceNotifier(Workspace workspace) {
		this.workspace = workspace;
		last.put(ET.INIT, workspace);
	}

	int initialized() {
		synchronized (lock) {
			last.clear();
			broadcast(ET.INIT, workspace);
			return ++revision;
		}
	}

	void message(Workspace workspace) {
		broadcast(ET.MESSAGE, workspace);
	}

	void closing(Workspace workspace) {
		broadcast(ET.CLOSING, workspace);
	}

	void repos(List<RepositoryPlugin> repos) {
		broadcast(ET.REPOS, repos);
	}

	void build(BuildInfo buildInfo) {
		broadcast(ET.BUILD, buildInfo);
	}

	public void projects(Collection<Project> projects) {
		broadcast(ET.PROJECTS, projects);
	}

	public void changedProject(Project project) {

		if (project.getClass() == Project.class)
			broadcast(ET.CHANGEDPROJECT, project);
	}

	private <T> void broadcast(ET type, T arg) {
		synchronized (lock) {
			if (closed || mute)
				return;
			last.put(type, arg);

			// System.out.println("workspace event " + type + " " + arg);

			List<Receipt> list = ons.get(type);
			if (list == null)
				return;
			serial.run(() -> {
				list.forEach(receipt -> {
					receipt.callback(arg);
				});
			});
		}
	}

	OnWorkspace on(String name) {
		OnWorkspace on = new OnWorkspace() {
			List<Receipt> callbacks = new ArrayList<>();

			@SuppressWarnings("unchecked")
			private <T> OnWorkspace register(ET type, Consumer<T> cb) {
				Receipt r = new Receipt(type, cb, name);
				synchronized (lock) {
					if (!closed) {

						callbacks.add(r);
						ons.add(type, r);
						Object v = last.get(type);
						if (v != null) {
							serial.run(() -> {
								r.callback(v);
							});
						}
					}
					return this;
				}
			}

			@Override
			public void close() throws IOException {
				synchronized (lock) {
					callbacks.forEach(Receipt::close);
				}
			}

			@Override
			public OnWorkspace initial(Consumer<? super Workspace> cb) {
				return register(ET.INIT, cb);
			}

			@Override
			public OnWorkspace message(Consumer<? super Workspace> cb) {
				return register(ET.MESSAGE, cb);
			}

			@Override
			public OnWorkspace closing(Consumer<? super Workspace> cb) {
				return register(ET.CLOSING, cb);
			}

			@Override
			public OnWorkspace repositoriesReady(Consumer<? super Collection<RepositoryPlugin>> cb) {
				return register(ET.REPOS, cb);
			}

			@Override
			public OnWorkspace build(Consumer<? super BuildInfo> cb) {
				return register(ET.BUILD, cb);
			}

			@Override
			public OnWorkspace projects(Consumer<? super Collection<Project>> cb) {
				return register(ET.PROJECTS, cb);
			}

			@Override
			public OnWorkspace changedProject(Consumer<? super Project> cb) {
				return register(ET.CHANGEDPROJECT, cb);
			}

			@Override
			public OnWorkspace changedRun(Consumer<? super Run> cb) {
				return register(ET.CHANGEDBNDRUN, cb);
			}
		};
		return on;
	}

	<T> void ifSameRevision(int revision, ET type, T arg) {
		synchronized (lock) {
			if (closed)
				return;

			if (revision != this.revision)
				return;
			broadcast(type, arg);
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			closed = true;
		}
	}

}
