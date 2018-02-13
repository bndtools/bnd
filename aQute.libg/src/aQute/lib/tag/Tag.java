package aQute.lib.tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;

import aQute.lib.exceptions.Exceptions;

/**
 * The Tag class represents a minimal XML tree. It consist of a named element
 * with a hashtable of named attributes. Methods are provided to walk the tree
 * and get its constituents. The content of a Tag is a list that contains String
 * objects or other Tag objects.
 */
public class Tag {

	final static String				NameStartChar	= ":A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF]\uFDF0-\uFFFD";
	final static String				NameChar		= "[" + NameStartChar + "0-9.\u00B7\u0300-\u036F\u203F-\u2040\\-]";
	final static String				Name			= "[" + NameStartChar + "]" + NameChar + "*";
	final public static Pattern		NAME_P			= Pattern.compile(Name);

	Tag								parent;														// Parent
	String							name;														// Name
	final Map<String,String>		attributes	= new LinkedHashMap<>();
	final List<Object>				content		= new ArrayList<>();						// Content
	final static SimpleDateFormat	format		= new SimpleDateFormat("yyyyMMddHHmmss.SSS");
	boolean							cdata;

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name, Object... contents) {
		this.name = name;
		Collections.addAll(content, contents);
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
	 * Construct a new Tag with a name and a set of attributes. The attributes are
	 * given as ( name, value ) ...
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
		if (value != null) {
			synchronized (format) {
				attributes.put(key, format.format(value));
			}
		}
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
	 * Add new content tags derived from the DTO value.
	 */
	public Tag addContent(String name, Object dto) {
		if (dto != null) {
			if (isComplex(dto)) {
				List<Object> flattedDtos = new LinkedList<>();

				if (flatCollection(dto, flattedDtos, true)) {
					for (Object d : flattedDtos) {
						addContent(name, d);
					}
				} else if (dto instanceof Map) {
					Tag tag = new Tag(name);
					for (Entry< ? , ? > entry : ((Map< ? , ? >) dto).entrySet()) {
						tag.addContent(Objects.toString(entry.getKey()), entry.getValue());
					}
					if (!tag.content.isEmpty()) {
						addContent(tag);
					}
				} else {
					Tag tag = new Tag(name);
					for (Field field : getFields(dto.getClass())) {
						try {
							tag.addContent(field.getName(), field.get(dto));
						} catch (IllegalAccessException bug) {
							/* should not be thrown if input respect dto spec */
							throw new RuntimeException(bug);
						}
					}
					addContent(tag);
				}
			} else {
				addContent(new Tag(name, dto.toString()));
			}
		}
		return this;
	}

	private boolean flatCollection(Object dto, List<Object> result, boolean topLevel) {
		if (dto != null) {
			if (dto.getClass().isArray()) {
				int length = Array.getLength(dto);
				for (int i = 0; i < length; i++) {
					flatCollection(Array.get(dto, i), result, false);
				}

				return true;
			} else if (dto instanceof Collection) {
				for (Object d : (Collection< ? >) dto) {
					flatCollection(d, result, false);
				}

				return true;
			} else if (!topLevel) {
				result.add(dto);
			}
		}
		return false;
	}

	private boolean isComplex(Object a) {
		return a instanceof Map || a instanceof Collection || a.getClass().isArray()
				|| getFields(a.getClass()).length > 0;
	}

	private Field[] getFields(Class< ? > c) {
		List<Field> publicFields = new ArrayList<>();

		for (Field field : c.getFields()) {
			if (field.isEnumConstant() || field.isSynthetic() || Modifier.isStatic(field.getModifiers()))
				continue;
			publicFields.add(field);
		}

		return publicFields.toArray(new Field[publicFields.size()]);
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
	 * Return a string representation of this Tag and all its children recursively.
	 */
	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		print(0, new PrintWriter(sw));
		return sw.toString();
	}

	/**
	 * Return only the tags of the first level of descendants that match the name.
	 */
	public List<Object> getContents(String tag) {
		List<Object> out = new ArrayList<>();
		for (Object o : content) {
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
				sb.append(o);
		}
	}

	/**
	 * Print the tag formatted to a PrintWriter.
	 */
	public Tag print(int indent, PrintWriter pw) {
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
			Object last = null;
			for (Object c : content) {
				if (c instanceof Tag) {
					if ((last == null) && (indent >= 0)) {
						pw.print('\n');
					}
					Tag tag = (Tag) c;
					tag.print(indent + 2, pw);
				} else {
					if (c == null)
						continue;

					String s = c.toString();

					if (cdata) {
						pw.print("<![CDATA[");
						s = s.replaceAll("]]>", "]]]]><![CDATA[>");
						pw.print(s);
						pw.print("]]>");
					} else
						pw.print(escape(s));
				}
				last = c;
			}
			if (last instanceof Tag) {
				spaces(pw, indent);
			}
			pw.print("</");
			pw.print(name);
		}
		pw.print('>');
		if (indent >= 0) {
			pw.print('\n');
		}
		return this;
	}

	/**
	 * Escape a string, do entity conversion.
	 */
	public static String escape(String s) {
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
		return select(path, null);
	}

	public Collection<Tag> select(String path, Tag mapping) {
		List<Tag> v = new ArrayList<>();
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

	public String compact() {
		StringWriter sw = new StringWriter();
		print(Integer.MIN_VALUE, new PrintWriter(sw));
		return sw.toString();
	}

	public String validate() {
		try (Formatter f = new Formatter()) {
			if (invalid(f))
				return f.toString();
			else
				return null;
		}
	}

	boolean invalid(Formatter f) {
		boolean invalid = false;

		if (!NAME_P.matcher(name).matches()) {
			f.format("%s: Invalid name %s\n", getPath(), name);
		}

		for (Object o : content) {
			if (o instanceof Tag) {
				invalid |= ((Tag) o).invalid(f);
			}
		}
		return invalid;
	}

	private String getPath() {
		if (parent == null)
			return name;
		else
			return parent.getPath() + "/" + name;
	}

	public InputStream toInputStream() {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(bout, "UTF-8"));
			this.print(2, pw);
			pw.flush();
			return new ByteArrayInputStream(bout.toByteArray());
		} catch (UnsupportedEncodingException e) {
			// impossible
			throw Exceptions.duck(e);
		}
	}
}
