package aQute.lib.tag;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The Tag class represents a minimal XML tree. It consist of a named element
 * with a hashtable of named attributes. Methods are provided to walk the tree
 * and get its constituents. The content of a Tag is a list that contains String
 * objects or other Tag objects.
 */
public class Tag {

	final private static String		ARRAY_ELEMENT_NAME	= "element";

	final static String				NameStartChar		= ":A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF]\uFDF0-\uFFFD";
	final static String				NameChar			= "[" + NameStartChar
		+ "0-9.\u00B7\u0300-\u036F\u203F-\u2040\\-]";
	final static String				Name				= "[" + NameStartChar + "]" + NameChar + "*";
	final public static Pattern		NAME_P				= Pattern.compile(Name);

	Tag								parent;																																												// Parent
	String							name;																																												// Name
	final Map<String, String>		attributes			= new LinkedHashMap<>();
	final List<Object>				content				= new ArrayList<>();																																			// Content
	static final DateTimeFormatter	DATE_TIME_FORMATTER	= DateTimeFormatter
		.ofPattern("yyyyMMddHHmmss.SSS", Locale.ROOT)
		.withZone(ZoneId.systemDefault());
	boolean							cdata;

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name, Object content) {
		this.name = name;
		this.content.add(content);
	}

	public Tag(String name, Object... contents) {
		this.name = name;
		Collections.addAll(content, contents);
	}

	public Tag(Tag parent, String name, Object... contents) {
		this(name, contents);
		parent.addContent(this);
	}

	public Tag(Tag parent, String name, Object content) {
		this(name, content);
		parent.addContent(this);
	}

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name, Map<String, String> attributes, Object content) {
		this(name, content);
		this.attributes.putAll(attributes);
	}

	public Tag(String name, Map<String, String> attributes, Object... contents) {
		this(name, contents);
		this.attributes.putAll(attributes);
	}

	public Tag(String name, Map<String, String> attributes) {
		this(name, attributes, new Object[0]);
	}

	/**
	 * Construct a new Tag with a name and a set of attributes. The attributes
	 * are given as ( name, value ) ...
	 */
	public Tag(String name, String[] attributes, Object content) {
		this(name, content);
		for (int i = 0; i < attributes.length; i += 2)
			addAttribute(attributes[i], attributes[i + 1]);
	}

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
	 * Add a new date attribute. The date is formatted by DATE_TIME_FORMATTER
	 * described at the top of this class.
	 */
	public Tag addAttribute(String key, Date value) {
		if (value != null) {
			attributes.put(key, DATE_TIME_FORMATTER.format(value.toInstant()));
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
	 * Add a new content tags.
	 */
	public Tag addContent(Map<String, ?> map) {
		map.forEach((name, content) -> addContent(new Tag(name, content)));
		return this;
	}

	/**
	 * Convert the DTO object in arguments to a Tag object with {@code rootName}
	 * as name.
	 * <p>
	 * Keys in {@link Map} and public fields' name are used to name tags.<br/>
	 * Objects in {@link Collection} or {@code array} are converted to
	 * {@code XML} elements and tags names are computed as follow:
	 * <ul>
	 * <li>If the parent element tag does not have a defined name,
	 * {@code arrayElementName} will be used.</li>
	 * <li>If the parent element tag name ends with a 's' or 'S', the
	 * depluralized version will be used.</li>
	 * <li>Otherwise, the first letter of {@code arrayElementName} is
	 * capitalized and appended to the parent element tag name to name the tag
	 * (If the parent element tag name does not end with a lowercase letter,
	 * {@code arrayElementName} is entirely capitalized and an '_' is first
	 * appended to to it)</li>
	 * </ul>
	 * <h3>Example:</h3>
	 *
	 * <pre>
	 * fromDTO("things", "element", `[{"FRIEND": ["Amy"]},{"children": ["Emily"]},["Bob", "Bill"]]`)
	 * </pre>
	 *
	 * gives
	 *
	 * <pre>
	 * {@code
	 * <things>
	 *    <thing>
	 *       <FRIEND>
	 *          <FRIEND_ELEMENT>Amy</FRIEND_ELEMENT>
	 *       </FRIEND>
	 *       <children>
	 *          <childrenElement>Emily</childrenElement>
	 *       </children>
	 *    </thing>
	 *    <thing>
	 *       <element>Bob</element>
	 *       <element>Bill</element>
	 *    </thing>
	 * </things>
	 * }
	 * </pre>
	 * <p>
	 * {@code null} values are ignored.
	 *
	 * @param rootName the name of the root tag, may be {@code null}.
	 * @param arrayElementName a generic name for elements in lists, if
	 *            {@code null} or empty, the default value "element" will be
	 *            used.
	 * @param dto the DTO to convert, if {@code null} an empty element is
	 *            returned.
	 * @return the corresponding Tag, never {@code null}.
	 */
	public static Tag fromDTO(String rootName, String arrayElementName, Object dto) {
		if (arrayElementName != null && !arrayElementName.isEmpty() && dto != null) {
			return convertDTO(rootName, arrayElementName, dto, true);
		} else {
			return fromDTO(rootName, dto);
		}
	}

	/**
	 * Convert the DTO object in arguments to a Tag object with {@code rootName}
	 * as name.
	 * <p>
	 * Keys in {@link Map} and public fields' name are used to name tags.<br/>
	 * Objects in {@link Collection} or {@code array} are converted to
	 * {@code XML} elements and tags names are computed as follow:
	 * <ul>
	 * <li>If the parent element tag does not have a defined name, "element"
	 * will be used.</li>
	 * <li>If the parent element tag name ends with a 's' or 'S', the
	 * depluralized version will be used.</li>
	 * <li>Otherwise, the first letter of "element" is capitalized and appended
	 * to the parent element tag name to name the tag (If the parent element tag
	 * name does not end with a lowercase letter, "element" is entirely
	 * capitalized and an '_' is first appended to to it)</li>
	 * </ul>
	 * <h3>Example:</h3>
	 *
	 * <pre>
	 * fromDTO("things", "element", `[{"FRIEND": ["Amy"]},{"children": ["Emily"]},["Bob", "Bill"]]`)
	 * </pre>
	 *
	 * gives
	 *
	 * <pre>
	 * {@code
	 * <things>
	 *    <thing>
	 *       <FRIEND>
	 *          <FRIEND_ELEMENT>Amy</FRIEND_ELEMENT>
	 *       </FRIEND>
	 *       <children>
	 *          <childrenElement>Emily</childrenElement>
	 *       </children>
	 *    </thing>
	 *    <thing>
	 *       <element>Bob</element>
	 *       <element>Bill</element>
	 *    </thing>
	 * </things>
	 * }
	 * </pre>
	 * <p>
	 * {@code null} values are ignored.
	 *
	 * @param rootName the name of the root tag, may be {@code null}.
	 * @param dto the DTO to convert, if {@code null} an empty element is
	 *            returned.
	 * @return the corresponding Tag, never {@code null}.
	 */
	public static Tag fromDTO(String rootName, Object dto) {
		if (dto == null) {
			return new Tag(rootName);
		} else {
			return convertDTO(rootName, Tag.ARRAY_ELEMENT_NAME, dto, true);
		}
	}

	private static Tag convertDTO(String rootName, String arrayElementName, Object dto, boolean suffix) {
		final Tag result = new Tag(rootName);

		if (isComplex(dto)) {
			if (dto.getClass()
				.isArray()) {
				int length = Array.getLength(dto);
				for (int i = 0; i < length; i++) {
					Object nextDTO = Array.get(dto, i);
					if (nextDTO != null) {
						result.addContent(Tag.convertDTO(
							suffix ? computeArrayElementName(rootName, arrayElementName) : arrayElementName,
							arrayElementName, nextDTO, false));
					}
				}
			} else if (dto instanceof Collection) {
				for (Object d : (Collection<?>) dto) {
					if (d != null) {
						result.addContent(Tag.convertDTO(
							suffix ? computeArrayElementName(rootName, arrayElementName) : arrayElementName,
							arrayElementName, d, false));
					}
				}
			} else if (dto instanceof Map) {
				for (Entry<?, ?> entry : ((Map<?, ?>) dto).entrySet()) {
					if (entry.getValue() != null && entry.getKey() != null) {
						result.addContent(
							Tag.convertDTO(Objects.toString(entry.getKey()), arrayElementName, entry.getValue(), true));
					}
				}
			} else {
				getFields(dto.getClass()).forEach(field -> {
					try {
						MethodHandle mh = MethodHandles.publicLookup()
							.unreflectGetter(field);
						Object nextDTO = mh.invoke(dto);
						if (nextDTO != null) {
							result.addContent(Tag.convertDTO(field.getName(), arrayElementName, nextDTO, true));
						}
						/* should not be thrown if input respect dto spec */
					} catch (Error | RuntimeException bug) {
						throw bug;
					} catch (Throwable bug) {
						throw new RuntimeException(bug);
					}
				});
			}
		} else {
			result.addContent(dto.toString());
		}

		return result;
	}

	private static String computeArrayElementName(String name, String arrayElementName) {
		if (name != null && name.length() > 0) {
			final char lastChar = name.charAt(name.length() - 1);
			if (lastChar == 's' || lastChar == 'S') {
				return name.substring(0, name.length() - 1);
			} else if (lastChar >= 'a' && lastChar <= 'z') {
				return name + arrayElementName.substring(0, 1)
					.toUpperCase(Locale.ROOT) + arrayElementName.substring(1);
			} else {
				return name + "_" + arrayElementName.toUpperCase(Locale.ROOT);
			}
		} else {
			return arrayElementName;
		}
	}

	private static boolean isComplex(Object a) {
		return a instanceof Map || a instanceof Collection || a.getClass()
			.isArray()
			|| getFields(a.getClass()).findAny()
				.isPresent();
	}

	private static Stream<Field> getFields(Class<?> c) {
		return Stream.of(c.getFields())
			.filter(
				field -> !(field.isEnumConstant() || field.isSynthetic() || Modifier.isStatic(field.getModifiers())));
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
			pw.print('"');
		}

		if (content.isEmpty()) {
			pw.print('/');
		} else {
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
						pw.write("<![CDATA[");
						int begin = 0;
						for (int end; (end = s.indexOf("]]>", begin)) >= 0; begin = end + 3) {
							pw.write(s, begin, end - begin);
							pw.print("]]]]><![CDATA[>");
						}
						pw.write(s, begin, s.length() - begin);
						pw.write("]]>");
					} else {
						pw.print(escape(s));
					}
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

		if (!NAME_P.matcher(name)
			.matches()) {
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
}
