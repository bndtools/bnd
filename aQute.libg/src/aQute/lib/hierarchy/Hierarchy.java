package aQute.lib.hierarchy;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import aQute.libg.ints.IntCounter;

/**
 * A general hierarchy of named nodes. Can be accessed fast via a path or
 * iterative.
 */
@SuppressWarnings("unchecked")
public class Hierarchy implements Iterable<NamedNode> {

	static abstract class Node implements NamedNode {
		final Optional<Folder>	parent;
		final String			name;

		Node(Optional<Folder> parent, String name) {
			this.parent = parent;
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String path() {
			StringBuilder sb = new StringBuilder();
			getPath(sb);
			return sb.toString();
		}

		@Override
		public int compareTo(NamedNode b) {
			return name.compareTo(b.name());
		}

		abstract void getPath(StringBuilder app);

		@Override
		public Optional<? extends Folder> parent() {
			return parent;
		}

		@Override
		public FolderNode root() {
			return parent.get() // overridden in root
				.root();
		}

		@Override
		public Optional<NamedNode> find(String path) {
			if (path.startsWith("/"))
				path = path.substring(1);

			if (path.endsWith("/"))
				path = path.substring(0, path.length() - 1);

			String parts[] = path.isEmpty() ? new String[0] : path.split("/");

			Node node = find(parts, 0);
			return Optional.ofNullable(node);
		}

		Node find(String[] parts, int i) {
			if (i == parts.length || ".".equals(parts[i]))
				return this;
			else if ("..".equals(parts[i])) {
				if (isRoot()) {
					throw new IllegalArgumentException(".. attempts to go beyond root");
				}
				return parent.get();
			} else
				return null;
		}

	}

	static class Folder extends Node implements FolderNode {
		final Node[] children;

		Folder(Optional<Folder> parent, String name, Map<String, Object> map, IntCounter size) {
			super(parent, name);
			this.children = new Node[map.size()];

			int n = 0;
			for (Map.Entry<String, Object> e : map.entrySet()) {
				Object value = e.getValue();
				if (value instanceof Map) {
					Map<String, Object> sub = (Map<String, Object>) value;
					Folder folder = new Folder(Optional.of(this), e.getKey(), sub, size);
					children[n] = folder;
				} else {
					children[n] = new Leaf(this, e.getKey(), value);
				}
				n++;
				size.inc();
			}
			Arrays.sort(children);
		}

		@Override
		void getPath(StringBuilder app) {
			parent.get() // override makes sure it is here
				.getPath(app);
			app.append(name)
				.append("/");
		}

		@Override
		public Node find(String[] parts, int i) {
			Node find = super.find(parts, i);
			if (find != null)
				return find;

			String p = parts[i];
			int index = indexOf(p);
			if (index < 0)
				return null;

			return children[index].find(parts, i + 1);
		}

		@Override
		public String toString() {
			return name + "/";
		}

		@Override
		public NamedNode[] children() {
			NamedNode[] cs = new NamedNode[children.length];
			System.arraycopy(children, 0, cs, 0, cs.length);
			return cs;
		}

		@Override
		public int size() {
			return children.length;
		}

		@Override
		public Iterator<NamedNode> iterator() {
			return new Iterator<NamedNode>() {
				int n = 0;

				@Override
				public boolean hasNext() {
					return n < children.length;
				}

				@Override
				public NamedNode next() {
					assert hasNext();
					return children[n++];
				}

			};
		}

		int indexOf(String name) {
			int low = 0;
			int high = children.length - 1;

			while (low <= high) {
				int mid = (low + high) >>> 1;
				int cmp = children[mid].name.compareTo(name);

				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else
					return mid; // key found
			}
			return -(low + 1); // key not found.
		}

		@Override
		public Optional<NamedNode> get(String name) {
			int n = indexOf(name);
			if (n < 0)
				return Optional.empty();
			else
				return Optional.of(children[n]);
		}

		public int indexOf(Node node) {
			return Arrays.binarySearch(children, node);
		}

	}

	static class RootNode extends Folder {
		final int size;

		public RootNode(Map<String, Object> map) {
			this(map, new IntCounter(1));
		}

		private RootNode(Map<String, Object> map, IntCounter size) {
			super(Optional.empty(), "", map, size);
			this.size = size.get();
		}

