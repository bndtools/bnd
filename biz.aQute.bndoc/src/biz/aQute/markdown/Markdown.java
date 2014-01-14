package biz.aQute.markdown;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.lib.env.*;
import aQute.lib.io.*;
import aQute.libg.sed.*;

public class Markdown extends Env {
	static Pattern					CONTENT_P				= Pattern.compile("\\$\\{(content|css)\\}");
	static final String				PROPERTY_S				= "(?<key>\\S*)\\s*[:=]\\s*(?<value>.*(\\s*\n +.+)*)\n";
	static final Pattern			PROPERTIES_P			= Pattern.compile("\\[meta\\]\n(" + PROPERTY_S + ")+");
	private static final Pattern	PROPERTY_P				= Pattern.compile(PROPERTY_S);

	static String					URL_S					= "((https?|ftps?):/(/[-%\\w\\d?+&@#.~=]*)+|(mailto:)?[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4})";

	static int						FLAGS					= Pattern.COMMENTS + Pattern.MULTILINE + Pattern.UNIX_LINES;
	static String					ENTITIES_S				= "&(\\#\\d+|Acirc|acirc|acute|AElig|aelig|Agrave|agrave|alefsym|Alpha|"
																	+ "alpha|amp|and|ang|apos|Aring|aring|asymp|Atilde|atilde|Auml|auml|bdquo|Beta|beta|brvbar|bull|"
																	+ "cap|Ccedil|ccedil|cedil|cent|Chi|chi|circ|clubs|cong|copy|crarr|cup|curren|Dagger|dagger|dArr|"
																	+ "darr|deg|Delta|delta|diams|divide|Eacute|eacute|Ecirc|ecirc|Egrave|egrave|empty|emsp|ensp|"
																	+ "Epsilon|epsilon|equiv|Eta|eta|ETH|eth|Euml|euml|euro|exist|fnof|forall|frac12|frac14|frac34|"
																	+ "frasl|Gamma|gamma|ge|gt|hArr|harr|hearts|hellip|Iacute|iacute|Icirc|icirc|iexcl|Igrave|igrave|"
																	+ "image|infin|int|Iota|iota|iquest|isin|Iuml|iuml|Kappa|kappa|Lambda|lambda|lang|laquo|lArr|larr|"
																	+ "lceil|ldquo|le|lfloor|lowast|loz|lrm|lsaquo|lsquo|lt|macr|mdash|micro|middot|minus|Mu|mu|nabla|"
																	+ "nbsp|ndash|ne|ni|not|notin|nsub|Ntilde|ntilde|Nu|nu|Oacute|oacute|Ocirc|ocirc|OElig|oelig|Ograve|"
																	+ "ograve|oline|Omega|omega|Omicron|omicron|oplus|or|ordf|ordm|Oslash|oslash|Otilde|otilde|otimes|"
																	+ "Ouml|ouml|para|part|permil|perp|Phi|phi|Pi|pi|piv|plusmn|pound|Prime|prime|prod|prop|Psi|psi|"
																	+ "quot|radic|rang|raquo|rArr|rarr|rceil|rdquo|real|reg|rfloor|Rho|rho|rlm|rsaquo|rsquo|sbquo|"
																	+ "Scaron|scaron|sdot|sect|shy|Sigma|sigma|sigmaf|sim|spades|sub|sube|sum|sup|sup1|sup2|sup3|"
																	+ "supe|szlig|Tau|tau|there4|Theta|theta|thetasym|thinsp|thorn|tilde|times|trade|Uacute|uacute|"
																	+ "uArr|uarr|Ucirc|ucirc|Ugrave|ugrave|uml|upsih|Upsilon|upsilon|Uuml|uuml|weierp|Xi|xi|Yacute|"
																	+ "yacute|yen|Yuml|yuml|Zeta|zeta|zwj|zwnj);";

