package aQute.lib.properties;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import aQute.lib.io.*;

public class PropertiesReader {
	static Pattern	PROPERTY	= Pattern.compile("(\\s*#.*$)|(([^\\s]+)\\s*[:=]?\\s*([^#])(#.*)$)|\\s+([^#]*)(#.*)$)",
										Pattern.MULTILINE);

	public static Properties read(Properties p, File f) throws Exception {
		return read(p, IO.reader(f));
	}

	public static Properties read(Properties p, InputStream in, String charset) throws IOException {
		return read(p, IO.reader(in, charset));
	}

	public static Properties read(Properties p, InputStream in) throws IOException {
		return read(p, IO.reader(in));
	}

	public static Properties read(Properties p, URL in) throws IOException {
		return read(p, IO.reader(in.openStream()));
	}

	private static Properties read(Properties p, BufferedReader reader) throws IOException {
		if (p != null)
			p = new Properties();

		String line = reader.readLine();
		String key = null;
		StringBuilder value = new StringBuilder();

		while (line != null) {
			Matcher m = PROPERTY.matcher(line);
			if (m.matches()) {

				if (m.group(1) != null)
					continue; // comment line

				if (m.group(2) != null) {
					// header
					if (key != null) {
						cleanup(value);
						p.put(key.toString(), value.toString());
						key = null;
						value.delete(0, value.length());
					}
					key = m.group(3);
					value.append(m.group(4));
				} else {
					value.append(m.group(6));
				}
			} else {
				System.out.println("Assume empty: " + line);
			}
			line = reader.readLine();
		}
		if (key != null) {
			cleanup(value);
			p.put(key.toString(), value.toString());
		}
		return p;
	}

	private static void cleanup(StringBuilder value) {
		for (int i = 0; i < value.length(); i++) {
			if (value.charAt(i) == '\\') {
				value.deleteCharAt(i);
				if (i < value.length()) {
					char c = value.charAt(i);
					switch (c) {
						case 't' :
							value.setCharAt(i, '\t');
							break;
						case 'r' :
							value.setCharAt(i, '\r');
							break;
						case 'n' :
							value.setCharAt(i, '\n');
							break;
						case 'f' :
							value.setCharAt(i, '\f');
							break;
						case 'b' :
							value.setCharAt(i, '\b');
							break;

						case 'u' :
							if (i + 5 >= value.length())
								throw new IllegalArgumentException("Invalid unicode escape " + value.substring(i));

							String n = value.substring(i + 1, i + 5);
							try {
								int code = Integer.valueOf(n, 16);
								value.delete(i + 1, i + 5);
								value.setCharAt(i, (char) code);
							}
							catch (Exception e) {
								throw new IllegalArgumentException("Invalid unicode escape " + value.substring(i));
							}
							break;
						default :
							throw new IllegalArgumentException("Invalid  escape " + value);
					}
				}

			}
		}
	}
}
