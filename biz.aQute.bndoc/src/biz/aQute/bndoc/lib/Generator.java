package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import aQute.lib.env.*;
import aQute.lib.io.*;

public abstract class Generator extends Base {
	static Pattern	CONTENT_P	= Pattern.compile("\\$\\{content\\}");
	File			images;
	String			outer;
	String			inner;
	File			output;

	public Generator(DocumentBuilder doc) {
		super(doc);
	}

	@Override
	public void close() throws IOException {}

	abstract public void generate() throws Exception;

	String getTemplate(String name, String deflt) {
		String path = getProperty(name);
		if (path == null)
			return deflt;

		try {
			URI uri = toURI(path);
			return IO.collect(uri.toURL().openStream());
		}
		catch (Exception e) {
			error("fail %s", e);
			return deflt;
		}

	}

	void doTemplate(String template, PrintWriter pw, Runnable r) {
		Matcher m = CONTENT_P.matcher(template);
		int start = 0;
		while (m.find()) {
			String prefix = template.substring(start, m.start());
			prefix = process(prefix);
			pw.write(prefix);
			start = m.end();
			r.run();
		}
		String suffix = template.substring(start);
		suffix = process(suffix);
		pw.write(suffix);
	}

	public StringBuilder _style(String args[]) throws Exception {
		StringBuilder sb = new StringBuilder();
		Map<URI,Props> css = getCss();
		if (!css.isEmpty()) {

			sb.append("\n<style>\n");
			for (Entry<URI,Props> entry : css.entrySet()) {
				String content = IO.collect(entry.getKey().toURL().openStream());
				sb.append(content).append("\n");
			}
			sb.append("\n</style>\n");
		}
		return sb;
	}

	private Map<URI,Props> getCss() throws Exception {
		return toURis(CSS);
	}

	public void prepare() throws Exception {
		output = IO.getFile(getProperty(OUTPUT, getDefaultOutput()));
		if (output.getPath().equals("-"))
			images = IO.getFile(getProperty(IMAGES, "imgs"));
		else
			images = IO.getFile(getProperty(IMAGES, output.getAbsolutePath() + ".imgs"));

		outer = getTemplate("outer", "<outer><head>${style}</head>\n  ${content}\n</outer>");
		inner = getTemplate("inner", "  <inner>\n    ${content}\n  </inner>");
	}

	public String getDefaultOutput() {
		return "doc";
	}

	public String toURI(File file) {
		URI relative = output.toURI().relativize(file.toURI());
		return relative.toString();
	}
}
