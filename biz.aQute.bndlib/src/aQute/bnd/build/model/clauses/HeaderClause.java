package aQute.bnd.build.model.clauses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringTokenizer;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.stream.MapStream;
import aQute.lib.strings.Strings;

public class HeaderClause implements Cloneable, Comparable<HeaderClause> {

	private static final String	INTERNAL_LIST_SEPARATOR				= ";";
	private static final String	INTERNAL_LIST_SEPARATOR_NEWLINES	= INTERNAL_LIST_SEPARATOR + "\\\n\t\t";

	protected String			name;
	protected Attrs				attribs;

	public HeaderClause(String name, Attrs attribs) {
		assert name != null;

		this.name = name;
		this.attribs = attribs == null ? new Attrs() : attribs;
	}

	/**
	 * Accept String syntax as defined by 1 element of a Parameters
	 *
	 * @param v one element of Parameter
	 */
	public HeaderClause(String v) {
		Parameters parameters = new Parameters(v);
		if (parameters.size() != 1)
			throw new IllegalArgumentException("Invalid header clause (not exactly 1 element) " + v);

		Entry<String, Attrs> next = parameters.entrySet()
			.iterator()
			.next();
		this.name = next.getKey();
		this.attribs = next.getValue();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Attrs getAttribs() {
		return attribs;
	}

	public List<String> getListAttrib(String attrib) {
		String string = attribs.get(attrib);
		if (string == null)
			return null;

		List<String> result = new ArrayList<>();
		StringTokenizer tokenizer = new StringTokenizer(string, ",");
		while (tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken()
				.trim());
		}

		return result;
	}

	public void setListAttrib(String attrib, Collection<? extends String> value) {
		if (value == null || value.isEmpty())
			attribs.remove(attrib);
		else {
			StringBuilder buffer = new StringBuilder();
			boolean first = true;
			for (String string : value) {
				if (!first)
					buffer.append(',');
				buffer.append(string);
				first = false;
			}
			attribs.put(attrib, buffer.toString());
		}
	}

	public void formatTo(StringBuilder buffer) {
		formatTo(buffer, null, newlinesBetweenAttributes());
	}

	public void formatTo(StringBuilder buffer, boolean newlinesBetweenAttributes) {
		formatTo(buffer, null, newlinesBetweenAttributes);
	}

	public void formatTo(StringBuilder buffer, Comparator<Entry<String, String>> sorter) {
		formatTo(buffer, sorter, newlinesBetweenAttributes());
	}

	public void formatTo(StringBuilder buffer, Comparator<Entry<String, String>> sorter,
		boolean newlinesBetweenAttributes) {
		String separator = newlinesBetweenAttributes ? INTERNAL_LIST_SEPARATOR_NEWLINES : INTERNAL_LIST_SEPARATOR;

		if (name.indexOf(',') >= 0)
			OSGiHeader.quote(buffer, name);
		else
			buffer.append(name);

		MapStream<String, String> entries = MapStream.ofNullable(attribs);
		if (sorter != null) {
			entries = entries.sorted(sorter);
		}
		entries.filterValue(Strings::nonNullOrEmpty)
			.forEachOrdered((name, value) -> {
				buffer.append(separator);
				int n = buffer.length();
				OSGiHeader.quote(buffer, name);
				n = buffer.length() - n;

				while (newlinesBetweenAttributes && n++ < 20) {
					buffer.append(' ');
				}
				buffer.append('=');
				OSGiHeader.quote(buffer, value);
			});
	}

	protected boolean newlinesBetweenAttributes() {
		return false;
	}

	@Override
	public HeaderClause clone() {
		try {
			HeaderClause clone = (HeaderClause) super.clone();
			clone.name = this.name;
			clone.attribs = new Attrs(this.attribs);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int compareTo(HeaderClause other) {
		return this.name.compareTo(other.name);
	}

	@SuppressWarnings("deprecation")
	@Override
	public int hashCode() {
		return Objects.hash(attribs, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeaderClause other = (HeaderClause) obj;
		if (attribs == null) {
			if (other.attribs != null)
				return false;
		} else if (!attribs.isEqual(other.attribs))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		formatTo(b);
		return b.toString();
	}

	public static Parameters toParameters(List<? extends HeaderClause> l) {
		Parameters parameters = new Parameters();

		l.forEach(hc -> parameters.put(hc.name, hc.attribs));

		return parameters;
	}

}
