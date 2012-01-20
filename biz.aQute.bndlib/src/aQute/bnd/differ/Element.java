package aQute.bnd.differ;

import java.util.*;

import aQute.bnd.service.diff.*;

/**
 * An element can be compared to another element of the same type. Elements with
 * the same name and same place in the hierarchy should have the same type. The
 * idea is that for a certain resource type you create an element (Structured or
 * Leaf). This process is done for the newer and older resource.
 * <p>
 * A Leaf type has a value, comparison is rather simple in this case.
 * <p>
 * A Structured type has named children. The comparison between the newer and
 * older child elements is then done on their name. Two elements with the same
 * name are then matched.
 * <p>
 * The classes are prepared for extension but so far it turned out to be
 * unnecessary.
 */

class Element implements Comparable<Element>, Tree {
	final static Element[]	EMPTY	= new Element[0];
	final Type				type;
	final String			name;
	final Delta				add;
	final Delta				remove;
	final String			comment;
	final Element[]			children;

	Element(Type type, String name) {
		this(type, name, null, Delta.MINOR, Delta.MAJOR, null);
	}

	Element(Type type, String name, Element... children) {
		this(type, name, Arrays.asList(children), Delta.MINOR, Delta.MAJOR, null);
	}

	Element(Type type, String name, Collection<? extends Element> children, Delta add,
			Delta remove, String comment) {
		this.type = type;
		this.name = name;
		this.add = add;
		this.remove = remove;
		this.comment = comment;
		if (children != null && children.size() > 0) {
			this.children = children.toArray(new Element[children.size()]);
			Arrays.sort(this.children);
		} else
			this.children = EMPTY;
	}

	public Element(Data data) {
		this.name = data.name;
		this.type = data.type;
		this.comment = data.comment;
		this.add = data.add;
		this.remove = data.rem;
		if (data.children == null)
			children = EMPTY;
		else {
			this.children = new Element[data.children.length];
			for (int i = 0; i < children.length; i++)
				children[i] = new Element(data.children[i]);
			Arrays.sort(this.children);
		}
	}
	public Data serialize() {
		Data data = new Data();
		data.type = this.type;
		data.name = this.name;
		data.add = this.add;
		data.rem = this.remove;
		data.comment = this.comment;
		if (children.length != 0) {
			data.children = new Data[children.length];
			for (int i = 0; i < children.length; i++) {
				data.children[i] = children[i].serialize();
			}
		}
		return data;
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	String getComment() {
		return comment;
	}

	public int compareTo(Element other) {
		if (type == other.type)
			return name.compareTo(other.name);
		else
			return type.compareTo(other.type);
	}

	public boolean equals(Object other) {
		if (other == null || getClass() != other.getClass())
			return false;

		return compareTo((Element) other) == 0;
	}

	public int hashCode() {
		return type.hashCode() ^ name.hashCode();
	}

	public Tree[] getChildren() {
		return children;
	}

	public Delta ifAdded() {
		return add;
	}

	public Delta ifRemoved() {
		return remove;
	}

	public Diff diff(Tree older) {
		return new DiffImpl(this, (Element) older);
	}

	public Element get(String name) {
		for (Element e : children) {
			if (e.name.equals(name))
				return e;
		}
		return null;
	}
	
	public String toString() {
		return type + " " + name + " (" + add + "/" + remove + ")";
	}


}