	static Pattern					BLOCK_ELEMENT_P			= Pattern
																	.compile("address|blockquote|del|div|dl|fieldset|form|h1|h2|h3|h4|h5|h6|ins|ol|p|pre|table|ul|article|figure|output|aside|footer|audio|section|canvas|header|dd|hgroup|tfoot|video|noscript");
	static Pattern					UNSAFE_ELEMENT_P		= Pattern
																	.compile("applet|head|html|body|frame|frameset|iframe|script|object");

	static Pattern					ENTITIES_P				= Pattern.compile("^" + ENTITIES_S + ".*");

	static String					TAG_S					= "<\\s*(?<end>/)?\\s*(?<tag>[\\w]{2,30})[^>]*/?>";
	static Pattern					TAG_P					= Pattern.compile(TAG_S, FLAGS);

	private static final Pattern	SUB_SUP_P				= Pattern.compile("(~|\\^)(([^ ~]|\\\\ )+)\\1");
	private static final Pattern	STRIKE_OUT_P			= Pattern.compile("---(.+(\n.+)*)---");
	/*
	 * Except inside a code block or inline code, any punctuation or space
	 * character preceded by a backslash will be treated literally, even if it
	 * would normally indicate formatting. Thus, for example, if one writes
	 * \*hello\** one will get <em>*hello*</em> instead of
	 * <strong>hello</strong> This rule is easier to remember than standard
	 * markdown’s rule, which allows only the following characters to be
	 * backslash-escaped: \`*_{}[]()>#+-.!
	 */
	/*
	 * TODO (However, if the markdown_strict format is used, the standard
	 * markdown rule will be used.)
	 */
	static Pattern					EM_P					= Pattern
																	.compile(
																			"(?<type>_{1,3}|\\*{1,3})(?<text>(\\\\.|.)+?)(?!\\\\)\\1",
																			FLAGS);
	static Pattern					INLINE_CODE_P			= Pattern.compile("`(`?)(?<text>([^`]|\\\\'|\\1)+)\\1`",
																	FLAGS);
	static Pattern					SINGLE_EM_OR_STRING_P	= Pattern.compile(" [*_]{1,3} ");

	static String					LINK_S					=
															/*
															 * Markdown supports
															 * two style of
															 * links: inline and
															 * reference. In
															 * both styles, the
															 * link text is
															 * delimited by
															 * [square
															 * brackets]. To
															 * create an inline
															 * link, use a set
															 * of regular
															 * parentheses
															 * immediately after
															 * the link text’s
															 * closing square
															 * bracket. For
															 * example: <pre>
															 * This is [an
															 * example
															 * ](http://example
															 * .com/ "Title")
															 * inline link.
															 * [This
															 * link](http:/
															 * /example.net/)
															 * has no title
															 * attribute. </pre>
															 */
															"\\[ *(?<text>([^\\[]|\\[.*?\\])*) *\\]"
															/*
															 * Either a
															 * reference or an
															 * inline
															 */
															+ "("
															/*
										  * 
										  */
															+ "( *\n?\\[(?<anchor>[^\\]]*)\\])"
															/*
															 * OR
															 */
															+ "|"
															/*
															 * Inside the
															 * parentheses, put
															 * the URL where you
															 * want the link to
															 * point, along with
															 * an optional title
															 * for the link,
															 * surrounded in
															 * quotes.
															 */
															+ "\\( *("
															/*
															 * The link URL may,
															 * optionally, be
															 * surrounded by
															 * angle brackets:
															 */
															+ "(<(?<hrefa>[^>\n]+)>)"
															/*
															 * OR
															 */
															+ "|"
															/*
															 * Direct href
															 */
															+ "(?<href>[^\n\")]+|)"
															/*
															 * along with an
															 * optional title
															 * for the link,
															 * surrounded in
															 * quotes.
															 */
															+ "(\\s+\"(?<title>.+)\")?"
															/*
															 * The closing
															 * parentheses
															 */
															+ ")\\s*\\)"
															/*
															 * The end of the
															 * optional inline
															 * group
															 */
															+ ")";
	static Pattern					AUTOLINK_P				= Pattern.compile("< *(?<url>" + URL_S + ") *>",
																	Pattern.CASE_INSENSITIVE);

