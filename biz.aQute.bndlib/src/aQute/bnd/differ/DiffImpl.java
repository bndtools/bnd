package aQute.bnd.differ;

import static aQute.bnd.service.diff.Delta.*;

import java.util.*;

import aQute.bnd.differ.Element.*;
import aQute.bnd.service.diff.*;

/**
 * A DiffImpl class compares a newer Element to an older Element. The Element
 * classes hide all the low level details. A Element class is either either
 * Structured (has children) or it is a Leaf, it only has a value. The
 * constructor will first build its children (if any) and then calculate the
 * delta. Each comparable element is translated to an Element. If necessary the
 * Element can be sub classed to provide special behavior.
 */

public class DiffImpl implements Diff, Comparable<DiffImpl> {

	final Element				older;
	final Element				newer;
	final Collection<DiffImpl>	children;
	final Delta					delta;

	/**
	 * The transitions table defines how the state is escalated depending on the
	 * children. horizontally is the current delta and this is indexed with the
	 * child delta for each child. This escalates deltas from below up.
	 */
	final static Delta[][]		TRANSITIONS	= {
			{ IGNORED, UNCHANGED, CHANGED, MICRO, MINOR, MAJOR }, // IGNORED
			{ IGNORED, UNCHANGED, CHANGED, MICRO, MINOR, MAJOR }, // UNCHANGED
			{ IGNORED, CHANGED, CHANGED, MICRO, MINOR, MAJOR }, // CHANGED
			{ IGNORED, MICRO, MICRO, MICRO, MINOR, MAJOR }, // MICRO
			{ IGNORED, MINOR, MINOR, MINOR, MINOR, MAJOR }, // MINOR
			{ IGNORED, MAJOR, MAJOR, MAJOR, MAJOR, MAJOR }, // MAJOR
			{ IGNORED, MAJOR, MAJOR, MAJOR, MAJOR, MAJOR }, // REMOVED
			{ IGNORED, MINOR, MINOR, MINOR, MINOR, MAJOR }, // ADDED_MINOR
			{ IGNORED, MAJOR, MAJOR, MAJOR, MAJOR, MAJOR }, // ADDED_MAJOR
											};

	/**
	 * Compares the newer against the older, traversing the children if
	 * necessary.
	 * 
	 * @param newer
	 *            The newer Element
	 * @param older
	 *            The older Element
	 */
	DiffImpl(Element newer, Element older) {
		assert newer != null || older != null;
		this.older = older;
		this.newer = newer;

		if ((newer == null ? older : newer) instanceof Structured) {

			// Either newer or older can be null, indicating remove or add
			// so we have to be very careful.

			Map<String, ? extends Element> newerChildren = Collections.emptyMap();
			Map<String, ? extends Element> olderChildren = Collections.emptyMap();

			if (newer != null)
				newerChildren = ((Structured) newer).getIndex();

			if (older != null)
				olderChildren = ((Structured) older).getIndex();

			// Create a list with all key names.

			Set<String> all = new HashSet<String>(newerChildren.keySet());
			all.addAll(olderChildren.keySet());
			TreeSet<DiffImpl> children = new TreeSet<DiffImpl>();

			// Compare the children

			for (String key : all) {
				DiffImpl diff = new DiffImpl(newerChildren.get(key), olderChildren.get(key));
				children.add(diff);
			}

			// make sure they're read only
			this.children = Collections.unmodifiableCollection(children);
		} else {
			children = Collections.emptyList();
		}
		delta = getDelta(null);
	}

	/**
	 * Return the absolute delta. Also see
	 * {@link #getDelta(aQute.bnd.service.diff.Diff.Ignore)} that allows you to
	 * ignore Diff objects on the fly (and calculate their parents accordingly).
	 */
	public Delta getDelta() {
		return delta;
	}

	/**
	 * This getDelta calculates the delta but allows the caller to ignore
	 * certain Diff objects by calling back the ignore call back parameter.
	 */
	public Delta getDelta(Ignore ignore) {

		// If ignored, we just return ignore.
		if (ignore != null && ignore.contains(this))
			return Delta.IGNORED;

		if (newer == null) {
			return Delta.REMOVED;
		} else if (older == null) {

			// Depending on the Element type we can see an add as major (adding
			// a
			// method to an interface for example) or minor (adding a method
			// to a class).

			if (newer.isAddMajor())
				return Delta.ADD_MAJOR;
			else
				return Delta.ADD_MINOR;

		} else {
			// now we're sure newer and older are both not null

			if (newer instanceof Structured) {
				Delta local = Delta.UNCHANGED;
				for (DiffImpl child : children) {
					Delta sub = child.getDelta(ignore);

					// The escalate method is used to calculate the default
					// transition in the
					// delta based on the children. In general the delta can
					// only escalate, i.e.
					// move up in the chain.

					local = TRANSITIONS[sub.ordinal()][local.ordinal()];
				}
				return local;
			} else {
				return ((Leaf) newer).isEqual((Leaf) older) ? Delta.UNCHANGED : Delta.CHANGED;
			}
		}
	}

	public Type getType() {
		return (newer == null ? older : newer).getType();
	}

	public String getName() {
		return (newer == null ? older : newer).getName();
	}

	public String getOlderValue() {
		return older == null || !(older instanceof Leaf) ? null : ((Leaf) older).getValue();
	}

	public String getNewerValue() {
		return newer == null || !(newer instanceof Leaf) ? null : ((Leaf) newer).getValue();
	}

	public Collection<? extends Diff> getChildren() {
		return children;
	}

	public String toString() {
		String value = "";
		if ((newer != null ? newer : older) instanceof Leaf) {
			String oldv = older == null ? null : ((Leaf) older).getValue();
			String newv = newer == null ? null : ((Leaf) newer).getValue();
			if (oldv != null || newv != null) {
				switch (getDelta()) {
				case REMOVED:
					value = " = /" + oldv;
					break;
				case ADD_MAJOR:
				case ADD_MINOR:
					value = " = " + newv + "/";
					break;
				case UNCHANGED:
					value = " = " + newv;
					break;
				default:
					value = " = " + newv + "/" + oldv;
				}
			}
		}
		return String.format("%-10s %-10s %s%s", getDelta(), getType(), getName(), value);
	}

	public boolean equals(Object other) {
		if (other instanceof DiffImpl) {
			DiffImpl o = (DiffImpl) other;
			return getDelta() == o.getDelta() && getType() == o.getType()
					&& getName().equals(o.getName());
		}
		return false;
	}

	public int hashCode() {
		return getDelta().hashCode() ^ getType().hashCode() ^ getName().hashCode();
	}

	public int compareTo(DiffImpl other) {
		if (getDelta() == other.getDelta()) {
			if (getType() == other.getType()) {
				return getName().compareTo(other.getName());
			} else
				return getType().compareTo(other.getType());
		} else
			return getDelta().compareTo(other.getDelta());
	}

	public Diff get(String name) {
		for (DiffImpl child : children) {
			if (child.getName().equals(name))
				return child;
		}
		return null;
	}

}
