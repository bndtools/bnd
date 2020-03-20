package aQute.lib.hierarchy;

import java.util.Optional;

/**
 * A named node in a hierarchy. Paths in the hierarchy are using the '/' as
 * separator. A hierarchy consists of Folder and Leafs. It is rooted in a root
 * FolderNode that is the only node without a parent. The root has an empty
 * name. I.e. not '/'
 * <p>
 * A NamedNode hashCode & equals are based on identity. They are, however,
 * {@link Comparable}
 * <p>
 * A Named Node is either a Folder or a Leaf, there are no other types.
 *
 * @Immutable
 */
public interface NamedNode extends Comparable<NamedNode> {

	/**
	 * The name of this node. This name is always without a '/'.
	 *
	 * @return the name
	 */
	String name();

	/**
	 * The path of this node in the hierarchy. This path never starts with a
	 * '/'. If it is a folder, the path must end with a '/'.
	 * <p>
	 * This path, when used with {@link Hierarchy#find(String)} must return the
	 * this node.
	 *
	 * @return the name
	 */
	String path();

	/**
	 * Return the parent of this node. Only the root will return an empty
	 * {@link Optional}. Each hierarchy has a single root.
	 *
	 * @return the parent or an empty {@link Optional} in the case of the root
	 *         node.
	 */
	Optional<? extends FolderNode> parent();

	/**
	 * Find a path from this node down. The `..` and '.' are supported, meaning
	 * parent and this.
	 *
	 * @param path the path
	 * @return an optional node if found
	 */
	Optional<NamedNode> find(String path);
	/**
	 * @return true if this is a FolderNode
	 */
	default boolean isFolder() {
		return this instanceof FolderNode;
	}

	/**
	 * @return true if this is not a FolderNode
	 */
	default boolean isLeaf() {
		return !isFolder();
	}

	/**
	 * @return true if this is the root node.
	 */
	default boolean isRoot() {
		return !parent().isPresent();
	}

	/**
	 * Return the siblings of this node.
	 *
	 * @return the siblings or empty if the root node
	 */
	default Optional<NamedNode[]> siblings() {
		return parent().map(FolderNode::children);
	}

	/**
	 * Answer the root node
	 */

	FolderNode root();

}
