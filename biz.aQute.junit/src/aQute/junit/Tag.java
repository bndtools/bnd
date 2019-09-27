package aQute.junit;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Tag class represents a minimal XML tree. It consist of a named element
 * with a map of named attributes. Methods are provided to walk the tree and get
 * its constituents. The content of a Tag is a list that contains String objects
 * or other Tag objects.
 */
public class Tag {
	Tag								parent;														// Parent
																								// element
	String							name;														// Name
																								// of
																								// the
																								// tag
	Map<String, String>				attributes	= new LinkedHashMap<>();						// Attributes
																								// name
																								// ->
																								// value
	List<Object>					content		= new ArrayList<>();							// Content
																								// elements
	boolean							cdata;

	static final DateTimeFormatter	DATE_TIME_FORMATTER	= DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS", Locale.ROOT)
		.withZone(ZoneId.systemDefault());

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name) {
		this.name = name;
	}

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name, Map<String, String> attributes) {
		this.name = name;
		this.attributes = attributes;
	}

	/**
	 * Construct a new Tag with a name and a set of attributes. The attributes
	 * are given as ( name, value ) ...
	 */
	public Tag(String name, String[] attributes) {
		this.name = name;
		for (int i = 0; i < attributes.length; i += 2)
			addAttribute(attributes[i], attributes[i + 1]);
	}

	/**
	 * Construct a new Tag with a single string as content.
	 */
	public Tag(String name, String content) {
		this.name = name;
		addContent(content);
	}

	public Tag(Tag testsuite, String name) {
		this(name);
		testsuite.addContent(this);
	}

	/**
	 * Add a new attribute.
	 */
	public void addAttribute(String key, String value) {
		attributes.put(key, value);
	}

	/**
	 * Add a new attribute.
	 */
	public void addAttribute(String key, Object value) {
		if (value == null)
			return;
		attributes.put(key, value.toString());
	}

	/**
	 * Add a new attribute.
	 */
	public void addAttribute(String key, int value) {
		attributes.put(key, Integer.toString(value));
	}

	/**
	 * Add a new date attribute. The date is formatted by DATE_TIME_FORMATTER
	 * described at the top of this class.
	 */
	public void addAttribute(String key, Date value) {
		attributes.put(key, DATE_TIME_FORMATTER.format(value.toInstant()));
	}

	/**
	 * Add a new content string.
	 */
	public void addContent(String string) {
		content.add(string);
	}

	/**
	 * Add a new content tag.
	 */
	public void addContent(Tag tag) {
		content.add(tag);
		tag.parent = this;
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
	public Map<String, String> getAttributes() {
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
	@Override
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
		List<Object> out = new ArrayList<>();
		for (Object o : content) {
			if (o instanceof Tag && ((Tag) o).getName()
				.equals(tag))
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
	public void print(int indent, PrintWriter pw) {
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
			pw.print('"');
		}

		if (content.isEmpty()) {
			pw.print('/');
		} else {
			pw.print('>');
			for (Object content : content) {
				if (content instanceof String) {
					String s = (String) content;
					if (cdata) {
						pw.write("<![CDATA[");
						int begin = 0;
						for (int end; (end = s.indexOf("]]>", begin)) >= 0; begin = end + 3) {
							pw.write(s, begin, end - begin);
							pw.print("]]]]><![CDATA[>");
						}
						pw.write(s, begin, s.length() - begin);
						pw.write("]]>");
					} else {
						formatted(pw, indent + 2, 60, s);
					}
				} else if (content instanceof Tag) {
					Tag tag = (Tag) content;
					tag.print(indent + 2, pw);
				} else if (content instanceof URL) {
					copyURL(pw, (URL) content);
				}
			}
			pw.print("\n");
			spaces(pw, indent);
			pw.print("</");
			pw.print(name);
		}
		pw.print('>');
	}

	private void copyURL(PrintWriter pw, URL url) {
		try {
			try (InputStream in = url.openStream();
				BufferedReader rdr = new BufferedReader(new InputStreamReader(in, UTF_8))) {
				String line = rdr.readLine();
				if (line != null) {
					while (line != null && line.trim()
						.startsWith("<?"))
						line = rdr.readLine();

					while (line != null) {
						pw.println(line);
						line = rdr.readLine();
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Problems copying extra XML");
		}
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
				case '"' :
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
	public Tag[] select(String path) {
		return select(path, null);
	}

	public Tag[] select(String path, Tag mapping) {
		List<Tag> v = new ArrayList<>();
		select(path, v, mapping);
		return v.toArray(new Tag[0]);
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
				if (child.getName()
					.equals(elementName) || elementName.equals("*"))
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
		Tag tags[] = select(path);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tags.length; i++) {
			if (attribute == null)
				tags[i].getContentsAsString(sb);
			else
				sb.append(tags[i].getAttribute(attribute));
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

	public void addContent(URL url) {
		content.add(url);
	}

	public void setCDATA() {
		cdata = true;
	}
}
