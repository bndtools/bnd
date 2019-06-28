package aQute.bnd.differ;

import static aQute.bnd.service.diff.Delta.ADDED;
import static aQute.bnd.service.diff.Delta.CHANGED;
import static aQute.bnd.service.diff.Delta.IGNORED;
import static aQute.bnd.service.diff.Delta.MAJOR;
import static aQute.bnd.service.diff.Delta.MICRO;
import static aQute.bnd.service.diff.Delta.MINOR;
import static aQute.bnd.service.diff.Delta.REMOVED;
import static aQute.bnd.service.diff.Delta.UNCHANGED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;
import aQute.libg.generics.Create;

/**
 * A DiffImpl class compares a newer Element to an older Element. The Element
 * classes hide all the low level details. A Element class is either either
 * Structured (has children) or it is a Leaf, it only has a value. The
 * constructor will first build its children (if any) and then calculate the
 * delta. Each comparable element is translated to an Element. If necessary the
 * Element can be sub classed to provide special behavior.
 */

public class DiffImpl implements Diff, Comparable<DiffImpl>, Formattable {

	final Tree					older;
	final Tree					newer;
	final Collection<DiffImpl>	children;
	final Delta					delta;

	/**
	 * The transitions table defines how the state is escalated depending on the
	 * children. horizontally is the current delta and this is indexed with the
	 * child delta for each child. This escalates deltas from below up.
	 */
	final static Delta[][]		TRANSITIONS	= {
		{
			IGNORED, UNCHANGED, CHANGED, MICRO, MINOR, MAJOR
		},													// IGNORED
		{
			IGNORED, UNCHANGED, CHANGED, MICRO, MINOR, MAJOR
		},													// UNCHANGED
		{
			IGNORED, CHANGED, CHANGED, MICRO, MINOR, MAJOR
		},													// CHANGED
		{
			IGNORED, MICRO, MICRO, MICRO, MINOR, MAJOR
		},													// MICRO
		{
			IGNORED, MINOR, MINOR, MINOR, MINOR, MAJOR
		},													// MINOR
		{
			IGNORED, MAJOR, MAJOR, MAJOR, MAJOR, MAJOR
		},													// MAJOR
		{
			IGNORED, MAJOR, MAJOR, MAJOR, MAJOR, MAJOR
		},													// REMOVED
		{
			IGNORED, MINOR, MINOR, MINOR, MINOR, MAJOR
		},													// ADDED
	};

	/**
	 * Compares the newer against the older, traversing the children if
	 * necessary.
	 *
	 * @param newer The newer Element
	 * @param older The older Element
	 */
	public DiffImpl(Tree newer, Tree older) {
		assert newer != null || older != null;
		this.older = older;
		this.newer = newer;

		// Either newer or older can be null, indicating remove or add
		// so we have to be very careful.
		Tree[] newerChildren = newer == null ? Element.EMPTY : newer.getChildren();
		Tree[] olderChildren = older == null ? Element.EMPTY : older.getChildren();

		int o = 0;
		int n = 0;
		List<DiffImpl> children = new ArrayList<>();
		while (true) {
			Tree nw = n < newerChildren.length ? newerChildren[n] : null;
			Tree ol = o < olderChildren.length ? olderChildren[o] : null;
			DiffImpl diff;

			if (nw == null && ol == null)
				break;

			if (nw != null && ol != null) {
				// we have both sides
				int result = nw.compareTo(ol);
				if (result == 0) {
					// we have two equal named elements
					// use normal diff
					diff = new DiffImpl(nw, ol);
					n++;
					o++;
				} else if (result > 0) {
					// we newer > older, so there is no newer == removed
					diff = new DiffImpl(null, ol);
					o++;
				} else {
					// we newer < older, so there is no older == added
					diff = new DiffImpl(nw, null);
					n++;
				}
			} else {
				// we reached the end of one of the list
				diff = new DiffImpl(nw, ol);
				n++;
				o++;
			}
			children.add(diff);
		}

		// make sure they're read only
		this.children = Collections.unmodifiableCollection(children);
		delta = getDelta(null);
	}

	/**
	 * Return the absolute delta. Also see
	 * {@link #getDelta(aQute.bnd.service.diff.Diff.Ignore)} that allows you to
	 * ignore Diff objects on the fly (and calculate their parents accordingly).
	 */
	@Override
	public Delta getDelta() {
		return delta;
	}