	static String					IMAGE_S					= "!" + LINK_S;
	static Pattern					LINK_P					= Pattern.compile(LINK_S, Pattern.UNIX_LINES);
	static Pattern					IMAGE_P					= Pattern.compile(IMAGE_S, Pattern.UNIX_LINES);
	static Pattern					BR_P					= Pattern.compile("\\s\\s+\n");

	static Pattern					EMPTY_P					= Pattern.compile("\n+", Pattern.UNIX_LINES);

	final Map<String,Link>			links					= new HashMap<>();
	final StringBuilder				css						= new StringBuilder();
	final List<Block>				tree					= new ArrayList<>();
	final List<Handler>				handlers				= new ArrayList<>();
	final ReplacerAdapter			macros					= new ReplacerAdapter(this);
	final Configuration				configuration			= config(Configuration.class, "markdown.");
	
	File							resourcesDir;
	String							resourcesPrefix;

	{
		addHandler(new ParaHandler());
		addHandler(new LinkHandler());
		addHandler(new CodeHandler());
		addHandler(new ListHandler());
		addHandler(new RuleHandler());
		addHandler(new BlockQuoteHandler());
		addHandler(new HeaderHandler());
		addHandler(new XmlHandler());
		addHandler(new TableHandler());
	}

	public void addHandler(Markdown.Handler handler) throws Exception {
		handler.configure(this);
		macros.addTarget(handler);
		handlers.add(0, handler);
	}

	public interface Handler {
		void configure(Markdown markdown) throws Exception;

		Block process(Rover rover) throws Exception;
	}

	public static class Link {
		public String	id;
		public String	url;
		public String	title;
	}

	public static class Rover extends Reader {
		StringBuilder	text;
		int				rover;
		Matcher			matcher;
		private boolean	disable;

		public Rover(CharSequence content) {
			text = new StringBuilder(content.length());
			int pos = 0;
			int previousSpaces = 0;

			for (int i = 0; i < content.length(); i++) {
				char c = content.charAt(i);
				switch (c) {
					case '\r' :
						break;

					case '\n' :
						if (previousSpaces == pos) {
							// empty
							text.delete(text.length() - previousSpaces, text.length());
						}
						text.append("\n");
						pos = previousSpaces = 0;
						break;

					case '\t' :
						int count = 4 - pos % 4;

						for (int j = 0; j < count; j++) {
							text.append(' ');
						}
						pos += count;
						previousSpaces += count;
						break;

					case ' ' :
						previousSpaces++;
						text.append(" ");
						pos++;
						break;

					default :
						text.append(c);
						previousSpaces = 0;
						pos++;
						break;
				}
			}
			text.append("\n\n");
		}

		public boolean at(Pattern p) {
			matcher = p.matcher(text);
			matcher.region(rover, text.length());
			return matcher.lookingAt();
		}

		public boolean isEof() {
			return text.length() <= rover;
		}

		public String consume() {
			rover = matcher.end();
			return matcher.group();
		}

		@Override
		public int read(char[] cbuf, int off, int len) {
			if (disable)
				return -1;

			int i = 0;
			for (; i < len && rover + i < text.length(); i++) {
				char c = text.charAt(rover + i);
				cbuf[off + i] = c;
				if (c == '\n')
					break;
			}
			rover = rover + i + 1;
			return i;
		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub

		}

		public String toString() {
			if (isEof())
				return "eof";

			return text.substring(rover, Math.min(rover + 40, text.length()));
		}

		public int mark() {
			return rover;
		}

		public void disable() {
			this.disable = true;
		}

		public void enable() {
			this.disable = false;
		}

		public String group(String string) {
			return matcher.group(string);
		}

		public void next() {
			rover = matcher.end();
		}

		public void error(String format, Object... args) {
			// TODO add line + file
			System.out.printf(format, args);
		}
	}

