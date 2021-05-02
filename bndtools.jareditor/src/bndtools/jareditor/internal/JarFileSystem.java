package bndtools.jareditor.internal;

import static java.util.Objects.requireNonNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.internal.filesystem.NullFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import aQute.bnd.result.Result;
import aQute.lib.io.IO;
import aQute.lib.io.NonClosingInputStream;
import aQute.lib.zip.ZipUtil;
import aQute.libg.tuple.Pair;
import aQute.libg.uri.URIUtil;

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
	private static final ILogger									logger		= Logger.getLogger(JarFileSystem.class);

	private static final String										SCHEME_JARF	= "jarf";
	private final ConcurrentMap<IFileStore, Reference<JarRootNode>>	roots		= new ConcurrentHashMap<>();

	private static final Pattern									JARF_P		= Pattern
		.compile("jarf:///(?<fileuri>.*)!(?<path>(/[^!]*)+)");

	static abstract class JarNode extends FileStore {
		private final JarFolderNode	parent;
		private final String		path;
		private final IFileInfo		info;

		JarNode(JarFolderNode parent, IPath path, boolean dir, long length, long lastModified) {
			this.parent = parent;
			this.path = requireNonNull(path).toString();
			String name = path.lastSegment();
			if (name == null) {
				name = "";
			}
			FileInfo info = new FileInfo(name);
			info.setDirectory(dir);
			info.setExists(true);
			info.setLength(length);
			info.setLastModified(lastModified);
			this.info = info;
		}

		@Override
		public IFileInfo fetchInfo() {
			return info;
		}

		@Override
		public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
			return fetchInfo();
		}

		@Override
		public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
			return EMPTY_STRING_ARRAY;
		}

		@Override
		public IFileStore getChild(String name) {
			return new NullFileStore(new Path(getPath()).append(name));
		}

		@Override
		public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
			return new ByteArrayInputStream(new byte[0]);
		}

		@Override
		public String getName() {
			return fetchInfo().getName();
		}

		@Override
		public JarFolderNode getParent() {
			return parent;
		}

		URI jar() {
			return getParent().jar();
		}

		String getPath() {
			return path;
		}

		@Override
		public URI toURI() {
			String path = getPath();
			URI jar = jar();
			return jarf(jar, path).orElseThrow(IllegalStateException::new);
		}
	}

	static class JarFolderNode extends JarNode {
		private final Map<String, JarNode> children = new LinkedHashMap<>();

		JarFolderNode(JarFolderNode parent, IPath path) {
			super(requireNonNull(parent), path, true, 0L, 0L);
		}

		JarFolderNode(IFileStore store) { // for root node
			super(null, Path.EMPTY, true, store.fetchInfo()
				.getLength(),
				store.fetchInfo()
					.getLastModified());
		}

		@Override
		public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
			return children.keySet()
				.stream()
				.toArray(String[]::new);
		}

		@Override
		public IFileStore getChild(String name) {
			JarNode node = children.get(name);
			if (node != null) {
				return node;
			}
			return super.getChild(name);
		}

		void createdNode(IPath path, int segment, long length, long lastModified) {
			int remainingSegments = path.segmentCount() - segment;
			assert remainingSegments > 0;
			String name = path.segment(segment);
			if (remainingSegments == 1) {
				JarFileNode node = new JarFileNode(this, path, length, lastModified);
				JarNode previous = children.put(name, node);
				assert previous == null;
			} else {
				int nextSegment = segment + 1;
				IPath folderPath = path.uptoSegment(nextSegment);
				JarNode node = children.computeIfAbsent(name,
					s -> new JarFolderNode(this, folderPath));
				assert node instanceof JarFolderNode;
				JarFolderNode folder = (JarFolderNode) node;
				folder.createdNode(path, nextSegment, length, lastModified);
			}
		}
	}

	static class JarRootNode extends JarFolderNode {
		private final URI uri;

		JarRootNode(IFileStore store) {
			super(store);
			this.uri = store.toURI();
		}

		@Override
		URI jar() {
			return uri;
		}
	}

	static class JarFileNode extends JarNode {
		JarFileNode(JarFolderNode parent, IPath path, long length, long lastModified) {
			super(requireNonNull(parent), path, false, length, lastModified);
		}

		@Override
		public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
			return JarFileSystem.openInputStream(jar(), getPath(), monitor)
				.mapErr(err -> new Status(IStatus.ERROR, Plugin.PLUGIN_ID, err))
				.orElseThrow(CoreException::new);
		}
	}

	@Override
	public IFileStore getStore(URI uri) {
		if (!SCHEME_JARF.equals(uri.getScheme())) {
			logger.logError("No file system for : " + uri, null);
			return new NullFileStore(Path.EMPTY);
		}
		return jarf(uri).map(pair -> {
			URI fileuri = pair.getFirst();
			IFileStore store;
			try {
				store = EFS.getStore(fileuri);
			} catch (CoreException e) {
				logger.logError("Cannot locate filestore for the JAR file: " + fileuri, e);
				return new NullFileStore(Path.EMPTY);
			}

			JarRootNode root = roots.compute(store, this::computeRootNode)
				.get();
			if (root == null) {
				logger.logError("Failed to load jar for: " + fileuri, null);
				return new NullFileStore(Path.EMPTY);
			}

			IPath path = pair.getSecond();
			IFileStore node = root;
			for (int segment = 0, segmentCount = path.segmentCount(); segment < segmentCount; segment++) {
				node = node.getChild(path.segment(segment));
			}
			return node;
		})
			.recover(err -> {
				logger.logError(err, null);
				return new NullFileStore(Path.EMPTY);
			})
			.unwrap();
	}

	private Reference<JarRootNode> computeRootNode(IFileStore store, Reference<JarRootNode> ref) {
		if (ref != null) {
			JarRootNode current = ref.get();
			if (current != null) {
				IFileInfo currentInfo = current.fetchInfo();
				IFileInfo storeInfo = store.fetchInfo();
				if ((currentInfo.getLastModified() == storeInfo.getLastModified())
					&& (currentInfo.getLength() == storeInfo.getLength())) {
					return ref;
				}
			}
		}
		JarRootNode root = new JarRootNode(store);
		try (ZipInputStream jin = new ZipInputStream(new BufferedInputStream(store.openInputStream(EFS.NONE, null)))) {
			for (ZipEntry entry; (entry = jin.getNextEntry()) != null;) {
				if (entry.isDirectory()) {
					continue;
				}
				IPath path = new Path(null, ZipUtil.cleanPath(entry.getName()));
				long size = entry.getSize();
				if (size < 0) {
					size = IO.drain(new NonClosingInputStream(jin));
				}
				try {
					root.createdNode(path, 0, size, ZipUtil.getModifiedTime(entry));
				} catch (Exception e) {
					root.createdNode(path, 0, -1L, 0L);
				}
			}
		} catch (Exception e) {
			logger.logError("Error processing zip file " + store.toString(), e);
		}
		return new WeakReference<>(root);
	}

	static Result<URI, String> jarf(URI jarfileuri, String path) {
		try {
			String separator = path.startsWith("/") ? "!" : "!/";
			return Result
				.ok(new URI("jarf:///" + jarfileuri.toString() + separator + URIUtil.encodePath(path)));
		} catch (Exception e) {
			return Result.err("failed to construct uri from jar uri=%sm path = %s: %s", jarfileuri, path, e);
		}
	}

	static Result<Pair<URI, IPath>, String> jarf(URI uri) {
		String s = uri.toString();
		Matcher matcher = JARF_P.matcher(s);
		if (!matcher.matches()) {
			return Result.err("%s is not a proper %s URI ", uri, SCHEME_JARF);
		}

		URI fileuri = URI.create(matcher.group("fileuri"));
		IPath path = new Path(null, matcher.group("path")).makeRelative();
		return Result.ok(new Pair<>(fileuri, path));
	}

	static Result<InputStream, String> openInputStream(URI uri, String path, IProgressMonitor monitor)
		throws CoreException {
		ZipInputStream jin = null;
		try {
			IFileStore store = EFS.getStore(uri);
			if (store == null) {
				return Result.err("Cannot locate filestore for the JAR file: %s", uri);
			}

			jin = new ZipInputStream(new BufferedInputStream(store.openInputStream(0, monitor)));
			for (ZipEntry entry; (entry = jin.getNextEntry()) != null;) {
				if (entry.isDirectory()) {
					continue;
				}
				if (Objects.equals(path, ZipUtil.cleanPath(entry.getName()))) {
					return Result.ok(jin); // receiver must close input stream
				}
			}
			IO.close(jin);
			return Result.err("No such resource %s in %s", path, uri);
		} catch (CoreException e) {
			IO.close(jin);
			throw e;
		} catch (Exception e) {
			IO.close(jin);
			return Result.err("Failed to open resource %s from %s : %s", path, uri, e);
		}
	}
}
