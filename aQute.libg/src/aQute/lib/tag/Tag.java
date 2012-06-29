package aQute.lib.tag;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * The Tag class represents a minimal XML tree. It consist of a named element
 * with a hashtable of named attributes. Methods are provided to walk the tree
 * and get its constituents. The content of a Tag is a list that contains String
 * objects or other Tag objects.
 */
public class Tag {
	Tag							parent;													// Parent
	String						name;														// Name
	final Map<String,String>	attributes	= new LinkedHashMap<String,String>();
	final List<Object>			content		= new ArrayList<Object>();						// Content
	SimpleDateFormat			format		= new SimpleDateFormat("yyyyMMddHHmmss.SSS");
	boolean						cdata;

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name, Object... contents) {
		this.name = name;
		for (Object c : contents)
			content.add(c);
	}

	public Tag(Tag parent, String name, Object... contents) {
		this(name, contents);
		parent.addContent(this);
	}

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name, Map<String,String> attributes, Object... contents) {
		this(name, contents);
		this.attributes.putAll(attributes);

	}

	public Tag(String name, Map<String,String> attributes) {
		this(name, attributes, new Object[0]);
	}

	/**
	 * Construct a new Tag with a name and a set of attributes. The attributes
	 * are given as ( name, value ) ...
	 */
	public Tag(String name, String[] attributes, Object... contents) {
		this(name, contents);
		for (int i = 0; i < attributes.length; i += 2)
			addAttribute(attributes[i], attributes[i + 1]);
	}

	public Tag(String name, String[] attributes) {
		this(name, attributes, new Object[0]);
	}

	/**
	 * Add a new attribute.
	 */
	public Tag addAttribute(String key, String value) {
		if (value != null)
			attributes.put(key, value);
		return this;
	}

	/**
	 * Add a new attribute.
	 */
	public Tag addAttribute(String key, Object value) {
		if (value == null)
			return this;
		attributes.put(key, value.toString());
		return this;
	}

	/**
	 * Add a new attribute.
	 */
	public Tag addAttribute(String key, int value) {
		attributes.put(key, Integer.toString(value));
		return this;
	}

	/**
	 * Add a new date attribute. The date is formatted as the SimpleDateFormat
	 * describes at the top of this class.
	 */
	public Tag addAttribute(String key, Date value) {
		if (value != null)
			attributes.put(key, format.format(value));
		return this;
	}

	/**
	 * Add a new content string.
	 */
	public Tag addContent(String string) {
		if (string != null)
			content.add(string);
		return this;
	}

	/**
	 * Add a new content tag.
	 */
	public Tag addContent(Tag tag) {
		content.add(tag);
		tag.parent = this;
		return this;
	}

	/**
	 * Return the name of the tag.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the attribute value.
	 */
	public String getAttribute(String key) {
		return attributes.get(key);
	}

	/**
	 * Return the attribute value or a default if not defined.
	 */
	public String getAttribute(String key, String deflt) {
		String answer = getAttribute(key);
		return answer == null ? deflt : answer;
	}

	/**
	 * Answer the attributes as a Dictionary object.
	 */
	public Map<String,String> getAttributes() {
		return attributes;
	}

	/**
	 * Return the contents.
	 */
	public List<Object> getContents() {
		return content;
	}

	/**
	 * Return a string representation of this Tag and all its children
	 * recursively.
	 */
	public String toString() {
		StringWriter sw = new StringWriter();
		print(0, new PrintWriter(sw));
		return sw.toString();
	}

	/**
	 * Return only the tags of the first level of descendants that match the
	 * name.
	 */
	public List<Object> getContents(String tag) {
		List<Object> out = new ArrayList<Object>();
		for (Object o : out) {
			if (o instanceof Tag && ((Tag) o).getName().equals(tag))
				out.add(o);
		}
		return out;
	}

	/**
	 * Return the whole contents as a String (no tag info and attributes).
	 */
	public String getContentsAsString() {
		StringBuilder sb = new StringBuilder();
		getContentsAsString(sb);
		return sb.toString();
	}

	/**
	 * convenient method to get the contents in a StringBuilder.
	 */
	public void getContentsAsString(StringBuilder sb) {
		for (Object o : content) {
			if (o instanceof Tag)
				((Tag) o).getContentsAsString(sb);
			else
				sb.append(o.toString());
		}
	}

	/**
	 * Print the tag formatted to a PrintWriter.
	 */
	public Tag print(int indent, PrintWriter pw) {
		pw.print("\n");
		spaces(pw, indent);
		pw.print('<');
		pw.print(name);

		for (String key : attributes.keySet()) {
			String value = escape(attributes.get(key));
			pw.print(' ');
			pw.print(key);
			pw.print("=\"");
			pw.print(value);
			pw.print("\"");
		}

		if (content.size() == 0)
			pw.print('/');
		else {
			pw.print('>');
			for (Object c : content) {
				if (c instanceof String) {
					if (cdata) {
						pw.print("<![CDATA[");
						String s = (String) c;
						s = s.replaceAll("]]>", "] ]>");
						pw.print(s);
						pw.print("]]>");
					} else
						formatted(pw, indent + 2, 60, escape((String) c));
				} else if (c instanceof Tag) {
					Tag tag = (Tag) c;
					tag.print(indent + 2, pw);
				}
			}
			pw.print("\n");
			spaces(pw, indent);
			pw.print("</");
			pw.print(name);
		}
		pw.print('>');
		return this;
	}

	/**
	 * Convenience method to print a string nicely and does character conversion
	 * to entities.
	 */
	void formatted(PrintWriter pw, int left, int width, String s) {
		int pos = width + 1;
		s = s.trim();

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i == 0 || (Character.isWhitespace(c) && pos > width - 3)) {
				pw.print("\n");
				spaces(pw, left);
				pos = 0;
			}
			switch (c) {
				case '<' :
					pw.print("&lt;");
					pos += 4;
					break;
				case '>' :
					pw.print("&gt;");
					pos += 4;
					break;
				case '&' :
					pw.print("&amp;");
					pos += 5;
					break;
				default :
					pw.print(c);
					pos++;
					break;
			}

		}
	}

	/**
	 * Escape a string, do entity conversion.
	 */
	String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '<' :
					sb.append("&lt;");
					break;
				case '>' :
					sb.append("&gt;");
					break;
				case '\"' :
					sb.append("&quot;");
					break;
				case '&' :
					sb.append("&amp;");
					break;
				default :
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

	/**
	 * Make spaces.
	 */
	void spaces(PrintWriter pw, int n) {
		while (n-- > 0)
			pw.print(' ');
	}

	/**
	 * root/preferences/native/os
	 */
	public Collection<Tag> select(String path) {
		return select(path, (Tag) null);
	}

	public Collection<Tag> select(String path, Tag mapping) {
		List<Tag> v = new ArrayList<Tag>();
		select(path, v, mapping);
		return v;
	}

	void select(String path, List<Tag> results, Tag mapping) {
		if (path.startsWith("//")) {
			int i = path.indexOf('/', 2);
			String name = path.substring(2, i < 0 ? path.length() : i);

			for (Object o : content) {
				if (o instanceof Tag) {
					Tag child = (Tag) o;
					if (match(name, child, mapping))
						results.add(child);
					child.select(path, results, mapping);
				}

			}
			return;
		}

		if (path.length() == 0) {
			results.add(this);
			return;
		}

		int i = path.indexOf("/");
		String elementName = path;
		String remainder = "";
		if (i > 0) {
			elementName = path.substring(0, i);
			remainder = path.substring(i + 1);
		}

		for (Object o : content) {
			if (o instanceof Tag) {
				Tag child = (Tag) o;
				if (child.getName().equals(elementName) || elementName.equals("*"))
					child.select(remainder, results, mapping);
			}
		}
	}

	public boolean match(String search, Tag child, Tag mapping) {
		String target = child.getName();
		String sn = null;
		String tn = null;

		if (search.equals("*"))
			return true;

		int s = search.indexOf(':');
		if (s > 0) {
			sn = search.substring(0, s);
			search = search.substring(s + 1);
		}
		int t = target.indexOf(':');
		if (t > 0) {
			tn = target.substring(0, t);
			target = target.substring(t + 1);
		}

		if (!search.equals(target)) // different tag names
			return false;

		if (mapping == null) {
			return tn == sn || (sn != null && sn.equals(tn));
		}
		String suri = sn == null ? mapping.getAttribute("xmlns") : mapping.getAttribute("xmlns:" + sn);
		String turi = tn == null ? child.findRecursiveAttribute("xmlns") : child.findRecursiveAttribute("xmlns:" + tn);
		return ((turi == null) && (suri == null)) || ((turi != null) && turi.equals(suri));
	}

	public String getString(String path) {
		String attribute = null;
		int index = path.indexOf("@");
		if (index >= 0) {
			// attribute
			attribute = path.substring(index + 1);

			if (index > 0) {
				// prefix path
				path = path.substring(index - 1); // skip -1
			} else
				path = "";
		}
		Collection<Tag> tags = select(path);
		StringBuilder sb = new StringBuilder();
		for (Tag tag : tags) {
			if (attribute == null)
				tag.getContentsAsString(sb);
			else
				sb.append(tag.getAttribute(attribute));
		}
		return sb.toString();
	}

	public String getStringContent() {
		StringBuilder sb = new StringBuilder();
		for (Object c : content) {
			if (!(c instanceof Tag))
				sb.append(c);
		}
		return sb.toString();
	}

	public String getNameSpace() {
		return getNameSpace(name);
	}

	public String getNameSpace(String name) {
		int index = name.indexOf(':');
		if (index > 0) {
			String ns = name.substring(0, index);
			return findRecursiveAttribute("xmlns:" + ns);
		}
		return findRecursiveAttribute("xmlns");
	}

	public String findRecursiveAttribute(String name) {
		String value = getAttribute(name);
		if (value != null)
			return value;
		if (parent != null)
			return parent.findRecursiveAttribute(name);
		return null;
	}

	public String getLocalName() {
		int index = name.indexOf(':');
		if (index <= 0)
			return name;

		return name.substring(index + 1);
	}

	public void rename(String string) {
		name = string;
	}

	public void setCDATA() {
		cdata = true;
	}

}
