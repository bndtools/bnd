package aQute.lib.properties;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.Enumeration;
import java.util.Properties;

import aQute.lib.hex.Hex;
import aQute.lib.io.IO;

/**
 * Parses properties files
 */
@Deprecated
public class PropertiesParser {

	public static final String $$$ERRORS = "$$$ERRORS";

	static public Properties parse(URI input) throws Exception {

		Reader reader = IO.reader(input.toURL()
			.openStream());
		return parse(reader, input);
	}

	static public Properties parse(Reader reader, URI input) throws Exception {
		String file = input.getPath();

		Properties properties = new Properties();
		int c;
		int line = 0;
		String section = null;
		String errors = "";

		file: while ((c = reader.read()) != -1) {
			int start = 0;

			c = ws(c, reader);
			if (c == -1)
				break;

			if (c == '[') {
				// Handle sections.
				c = reader.read();
				c = ws(c, reader);
				if (c == -1) {
					//
					break file;
				}

				StringBuilder sb = new StringBuilder();
				while (Character.isJavaIdentifierPart(c)) {
					sb.append((char) c);
					c = reader.read();
				}
				c = ws(c, reader);
				if (c == ']') {
					c = reader.read();
					if (sb.length() == 0)
						section = null;
					else
						section = sb.toString();
				} else
					errors += file + "#" + line + ": section " + sb + " not properly finished, ignored\n";
			} else if (c == '#' || c == '/') {

				// Comments

				if (c == '/') {
					c = reader.read();
					if (c == '*') {
						multilinecomment: while ((c = reader.read()) != -1) {
							while (c == '*') {
								c = reader.read();
								if (c == '/' || c == -1)
									break multilinecomment;
							}
						}
					} else
						errors += file + "#" + line + ": false comment";
				}

			} else {

				// parse the name

				StringBuilder name = new StringBuilder();

				//
				// If we have a section set, we prefix the section
				// before the name
				//

				if (section != null)
					name.append(section)
						.append(".");

				while (Character.isJavaIdentifierPart(c) || c == '-') {
					name.append((char) c);
					c = reader.read();
				}

				c = ws(c, reader);

				StringBuilder value = new StringBuilder();

				if (c != -1) {

					// The : or = are optional.

					if (c == ':' || c == '=') {
						c = reader.read();
						c = ws(c, reader);
					}

					//
					// Check for a multiline definition. These
					// start with { and are closed with }
					//
					boolean multiline = c == '{';
					if (multiline) {
						c = reader.read();
						c = ws(c, reader);
						while (c == '\n' || c == '\r') {
							if (c == '\n')
								line++;
							c = reader.read();
						}
					}

					value: while (true) {
						switch (c) {
							case -1 :
								break value;

							case '\n' :
								if (multiline) {
									line++;
									value.append('\n');
									break;
								} else
									break value;

							case '}' :
								if (multiline) {
									c = reader.read();
									break value;
								} else
									value.append((char) c);
								break;

							case '\r' :
								// ignore
								break;

							case '\\' :
								c = reader.read();
								switch (c) {
									case -1 :
										errors = file + "#" + line + ": escaped eof";
										break;

									case 'u' :
										try {
											int code = Hex.nibble(reader.read()) * 0x1000;
											code += Hex.nibble(reader.read()) * 0x0100;
											code += Hex.nibble(reader.read()) * 0x0010;
											code += Hex.nibble(reader.read()) * 0x0001;
											if (code >= 0 && code <= 0xFFFF)
												value.append((char) code);
										} catch (Exception e) {
											errors += file + "#" + line + ": " + e + "\n";
										}
										break;

									case '\n' :
										line++;

									default :
										value.append((char) c);
										break;
								}
							default :
								value.append((char) c);
								break;
						}
						c = reader.read();
					}
				}

				while (Character.isWhitespace(value.charAt(value.length() - 1)))
					value.deleteCharAt(value.length() - 1);

				if (name.toString()
					.equals("-include")) {
					for (String uri : name.toString()
						.split("\\s*,\\s*")) {
						boolean mandatory = true;
						prefixes: while (true) {
							if (uri.startsWith("-")) {
								mandatory = false;
								uri = uri.substring(1);
							} else
								break prefixes;
						}

						URI u = input.resolve(uri);
						try (Reader inc = IO.reader(u.toURL()
							.openStream())) {
							Properties p = parse(u);
							for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {
								String k = (String) e.nextElement();
								String v = p.getProperty(k);

								if (k.equals($$$ERRORS))
									errors += v;
								else
									properties.setProperty(k, v);
							}
						} catch (Exception e) {
							if (mandatory)
								errors += file + "#" + line + ": include not found " + uri + "\n";
						}
					}

				} else {
					properties.setProperty(name.toString(), value.toString());
					properties.setProperty("$$$." + name.toString(), file + "#" + line);
				}
				if (c != -1 && c != '\n')
					c = ws(c, reader);

				if (c != '\n' && c != -1)
					errors += file + "#" + line + ": found unexpected characters at end of line\n";

				c = eol(c, reader);
				line++;
			}

		}
		if (errors.length() != 0)
			properties.put($$$ERRORS, errors);
		return properties;
	}

	private static int eol(int c, Reader reader) throws IOException {
		while (c != '\n' && c != -1)
			c = reader.read();
		return c;
	}

	private static int ws(int c, Reader reader) throws IOException {
		while (Character.isWhitespace(c) || c == '\r')
			c = reader.read();
		return c;
	}
}
