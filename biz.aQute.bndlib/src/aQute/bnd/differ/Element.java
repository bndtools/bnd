package aQute.bnd.differ;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;

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

class Element implements Tree {
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

	Element(Type type, String name, List<Element> children) {
		this(type, name, children, Delta.MINOR, Delta.MAJOR, null);
	}

	Element(Type type, String name, Collection<? extends Element> children, Delta add, Delta remove, String comment) {
		this.type = type;
		this.name = name;
		this.add = add;
		this.remove = remove;
		this.comment = comment;
		if ((children == null) || children.isEmpty()) {
			this.children = EMPTY;
		} else {
			this.children = children.stream()
				.sorted()
				.toArray(Element[]::new);
		}
	}

	public Element(Data data) {
		this.name = data.name;
		this.type = data.type;
		this.comment = data.comment;
		this.add = data.add;
		this.remove = data.rem;
		if ((data.children == null) || (data.children.length == 0)) {
			this.children = EMPTY;
		} else {
			this.children = Arrays.stream(data.children)
				.map(Element::new)
				.sorted()
				.toArray(Element[]::new);
		}
	}

	@Override
	public Data serialize() {
		Data data = new Data();
		data.type = getType();
		data.name = getName();
		data.add = ifAdded();
		data.rem = ifRemoved();
		data.comment = getComment();
		data.children = Arrays.stream(getChildren())
			.map(Element::serialize)
			.toArray(Data[]::new);
		return data;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	String getComment() {
		return comment;
	}

	@Override
	public int compareTo(Tree other) {
		if (getType() == other.getType())
			return getName().compareTo(other.getName());
		return getType().compareTo(other.getType());
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || getClass() != other.getClass())
			return false;

		return compareTo((Element) other) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getType(), getName());
	}

	@Override
	public Element[] getChildren() {
		return children;
	}

	@Override
	public Delta ifAdded() {
		return add;
	}

	@Override
	public Delta ifRemoved() {
		return remove;
	}

	@Override
	public Diff diff(Tree older) {
		return new DiffImpl(this, older);
	}

	@Override
	public Element get(String name) {
		for (Element e : getChildren()) {
			if (e.getName()
				.equals(name))
				return e;
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, "");
		return sb.toString();
	}

	private void toString(StringBuilder sb, String indent) {
		sb.append(indent)
			.append(getType())
			.append(" ")
			.append(getName())
			.append(" (")
			.append(ifAdded())
			.append("/")
			.append(ifRemoved())
			.append(")")
			.append("\n");
		for (Element e : getChildren())
			e.toString(sb, indent + " ");
	}

}