	public static class Block {
		String				tag;
		Map<String,String>	attributes;

		public Block attr(String key, String value) {
			if (attributes == null)
				attributes = new HashMap<String,String>();
			attributes.put(key, value);
			return this;
		}

		public Block style(String value) {
			if (attributes == null)
				attributes = new HashMap<String,String>();
			String prev = attributes.get("style");
			if (prev == null)
				prev = value;
			else
				prev += ";" + value;
			attributes.put("style", prev);
			return this;
		}

		public Block clazz(String value) {
			if (attributes == null)
				attributes = new HashMap<String,String>();
			String prev = attributes.get("class");
			if (prev == null)
				prev = value;
			else
				prev += " " + value;
			attributes.put("class", prev);
			return this;
		}

		public Block(String tag) {
			this.tag = tag;
		}

		public Block() {
			this.tag = null;
		}

		public void append(Formatter a) {

		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public void beginTag(Formatter f) {
			if (tag != null) {
				f.format("<%s", tag);
				doAttributes(f);
				f.format(">");
			}
		}

		public void doAttributes(Formatter f) {
			if (attributes != null) {
				for (String key : attributes.keySet()) {
					f.format(" %s=\"%s\"", key, escape(attributes.get(key), true));
				}
			}
		}

		public void endTag(Formatter f) {
			if (tag != null)
				f.format("</%s>\n", tag);
		}
	}

	public class Literal extends Block {
		CharSequence	content;

		public Literal(CharSequence text) {
			this.content = text;
		}

		public void append(Formatter a) {
			a.format("%s", content);
		}
	}

	public class Escaped extends Block {
		CharSequence	content;

		public Escaped(CharSequence text) {
			this.content = text;
		}

		public void append(Formatter a) {
			a.format("%s", escape(content, false));
		}
	}

	public static class CompositeBlock extends Block {
		List<Block>	contents;

		public CompositeBlock(String tag, List<Block> list) {
			super(tag);
			this.contents = list;
		}

		public void append(Formatter a) {
			beginTag(a);

			for (Block b : contents) {
				b.append(a);
			}

			endTag(a);
		}

		public void compress() {
			for (Block b : contents) {
				if (b instanceof Paragraph)
					b.setTag(null);
			}
		}

	}

	public class Paragraph extends Block {
		CharSequence	text;

		public Paragraph(String para) {
			super("p");
			text = para.trim();
		}

		public Paragraph(CharSequence text2, String tag) {
			super(tag);
			text = text2 != null ? text2 : null;
		}

		public void append(Formatter a) {
			if (text != null && text.length() > 0) {
				beginTag(a);
				para(a, text);
				endTag(a);
			} else {
				a.format("<%s", tag);
				doAttributes(a);
				a.format(" />");
			}
		}

		public void noTag() {
			tag = null;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		@Override
		public String toString() {
			return "Paragraph ["
					+ (text != null ? "text=" + text.subSequence(0, Math.min(text.length(), 50)) + ", " : "")
					+ (tag != null ? "tag=" + tag : "") + "]";
		}

	}

	public Markdown() throws Exception {
		for (Handler h : handlers) {
			h.configure(this);
		}
	}

	public Markdown(CharSequence string) throws Exception {
		this();
		parse(string);
	}

	public Markdown(Env main) throws Exception {
		super(main);
	}

	public Markdown parse(CharSequence content) throws Exception {
		content = parseProperties(content);
		tree.addAll(parseContent(content));
		return this;
	}

	/*
	 * First lines of any document are properties
	 */
	private CharSequence parseProperties(CharSequence content) {
		Matcher m = PROPERTIES_P.matcher(content);

		if (m.lookingAt()) {
			Matcher pm = PROPERTY_P.matcher(content);
			pm.region(m.start(), m.end());

			while (pm.find()) {
				setProperty(pm.group("key"), pm.group("value"));
			}

			return content.subSequence(m.end(), content.length());
		}
		return content;
	}