		@Override
		void getPath(StringBuilder app) {}

		@Override
		public FolderNode root() {
			return this;
		}

		@Override
		public Optional<? extends Folder> parent() {
			return Optional.empty();
		}
	}

	static class OrphanNode extends Node {
		OrphanNode(String name) {
			super(null, name);
		}

		@Override
		Node find(String[] parts, int i) {
			assert false : "May not be called";
			return null;
		}

		@Override
		void getPath(StringBuilder app) {
			assert false : "May not be called";
		}

	}

	static class Leaf extends Node implements LeafNode {
		final Object payload;

		Leaf(Folder parent, String name, Object payload) {
			super(Optional.of(parent), name);
			this.payload = payload;
		}

		@Override
		void getPath(StringBuilder app) {
			parent.get()
				.getPath(app);
			app.append(name);
		}

		@Override
		public String toString() {
			return name;
		}

		Object payload() {
			return payload;
		}

	}

	final RootNode root;

	public Hierarchy(Map<String, Object> map) {
		root = new RootNode(map);
	}

	/**
	 * Find a folder
	 *
	 * @param path the name of the folder. Can end in '/' or not
	 * @return a folder node
	 */
	public Optional<FolderNode> findFolder(String path) {
		return find(path).filter(NamedNode::isFolder)
			.map(FolderNode.class::cast);
	}

	public Optional<FolderNode> findFolder(String[] parts) {
		return find(parts).filter(NamedNode::isFolder)
			.map(FolderNode.class::cast);
	}

	/**
	 * Find a node in the hierarchy.
	 *
	 * @param path a '/' separated path. May start and end with superfluous '/'
	 * @return a node or {@link Optional#empty()} if not found
	 */
	public Optional<NamedNode> find(String path) {
		return root.find(path);
	}

	@Override
	public Iterator<NamedNode> iterator() {
		return new Iterator<NamedNode>() {
			Node node = root;

			@Override
			public boolean hasNext() {
				return node != null;
			}

			@Override
			public Node next() {
				if (node == null)
					throw new NoSuchElementException();

				Node ret = node;
				if (node instanceof Folder) {
					Folder folder = (Folder) node;
					if (folder.children.length > 0) {
						node = folder.children[0];
						return ret;
					}
				}

				Folder rover = node.parent.orElse(null);
				while (true) {

					if (rover == null) {
						node = null;
						return ret;
					}

					int n = node.parent.get()
						.indexOf(node);
					assert n >= 0 : "we are its child!";

					n++;

					if (n >= rover.children.length) {
						node = rover;
						rover = rover.parent.orElse(null);
					} else {
						node = rover.children[n];
						return ret;
					}
				}
			}

		};
	}

	protected Object payload(LeafNode node) {
		return ((Leaf) node).payload;
	}

	protected Map<String, ?> asMap() {
		return new AbstractMap<String, Object>() {

			@Override
			public Set<Entry<String, Object>> entrySet() {
				return new AbstractSet<Entry<String, Object>>() {

					@Override
					public Iterator<Entry<String, Object>> iterator() {
						Iterator<NamedNode> i = Hierarchy.this.iterator();
						return new Iterator<Map.Entry<String, Object>>() {

							@Override
							public boolean hasNext() {
								return i.hasNext();
							}

							@Override
							public Entry<String, Object> next() {
								NamedNode next = i.next();

								return new Entry<String, Object>() {

									@Override
									public String getKey() {
										return next.path();
									}

									@Override
									public Object getValue() {
										if (next instanceof Leaf) {
											return ((Leaf) next).payload;
										}
										return null;
									}

									@Override
									public NamedNode setValue(Object value) {
										throw new UnsupportedOperationException();
									}
								};
							}
						};
					}

					@Override
					public int size() {
						return root.size;
					}
				};
			}

			@Override
			public Object get(Object key) {
				if (!(key instanceof String))
					return null;

				String path = (String) key;
				return find(path).filter(n -> n instanceof Leaf)
					.map(Leaf.class::cast)
					.map(Leaf::payload)
					.orElse(null);
			}

			@Override
			public int size() {
				return root.size;
			}

		};
	}

	public int size() {
		return root.size;
	}

	public Stream<NamedNode> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	public Optional<NamedNode> find(String[] parts) {
		return Optional.ofNullable(root.find(parts, 0));
	}

}
