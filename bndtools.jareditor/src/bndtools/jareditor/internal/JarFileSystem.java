package bndtools.jareditor.internal;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.internal.filesystem.NullFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.result.Result;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.strings.Strings;

/**
 * Implements the URI
 *
 * <pre>
 *
 * 			jarf 		::= 'jarf:///' jarfileuri '!' <path starting with />
 *   		jarfileuri	::= <any uri including jarf>
 * </pre>
 *
 * @author aqute
 */
public class JarFileSystem extends FileSystem {

	static final Object							SCHEME_JAR	= "jarf";
	final Map<URI, WeakReference<JarRootNode>>	roots		= new HashMap<URI, WeakReference<JarFileSystem.JarRootNode>>();

	final static Pattern						JARF_P		= Pattern
		.compile("jarf:///(?<fileuri>.*)!(?<path>(/[^!]*)+)");

	static abstract class JarNode extends FileStore {
		final JarNode	parent;
		final String	name;
		final FileInfo	info;

		JarNode(JarNode parent, String name, boolean exists, boolean dir, long length, long lastModified) {
			this.parent = parent;
			this.name = name;
			this.info = new FileInfo(name);
			this.info.setDirectory(dir);
			this.info.setExists(exists);
			this.info.setLength(length);
			this.info.setLastModified(lastModified);
		}

		@Override
		public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
			return info;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public IFileStore getParent() {
			return parent;
		}

		URI jar() {
			return parent.jar();
		}

		abstract String getPath();

		@Override
		public URI toURI() {
			String path = getPath();
			URI jar = jar();
			return jarf(jar, path).orElseThrow(IllegalArgumentException::new);
		}

	}

	static class JarFolderNode extends JarNode {

		JarFolderNode(JarNode parent, String name) {
			super(parent, name, true, true, 0, 0);
		}

		final Map<String, JarNode> children = new LinkedHashMap<>();

		@Override
		public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
			return children.keySet()
				.stream()
				.toArray(String[]::new);
		}

		@Override
		public IFileStore getChild(String name) {
			return children.get(name);
		}

		@Override
		public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
			throw new UnsupportedOperationException();
		}

		void createdNode(String path, String[] split, int index, long size, long modified) {
			int segment = split.length - index;
			assert segment > 0;
			String name = split[index];
			if (segment == 1) {
				JarFileNode node = new JarFileNode(this, name, size, modified);
				JarNode previous = children.put(name, node);
				assert previous == null;
			} else {
				JarNode node = children.computeIfAbsent(name, s -> new JarFolderNode(this, s));

				assert node instanceof JarFolderNode;

				JarFolderNode dir = (JarFolderNode) node;
				dir.createdNode(path, split, index + 1, size, modified);
			}
		}

		@Override
		String getPath() {
			return parent.getPath() + name + "/";
		}

	}

	static class JarRootNode extends JarFolderNode {

		final URI uri;

		JarRootNode(URI uri) {
			super(null, "");
			this.uri = uri;
		}

		@Override
		URI jar() {
			return uri;
		}

		@Override
		String getPath() {
			return "";
		}

	}

	static class JarFileNode extends JarNode {

		JarFileNode(JarNode parent, String name, long length, long lastModified) {
			super(parent, name, true, false, length, lastModified);
		}

		@Override
		public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
			return EMPTY_STRING_ARRAY;
		}

		@Override
		public IFileStore getChild(String name) {
			return null;
		}

		@Override
		public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
			return JarFileSystem.openInputStream(jar(), getPath(), monitor)
				.orElseThrow(s -> {
					Status status = new Status(Status.ERROR, Plugin.PLUGIN_ID, s);
					return Exceptions.duck(new CoreException(status));
				});
		}

		@Override
		String getPath() {
			return parent.getPath() + name;
		}
	}

	@Override
	public IFileStore getStore(URI uri) {
		if (SCHEME_JAR.equals(uri.getScheme())) {
			// WeakReference<JarRootNode> wr;
			// synchronized (this) {
			// wr = roots.get(uri);
			// if (wr != null) {
			// JarRootNode jarRootNode = wr.get();
			// if (jarRootNode != null)
			// return jarRootNode;
			// }
			// }

			return jarf(uri).flatMap(ss -> {
				URI u = new URI(ss[0]);
				IFileStore store = EFS.getStore(u);
				if (store == null) {
					return Result.err("Cannot locate filestore for the JAR file: %s", uri);
				}
				JarRootNode root = new JarRootNode(store.toURI());

				try (Jar jar = new Jar(u.toString(), store.openInputStream(0, null))) {
					jar.getResources()
						.forEach((path, resource) -> {
							String[] segments = path.split("/");
							try {
								root.createdNode(path, segments, 0, resource.size(), resource.lastModified());
							} catch (Exception e) {
								root.createdNode(path, segments, 0, -1, 0);
							}
						});
					WeakReference<JarRootNode> weakRef = new WeakReference<>(root);
					synchronized (this) {
						roots.putIfAbsent(uri, weakRef);
					}
					if (ss[1] == null || ss[1].equals("/") || ss[1].isEmpty()) {
						return Result.ok(root);
					}

					List<String> segments = Strings.split("/", ss[1]);
					JarNode rover = root;
					for (String segment : segments) {
						if (!(rover instanceof JarFolderNode)) {
							return Result.ok(new NullFileStore(null));
						}
						JarFolderNode jfn = (JarFolderNode) rover;
						rover = (JarNode) rover.getChild(segment);
					}
					return Result.ok(rover);
				} catch (Exception e) {
					return Result.err("Failed to load jar for %s: %s", u, e);
				}
			})
				.map(s -> (IFileStore) s)
				.orElse(null);
		}
		throw new IllegalArgumentException("No file system for " + uri);
	}

	static Result<URI, String> jarf(URI jarfileuri, String path) {
		try {
			if (path == null)
				path = "/";
			else if (!path.startsWith("/"))
				path = "/" + path;
			return Result.ok(new URI("jarf:///" + jarfileuri.toString() + "!" + path));
		} catch (Exception e) {
			return Result.err("failed to construct uri from jar uri=%sm path = %s: %s", jarfileuri, path,
				e.getMessage());
		}
	}

	static Result<String[], String> jarf(URI uri) {
		if (uri == null)
			return Result.err("uri parameter is null");

		String s = uri.toString();
		Matcher matcher = JARF_P.matcher(s);
		if (!matcher.matches()) {
			return Result.err("%s is not a proper %s URI ", uri, SCHEME_JAR);
		}

		String[] result = new String[2];
		result[0] = matcher.group("fileuri");
		result[1] = matcher.group("path");
		return Result.ok(result);
	}

	static Result<InputStream, String> openInputStream(URI uri, String path, IProgressMonitor monitor)
		throws CoreException {
		try {
			IFileStore store = EFS.getStore(uri);
			if (store == null) {
				return Result.err("Cannot locate filestore for the JAR file: %s", uri);
			}

			try (Jar jar = new Jar(".", store.openInputStream(0, monitor))) {
				Resource r = jar.getResource(path);
				if (r == null) {
					return Result.err("No such resource %s in %s", path, uri);
				}
				return Result.ok(r.openInputStream());
			}
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			return Result.err("Failed to open resource %s from %s : %s", path, uri, e.getMessage());
		}
	}
}
