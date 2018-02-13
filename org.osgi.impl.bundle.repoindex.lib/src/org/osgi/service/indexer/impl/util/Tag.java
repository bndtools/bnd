/*
 * Copyright (c) OSGi Alliance (2002, 2006, 2007). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.indexer.impl.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/**
 * The Tag class represents a minimal XML tree. It consist of a named element
 * with a hashtable of named attributes. Methods are provided to walk the tree
 * and get its constituents. The content of a Tag is a list that contains String
 * objects or other Tag objects.
 */
public class Tag {
	Tag						parent;
	String					name;
	Map<String,String>		attributes	= new TreeMap<>();
	Vector<Object>			content		= new Vector<>();
	Vector<String>			comments	= new Vector<>();

	static SimpleDateFormat	format		= new SimpleDateFormat("yyyyMMddHHmmss.SSS");

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name) {
		this.name = name;
	}

	/**
	 * Construct a new Tag with a name.
	 */
	public Tag(String name, Map<String,String> attributes) {
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
	 * Add a new date attribute. The date is formatted as the SimpleDateFormat
	 * describes at the top of this class.
	 */
	public void addAttribute(String key, Date value) {
		attributes.put(key, format.format(value));
	}

	/**
	 * Add a new content string.
	 */
	public void addContent(String string) {
		content.addElement(string);
	}

	/**
	 * Add a new content tag.
	 */
	public void addContent(Tag tag) {
		content.addElement(tag);
		tag.parent = this;
	}

	/**
	 * Add a new content tag.
	 */
	public void addComment(String comment) {
		comments.addElement(comment);
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
	public Vector<Object> getContents() {
		return content;
	}

	/**
	 * Return the contents.
	 */
	public Vector<String> getComments() {
		return comments;
	}

	/**
	 * Return a string representation of this Tag and all its children
	 * recursively.
	 */
	public String toString() {
		StringWriter sw = new StringWriter();
		print(Indent.NONE, new PrintWriter(sw));
		return sw.toString();
	}

	/**
	 * Return only the tags of the first level of descendants that match the
	 * name.
	 */
	public Vector<Object> getContents(String tag) {
		Vector<Object> out = new Vector<>();
		for (Object o : content) {
			if (o instanceof Tag && ((Tag) o).getName().equals(tag))
				out.addElement(o);
		}
		return out;
	}

	/**
	 * Return the whole contents as a String (no tag info and attributes).
	 */
	public String getContentsAsString() {
		StringBuffer sb = new StringBuffer();
		getContentsAsString(sb);
		return sb.toString();
	}

	/**
	 * convenient method to get the contents in a StringBuffer.
	 */
	public void getContentsAsString(StringBuffer sb) {
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
	public void print(Indent indent, PrintWriter pw) {
		boolean empty = (content.size() + comments.size()) == 0;

		printOpen(indent, pw, empty);
		if (!empty) {
			printComments(indent, pw);
			printContents(indent, pw);
			printClose(indent, pw);
		}
	}

	public void printOpen(Indent indent, PrintWriter pw, boolean andClose) {
		indent.print(pw);
		pw.print('<');
		pw.print(name);

		String quote = "\"";
		for (Map.Entry<String,String> e : attributes.entrySet()) {
			String key = e.getKey();
			String value = escape(e.getValue());
			pw.print(' ');
			pw.print(key);
			pw.print("=");
			pw.print(quote);
			pw.print(value.replaceAll("\"", "&quot;"));
			pw.print(quote);
		}

		if (andClose)
			pw.print("/>");
		else
			pw.print('>');
	}

	public void printComments(Indent indent, PrintWriter pw) {
		for (String comment : this.comments) {
			Indent nextIndent = indent.next();
			nextIndent.print(pw);
			pw.print("<!-- ");
			pw.print(escape(comment));
			pw.print(" -->");
		}
	}

	public void printContents(Indent indent, PrintWriter pw) {
		for (Object content : this.content) {
			Indent nextIndent = indent.next();
			if (content instanceof String) {
				formatted(pw, nextIndent, 60, escape((String) content));
			} else if (content instanceof Tag) {
				Tag tag = (Tag) content;
				tag.print(nextIndent, pw);
			}
		}
	}

	public void printClose(Indent indent, PrintWriter pw) {
		indent.print(pw);
		pw.print("</");
		pw.print(name);
		pw.print('>');
	}

	/**
	 * Convenience method to print a string nicely and does character conversion
	 * to entities.
	 */
	void formatted(PrintWriter pw, Indent indent, int width, String s) {
		int pos = width + 1;
		s = s.trim();

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i == 0 || (Character.isWhitespace(c) && pos > width - 3)) {
				indent.print(pw);
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
		if (s == null)
			return "?null?";

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
	 * Make spaces. void spaces(PrintWriter pw, int n) { while (n-- > 0)
	 * pw.print(' '); }
	 */

	/**
	 * root/preferences/native/os
	 */
	public Tag[] select(String path) {
		return select(path, null);
	}

	public Tag[] select(String path, Tag mapping) {
		Vector<Object> v = new Vector<>();
		select(path, v, mapping);
		Tag[] result = new Tag[v.size()];
		v.copyInto(result);
		return result;
	}

	void select(String path, Vector<Object> results, Tag mapping) {
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
			results.addElement(this);
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
			return (tn == null && sn == null) || (sn != null && sn.equals(tn));
		} else {
			String suri = sn == null ? mapping.getAttribute("xmlns") : mapping.getAttribute("xmlns:" + sn);
			String turi = tn == null ? child.findRecursiveAttribute("xmlns")
					: child.findRecursiveAttribute("xmlns:" + tn);
			return turi == suri || (turi != null && suri != null && turi.equals(suri));
		}
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
		StringBuffer sb = new StringBuffer();
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
		} else
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

	public static void convert(Collection<Map<String,String>> c, String type, Tag parent) {
		for (Map<String,String> map : c) {
			parent.addContent(new Tag(type, map));
		}
	}

}