	/**
	 * This getDelta calculates the delta but allows the caller to ignore
	 * certain Diff objects by calling back the ignore call back parameter. This
	 * can be useful to ignore warnings/errors.
	 */

	@Override
	public Delta getDelta(Ignore ignore) {

		// If ignored, we just return ignore.
		if (ignore != null && ignore.contains(this))
			return IGNORED;

		if (newer == null) {
			return REMOVED;
		} else if (older == null) {
			return ADDED;
		} else {
			// now we're sure newer and older are both not null
			assert newer != null && older != null;
			assert newer.getClass() == older.getClass();

			Delta local = Delta.UNCHANGED;

			for (DiffImpl child : children) {
				Delta sub = child.getDelta(ignore);
				if (sub == REMOVED)
					sub = child.older.ifRemoved();
				else if (sub == ADDED)
					sub = child.newer.ifAdded();

				// The escalate method is used to calculate the default
				// transition in the
				// delta based on the children. In general the delta can
				// only escalate, i.e.
				// move up in the chain.

				local = TRANSITIONS[sub.ordinal()][local.ordinal()];
			}
			return local;
		}
	}

	@Override
	public Type getType() {
		return (newer == null ? older : newer).getType();
	}

	@Override
	public String getName() {
		return (newer == null ? older : newer).getName();
	}

	@Override
	public Collection<? extends Diff> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		return String.format("%-10s %-10s %s", getDelta(), getType(), getName());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof DiffImpl) {
			DiffImpl o = (DiffImpl) other;
			return getDelta() == o.getDelta() && getType() == o.getType() && getName().equals(o.getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDelta(), getType(), getName());
	}

	@Override
	public int compareTo(DiffImpl other) {
		if (getDelta() == other.getDelta()) {
			if (getType() == other.getType()) {
				return getName().compareTo(other.getName());
			}
			return getType().compareTo(other.getType());
		}
		return getDelta().compareTo(other.getDelta());
	}

	@Override
	public Diff get(String name) {
		for (DiffImpl child : children) {
			if (child.getName()
				.equals(name))
				return child;
		}
		return null;
	}

	@Override
	public Tree getOlder() {
		return older;
	}

	@Override
	public Tree getNewer() {
		return newer;
	}

	@Override
	public Data serialize() {
		Data data = new Data();
		data.type = getType();
		data.delta = delta;
		data.name = getName();
		data.children = new Data[children.size()];

		int i = 0;
		for (Diff d : children)
			data.children[i++] = d.serialize();

		return data;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision) {
		boolean alternate = (flags & FormattableFlags.ALTERNATE) != 0;
		if (alternate) {
			Set<Delta> deltas = EnumSet.allOf(Delta.class);
			if ((flags & FormattableFlags.UPPERCASE) != 0) {
				deltas.remove(Delta.UNCHANGED);
			}
			int indent = Math.max(width, 0);
			format(formatter, this, Create.list(), deltas, indent, 0);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append('%');
			if ((flags & FormattableFlags.LEFT_JUSTIFY) != 0) {
				sb.append('-');
			}
			if (width != -1) {
				sb.append(width);
			}
			if (precision != -1) {
				sb.append('.');
				sb.append(precision);
			}
			if ((flags & FormattableFlags.UPPERCASE) != 0) {
				sb.append('S');
			} else {
				sb.append('s');
			}
			formatter.format(sb.toString(), toString());
		}
	}

	private static void format(final Formatter formatter, final Diff diff, final List<String> formats,
		final Set<Delta> deltas, final int indent, final int depth) {
		if (depth == formats.size()) {
			StringBuilder sb = new StringBuilder();
			if (depth > 0) {
				sb.append("%n");
			}
			int width = depth * 2;
			for (int leading = width + indent; leading > 0; leading--) {
				sb.append(' ');
			}
			sb.append("%-");
			sb.append(Math.max(20 - width, 1));
			sb.append("s %-10s %s");
			formats.add(sb.toString());
		}
		String format = formats.get(depth);
		formatter.format(format, diff.getDelta(), diff.getType(), diff.getName());
		for (Diff childDiff : diff.getChildren()) {
			if (deltas.contains(childDiff.getDelta())) {
				format(formatter, childDiff, formats, deltas, indent, depth + 1);
			}
		}
	}
}