	List<Block> parseContent(CharSequence content) throws Exception {
		Rover rover = new Rover(content);
		List<Block> blocks = new ArrayList<>();

		while (!rover.isEof()) {
			while (rover.at(EMPTY_P))
				rover.next();

			if (rover.isEof())
				break;

			for (Handler h : handlers) {
				Block b = h.process(rover);
				if (b != null) {
					blocks.add(b);
					break;
				}
			}
		}
		return blocks;
	}

	public CompositeBlock parseComposite(String tag, CharSequence text) throws Exception {
		return new CompositeBlock(tag, parseContent(text));
	}

	public String toString() {
		Formatter f = new Formatter();
		append(f);
		return f.toString();
	}

	public void append(Formatter f) {
		for (Block o : tree) {
			if (o instanceof Block) {
				Block b = (Block) o;
				b.append(f);
			} else {
				f.format("<p>");
				para(f, o.toString());
				f.format("</p>\n");
			}
		}
	}

	public void append(Appendable a) {
		Formatter f = new Formatter(a);
		append(f);
	}

	/**
	 * This ghastly method parses the content of a paragraph and creates the
	 * output.
	 * 
	 * @param f
	 * @param content
	 */
	void para(Formatter f, CharSequence content) {
		if (content == null || content.length() == 0)
			return;

		boolean inCode = false;
		char prev = 0;
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);
			char next = i < content.length() - 1 ? content.charAt(i + 1) : 0;
			switch (c) {
				case '&' : {
					if (!inCode) {
						Matcher m = ENTITIES_P.matcher(content);
						m.region(i, content.length());
						if (m.lookingAt()) {
							f.format(m.group());
							i = m.end() - 1;
							break;
						}
					}
					f.format("&amp;");
					break;
				}

				case '<' : {
					Matcher m = AUTOLINK_P.matcher(content);
					m.region(i, content.length());
					if (m.lookingAt()) {
						f.format("<a href='%s'>%1$s</a>", escape(m.group("url"), false));
						i = m.end() - 1;
					} else {
						m = TAG_P.matcher(content);
						m.region(i, content.length());
						if (m.lookingAt()) {
							f.format("%s", m.group());
							i = m.end() - 1;
							String tag = m.group("tag");
							if ("code".equals(tag)) {
								if (m.group("end") == null)
									inCode = true;
								else
									inCode = false;
							}
						} else {
							f.format("&lt;");
						}
					}
					break;
				}

				case '-' :
					if (next == '-' && Character.isWhitespace(prev)) {
						Matcher m = STRIKE_OUT_P.matcher(content);
						m.region(i, content.length());
						if (m.lookingAt()) {
							f.format("<del>");
							para(f, m.group(1));
							f.format("</del>");
							i = m.end() - 1;
							break;
						}
					}
					f.format("%s", c);
					break;

				case '[' : {
					if (!inCode) {
						Matcher m = LINK_P.matcher(content);
						m.region(i, content.length());
						if (m.lookingAt()) {
							String text = m.group("text");
							String title = m.group("title");
							String href = m.group("href");
							if (href == null)
								href = m.group("hrefa");

							String id = m.group("anchor");
							if (id != null) {
								if (id.trim().isEmpty())
									id = text;

								Link link = getLink(id);
								if (link != null) {
									href = link.url;
									title = link.title;
								} else {
									f.format("[");
									break;
								}
							}

							if (href == null)
								href = "";

							f.format("<a href=\"%s\"", escape(href, false));
							if (title != null) {
								f.format(" title=\"%s\"", escape(title, true));
							}
							f.format(">");
							para(f, text);
							f.format("</a>\n");
							i = m.end() - 1;
						} else
							f.format("[");
						break;
					}
					f.format("[");
					break;
				}
				case '!' : {
					if (!inCode) {
						Matcher m = IMAGE_P.matcher(content);
						m.region(i, content.length());
						if (m.lookingAt()) {
							String text = m.group("text");
							String title = m.group("title");
							String href = m.group("href");
							if (href == null)
								href = m.group("hrefa");
							String anchor = m.group("anchor");
							if (anchor != null) {
								Link link = getLink(anchor);
								href = link.url;
								if (title == null && link.title != null)
									title = link.title;
							}

							f.format("<img src=\"%s\"", escape(href, false));
							if (title != null) {
								f.format(" title=\"%s\"", escape(title, true));
							}
							if (text != null) {
								f.format(" alt=\"%s\"", escape(text, true));
							}
							f.format(" />");
							i = m.end() - 1;
						}
						break;
					}
					f.format("[");
					break;
				}
				case ' ' : {
					if (!inCode) {
						Matcher m = BR_P.matcher(content);
						m.region(i, content.length());
						if (m.lookingAt()) {
							f.format("<br />\n");
							i = m.end() - 1;
							break;
						} else {
							m = SINGLE_EM_OR_STRING_P.matcher(content);
							m.region(i, content.length());
							if (m.lookingAt()) {
								f.format("%s", m.group());
								i = m.end() - 1;
								break;
							} else
								f.format("%s", c);
						}
					} else
						f.format(" ");
					break;
				}

				case '$' :
					if (!inCode) {
						int end = macros.findMacro(content, i);
						if (end > i) {
							CharSequence macro = content.subSequence(i, end);
							String replacement = macros.process(macro.toString(), this);
							if (replacement != null) {
								CompositeBlock blocks;
								try {
									blocks = parseComposite(null, replacement);
									blocks.compress();
									blocks.append(f);
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}
							i = end - 1;
							break;
						}
					}
					f.format("$");
					break;
				case '`' : {
					Matcher m = INLINE_CODE_P.matcher(content);
					m.region(i, content.length());
					if (m.lookingAt()) {
						f.format("<code>%s</code>", escape(m.group("text"), false));
						i = m.end() - 1;
						break;
					}
					f.format("%s", c);
					break;
				}

				/*
				 * Extension: superscript, subscript Superscripts may be written
				 * by surrounding the superscripted text by ^ characters;
				 * subscripts may be written by surrounding the subscripted text
				 * by ~ characters. Thus, for example, H~2~O is a liquid. 2^10^
				 * is 1024. If the superscripted or subscripted text contains
				 * spaces, these spaces must be escaped with backslashes. (This
				 * is to prevent accidental superscripting and subscripting
				 * through the ordinary use of ~ and ^.) Thus, if you want the
				 * letter P with “a cat” in subscripts, use P~a\ cat~, not P~a
				 * cat~.
				 */
				case '~' :
				case '^' : {
					Matcher m = SUB_SUP_P.matcher(content);
					m.region(i, content.length());
					if (m.lookingAt()) {
						if (c == '~')
							f.format("<sub>");
						else
							f.format("<sup>");

						para(f, m.group(2));

						if (c == '~')
							f.format("</sub>");
						else
							f.format("</sup>");
						i = m.end() - 1;
						break;
					}
					f.format("%s", c);
					break;
				}

				case '_' :
					// TODO pandoc
					/*
					 * Extension: intraword_underscores Because _ is sometimes
					 * used inside words and identifiers, pandoc does not
					 * interpret a _ surrounded by alphanumeric characters as an
					 * emphasis marker. If you want to emphasize just part of a
					 * word, use *: feas*ible*, not feas*able*.
					 */
					if (Character.isJavaIdentifierStart(prev) && Character.isJavaIdentifierPart(next)) {
						f.format("_");
						break;
					}
					// Fall through

				case '*' : {
					if (!inCode) {
						Matcher m = EM_P.matcher(content);
						m.region(i, content.length());
						if (m.lookingAt()) {
							switch (m.group("type").length()) {
								case 1 :
									f.format("<em>");
									break;

								case 2 :
									f.format("<strong>");
									break;
								case 3 :
									f.format("<strong><em>");
									break;
							}
							para(f, m.group("text"));
							switch (m.group("type").length()) {
								case 1 :
									f.format("</em>");
									break;

								case 2 :
									f.format("</strong>");
									break;
								case 3 :
									f.format("</em></strong>");
									break;
							}
							i = m.end() - 1;
							break;
						}
					}
					f.format("%s", c);
					break;
				}

				case '\\' :
					if (i < content.length() - 1) {
						c = content.charAt(++i);
						if (c == ' ')
							// TODO pandoc, not standard markdown
							f.format("%s", "&#160;");
						else
							f.format("%s", c);
					}
					break;

				default :
					f.format("%s", c);
					break;
			}
			prev = c;
		}
	}

