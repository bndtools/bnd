package aQute.bnd.differ;

import java.util.*;

import aQute.bnd.service.diff.*;

/**
 * An element can be compared to another element of the same type. Elements with
 * the same name and same place in the hierarchy should have the same type. The idea
 * is that for a certain resource type you create an element (Structured or Leaf). This
 * process is done for the newer and older resource. 
 * <p>
 * A Leaf type has a value, comparison is rather simple in this case.
 * <p>
 * A Structured type has named children. The comparison between the newer and older
 * child elements is then done on their name. Two elements with the same name are then
 * matched.
 * <p>
 * The classes are prepared for extension but so far it turned out to be unnecessary. 
 */
abstract class Element {
	final Type		type;
	final String	name;
	final boolean	addMajor;

	/**
	 * Use for elements that have children
	 */
	static class Structured extends Element {
		final Collection<? extends Element>	children;

		Structured(Type type, String name, Collection<? extends Element> children, boolean addMajor) {
			super(type, name, addMajor);
			if (children != null)
				this.children = children;
			else
				this.children = Collections.emptySet();
		}

		Map<String, ? extends Element> getIndex() {
			Map<String, Element> map = new HashMap<String, Element>();
			for (Element def : children) {
				map.put(def.getName(), def);
			}
			return map;
		}
	}

	/**
	 * Use for elements that have a value
	 */
	static class Leaf extends Element {
		final String	value;

		Leaf(Type type, String name, String value, boolean addMajor) {
			super(type, name, addMajor);
			this.value = value;
		}

		String getValue() {
			return value;
		}
		
		boolean isEqual(Leaf other) {
			return getValue() == other.getValue()
					|| (getValue() != null && getValue().equals(other.getValue()));

		}
	}

	Element(Type type, String name, boolean addMajor) {
		this.type = type;
		this.name = name;
		this.addMajor = addMajor;
	}

	Type getType() {
		return type;
	}

	String getName() {
		return name;
	}


	boolean isAddMajor() {
		return addMajor;
	}

}
