package aQute.bnd.build.model.clauses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import aQute.bnd.header.Attrs;

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
			result.add(tokenizer.nextToken().trim());
		}

		return result;
	}

	public void setListAttrib(String attrib, Collection< ? extends String> value) {
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
		formatTo(buffer, null);
	}

	public void formatTo(StringBuilder buffer, Comparator<Entry<String,String>> sorter) {
		String separator = newlinesBetweenAttributes() ? INTERNAL_LIST_SEPARATOR_NEWLINES : INTERNAL_LIST_SEPARATOR;
		// If the name contains a comma, then quote the whole thing
		String tmpName = name;
		if (tmpName.indexOf(',') > -1)
			tmpName = "'" + tmpName + "'";
		buffer.append(tmpName);

		if (attribs != null) {
			Set<Entry<String,String>> set;
			if (sorter != null) {
				set = new TreeSet<>(sorter);
				set.addAll(attribs.entrySet());
			} else {
				set = attribs.entrySet();
			}

			for (Iterator<Entry<String,String>> iter = set.iterator(); iter.hasNext();) {
				Entry<String,String> entry = iter.next();
				String name = entry.getKey();
				String value = entry.getValue();

				if (value != null && value.length() > 0) {
					buffer.append(separator);

					// If the value contains any comma or equals, then quote the
					// whole thing
					if (value.indexOf(',') > -1 || value.indexOf('=') > -1)
						value = "'" + value + "'";

					buffer.append(name).append('=').append(value);
				}
			}
		}
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
}