	public String toHtml(String para) {
		Formatter f = new Formatter();
		para(f, para);
		return f.toString();
	}

	/**
	 * Escape a string, do entity conversion.
	 */
	public static String escape(CharSequence s, boolean literal) {
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
					if (!literal)
						sb.append(c);
					else
						sb.append("&quot;");
					break;

				case '&' :
					if (literal) {
						Matcher m = ENTITIES_P.matcher(s);
						m.region(i, s.length());
						if (m.lookingAt()) {
							sb.append(m.group());
							i = m.end() - 1;
							break;
						}
					}
					sb.append("&amp;");
					break;
				default :
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

	public Block paragraph(CharSequence text, String tag) {
		return new Paragraph(text, tag);
	}

	public Block literal(CharSequence text) {
		return new Literal(text);
	}

	public Block escaped(CharSequence content) {
		return new Escaped(content);
	}

	public Link getLink(String id) {
		Link link = links.get(id);
		if (link == null)
			return null;
		return link;
	}

	public void link(String id, String url, String title) {
		Link l = new Link();
		l.id = id;
		l.url = url;
		l.title = title;
		links.put(id, l);
	}

	public Configuration getConfiguration() {
		return this.configuration;
	}

	public void single(final Appendable out, String outerTemplate, final String innerTemplate) throws IOException {
		doTemplate(outerTemplate, out, new Runnable() {

			@Override
			public void run() {
				try {

					doTemplate(innerTemplate, out, new Runnable() {

						@Override
						public void run() {
							Formatter f = new Formatter(out);
							for (Block b : tree) {
								b.append(f);
							}
							f.close();
						}
					});
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

		});

	}

	void doTemplate(String template, Appendable pw, Runnable r) throws IOException {
		Matcher m = CONTENT_P.matcher(template);
		int start = 0;
		while (m.find()) {
			String prefix = template.substring(start, m.start());
			prefix = process(prefix);
			pw.append(prefix);
			start = m.end();

			switch (m.group(1)) {
				case "content" :
					r.run();
					break;
				case "css" :
					if ( css.length() != 0) {
						pw.append("\n<style>");
						pw.append(css);
						pw.append("\n<style>\n");
					}
					break;
			}
		}
		String suffix = template.substring(start);
		suffix = process(suffix);
		pw.append(suffix);
	}

	public void parse(File f) throws IOException, Exception {
		parse(IO.collect(f));
	}

	public File getResourcesDir() {
		if (resourcesDir == null) {
			String dir = getConfiguration().resources_dir();
			if (dir == null) {
				resourcesDir = new File("www");
			} else {
				resourcesDir = IO.getFile(dir);
			}
			resourcesDir.mkdirs();
			if (!resourcesDir.isDirectory())
				error("Could not create the resources directory %s", resourcesDir);

		}
		return resourcesDir;
	}

	public String getResourcesRelative() {
		if (resourcesPrefix == null) {
			String path = getConfiguration().resources_relative();
			if (path == null) {
				resourcesPrefix = getResourcesDir().getName();
			} else {
				resourcesPrefix = path;
			}
		}
		return resourcesPrefix;
	}

	public void addStyle(String style) {
		if (css.length() != 0)
			css.append("\n");
		css.append(style);
	}
}
