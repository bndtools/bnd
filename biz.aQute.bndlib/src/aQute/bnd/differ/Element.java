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
class Element implements Comparable<Element> {
	final String		value;
	final static Element[]	EMPTY	= new Element[0];
	final Type				type;
	final String			name;
	final Delta				add;
	final Delta				remove;
	final String			comment;

	/**
	 * Use for elements that have children
	 */
	static class Structured extends Element {

		final Element[]	children;

		Structured(Type type, String name, String value, Collection<? extends Element> children,
				Delta add, Delta remove, String comment) {
			super(type, name, value, add, remove, comment);
			if (children != null && children.size() > 0) {
				this.children = children.toArray(new Element[children.size()]);
				Arrays.sort(this.children);
			} else
				this.children = EMPTY;
		}
	}

	Element(Type type, String name, String value, Delta add, Delta remove, String comment) {
		this.type = type;
		this.name = name;
		this.value = value;
		this.add = add;
		this.remove = remove;
		this.comment = comment;
	}

	Type getType() {
		return type;
	}

	String getName() {
		return name;
	}

	String getComment() {
		return comment;
	}

	public int compareTo(Element other) {
		if ( type == other.type)
			return name.compareTo(other.name);
		else
			return type.compareTo(other.type);
	}

	String getValue() {
		return value;
	}

	Delta getValueDelta(Element other) {
		if( getValue() == other.getValue()
				|| (getValue() != null && getValue().equals(other.getValue())))
			return Delta.UNCHANGED;
		
		return Delta.CHANGED;
	}
	
	public boolean equals(Object other) {
		if ( getClass() != other.getClass())
			return false;
		
		return compareTo((Element) other) == 0;
	}
	
	public int hashCode() {
		return type.hashCode() ^ name.hashCode();
	}
}
